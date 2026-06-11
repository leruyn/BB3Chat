import AVFoundation
import UIKit
import shared

final class QrScannerBridge: NSObject, AVCaptureMetadataOutputObjectsDelegate {

    static let shared = QrScannerBridge()
    private override init() { super.init() }

    private var captureSession: AVCaptureSession?
    private var previewLayer: AVCaptureVideoPreviewLayer?
    private var onDetected: ((String) -> Void)?
    private var lastDetectedAt: TimeInterval = 0

    func registerWithKotlin() {
        QrScannerBridgeHolder.shared.bindScanner(
            start: { view in QrScannerBridge.shared.start(in: view) },
            stop: { QrScannerBridge.shared.stop() },
            updateFrame: { view in QrScannerBridge.shared.updateFrame(for: view) }
        )
    }

    private func updateFrame(for view: UIView) {
        previewLayer?.frame = view.bounds
    }

    private func start(in view: UIView) {
        stop()
        onDetected = { code in
            QrScannerBridgeHolder.shared.deliverDetected(code: code)
        }

        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            configureSession(in: view)
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    guard granted else { return }
                    self?.configureSession(in: view)
                }
            }
        default:
            break
        }
    }

    private func configureSession(in view: UIView) {
        let session = AVCaptureSession()
        guard let device = AVCaptureDevice.default(for: .video),
              let input = try? AVCaptureDeviceInput(device: device) else { return }

        let output = AVCaptureMetadataOutput()
        guard session.canAddInput(input), session.canAddOutput(output) else { return }

        session.addInput(input)
        session.addOutput(output)
        output.setMetadataObjectsDelegate(self, queue: .main)
        output.metadataObjectTypes = [.qr]

        let layer = AVCaptureVideoPreviewLayer(session: session)
        layer.videoGravity = .resizeAspectFill
        layer.frame = view.bounds
        view.layer.insertSublayer(layer, at: 0)

        captureSession = session
        previewLayer = layer

        DispatchQueue.global(qos: .userInitiated).async {
            session.startRunning()
        }
    }

    func stop() {
        captureSession?.stopRunning()
        previewLayer?.removeFromSuperlayer()
        captureSession = nil
        previewLayer = nil
        onDetected = nil
    }

    func metadataOutput(
        _ output: AVCaptureMetadataOutput,
        didOutput metadataObjects: [AVMetadataObject],
        from connection: AVCaptureConnection
    ) {
        let now = Date().timeIntervalSince1970
        guard now - lastDetectedAt > 1.5 else { return }
        guard let object = metadataObjects.first as? AVMetadataMachineReadableCodeObject,
              let value = object.stringValue,
              value.hasPrefix("BB3:") else { return }
        lastDetectedAt = now
        onDetected?(value)
    }
}
