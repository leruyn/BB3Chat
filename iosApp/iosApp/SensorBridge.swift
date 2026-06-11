import CoreMotion
import shared

final class SensorBridge {

    static let shared = SensorBridge()
    private let motionManager = CMMotionManager()

    private init() {}

    func startMonitoring() {
        guard motionManager.isAccelerometerAvailable else { return }
        motionManager.accelerometerUpdateInterval = 0.15

        motionManager.startAccelerometerUpdates(to: .main) { [weak self] data, error in
            guard let data = data, error == nil else { return }
            let z = data.acceleration.z
            let magnitude = sqrt(
                pow(data.acceleration.x, 2) +
                pow(data.acceleration.y, 2) +
                pow(data.acceleration.z, 2)
            )
            // Úp màn hình: z < -0.8 (normalized gravity units)
            // Lắc mạnh: magnitude > 2.5
            if z < -0.8 || magnitude > 2.5 {
                SensorBridgeHolder.shared.onPanicDetected()
            }
        }
    }

    func stopMonitoring() {
        motionManager.stopAccelerometerUpdates()
    }
}
