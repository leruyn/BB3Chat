import UserNotifications
import shared

final class LocalNotificationBridge {

    static let shared = LocalNotificationBridge()

    private init() {}

    func registerWithKotlin() {
        LocalNotificationBridgeHolder.shared.register(
            show: { title, body in
                self.show(title: title, body: body)
            },
            areGranted: {
                KotlinBoolean(value: self.cachedGranted)
            },
            ensureGranted: { onResult in
                Task {
                    let ok = await self.ensureAuthorization()
                    onResult(KotlinBoolean(value: ok))
                }
            }
        )
        refreshAuthorizationStatus()
    }

    private var cachedGranted = false

    private func refreshAuthorizationStatus() {
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            DispatchQueue.main.async {
                self.cachedGranted = settings.authorizationStatus == .authorized
                    || settings.authorizationStatus == .provisional
            }
        }
    }

    @discardableResult
    private func ensureAuthorization() async -> Bool {
        let center = UNUserNotificationCenter.current()
        let settings = await center.notificationSettings()
        switch settings.authorizationStatus {
        case .authorized, .provisional, .ephemeral:
            cachedGranted = true
            return true
        case .denied:
            cachedGranted = false
            return false
        case .notDetermined:
            let granted = (try? await center.requestAuthorization(options: [.alert, .sound, .badge])) ?? false
            cachedGranted = granted
            return granted
        @unknown default:
            return false
        }
    }

    private func show(title: String, body: String) {
        Task {
            guard await ensureAuthorization() else { return }
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = body
            content.sound = .default

            let request = UNNotificationRequest(
                identifier: UUID().uuidString,
                content: content,
                trigger: UNTimeIntervalNotificationTrigger(timeInterval: 0.3, repeats: false)
            )
            try? await UNUserNotificationCenter.current().add(request)
        }
    }
}
