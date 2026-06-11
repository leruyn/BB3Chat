package com.bb3.bb3chat.core.platform

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import com.bb3.bb3chat.feature.security.domain.usecase.CaptureIntruderUseCase
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidIntruderCapture(
    private val context: Context,
    private val captureUseCase: CaptureIntruderUseCase
) : IntruderCapture {

    private val backgroundThread = HandlerThread("IntruderCapture").also { it.start() }
    private val handler          = Handler(backgroundThread.looper)

    override suspend fun capture(attemptCount: Int) {
        val jpegBytes = captureJpeg() ?: return
        captureUseCase.execute(jpegBytes, attemptCount)
        captureUseCase.onNewSnapshot()
    }

    private suspend fun captureJpeg(): ByteArray? =
        suspendCancellableCoroutine { cont ->
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
                val frontId = cameraManager.cameraIdList.firstOrNull { id ->
                    cameraManager.getCameraCharacteristics(id)
                        .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
                } ?: run { cont.resume(null); return@suspendCancellableCoroutine }

                val imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 1)
                imageReader.setOnImageAvailableListener({ reader ->
                    val image  = reader.acquireLatestImage()
                    val buffer = image?.planes?.get(0)?.buffer
                    val bytes  = buffer?.let { ByteArray(it.remaining()).also { b -> it.get(b) } }
                    image?.close()
                    imageReader.close()
                    cont.resume(bytes)
                }, handler)

                cameraManager.openCamera(frontId, object : CameraDevice.StateCallback() {
                    override fun onOpened(camera: CameraDevice) {
                        val surfaces = listOf(imageReader.surface)
                        camera.createCaptureSession(surfaces,
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(session: CameraCaptureSession) {
                                    val request = camera.createCaptureRequest(
                                        CameraDevice.TEMPLATE_STILL_CAPTURE
                                    ).apply { addTarget(imageReader.surface) }.build()
                                    session.capture(request, null, handler)
                                }
                                override fun onConfigureFailed(session: CameraCaptureSession) {
                                    cont.resume(null)
                                }
                            }, handler)
                    }
                    override fun onDisconnected(camera: CameraDevice) { camera.close(); cont.resume(null) }
                    override fun onError(camera: CameraDevice, error: Int) { camera.close(); cont.resume(null) }
                }, handler)
            } catch (_: Exception) {
                cont.resume(null)
            }
        }
}
