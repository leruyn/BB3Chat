package com.bb3.bb3chat.core.bridge

import platform.UIKit.UIView

/** Swift AVCapture session renders into a UIView; Kotlin receives scanned payloads. */
object QrScannerBridgeHolder {
    private var onDetected: ((String) -> Unit)? = null
    private var startFn: ((UIView) -> Unit)? = null
    private var stopFn: (() -> Unit)? = null
    private var updateFrameFn: ((UIView) -> Unit)? = null

    fun bindScanner(
        start: (UIView) -> Unit,
        stop: () -> Unit,
        updateFrame: (UIView) -> Unit
    ) {
        startFn = start
        stopFn = stop
        updateFrameFn = updateFrame
    }

    fun setCallback(callback: (String) -> Unit) {
        onDetected = callback
    }

    fun clearCallback() {
        onDetected = null
    }

    fun start(inView: UIView) {
        startFn?.invoke(inView)
    }

    fun stop() {
        stopFn?.invoke()
    }

    fun updateFrame(view: UIView) {
        updateFrameFn?.invoke(view)
    }

    fun deliverDetected(code: String) {
        onDetected?.invoke(code)
    }
}
