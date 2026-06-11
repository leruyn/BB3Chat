import UIKit
import FirebaseCore
import FirebaseMessaging
import shared

@main
class AppDelegate: UIResponder, UIApplicationDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        FirebaseApp.configure()
        MainViewControllerKt.initIosKoin()
        CryptoBridge.shared.registerWithKotlin()
        AuthBridge.shared.registerWithKotlin()
        FirestoreBridge.shared.registerWithKotlin()
        ImagePickerBridge.shared.registerWithKotlin()

        Task {
            try? await AuthBridgeHolder.shared.ensureSignedIn()
        }

        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = MainViewControllerKt.MainViewController()
        window?.makeKeyAndVisible()

        application.registerForRemoteNotifications()
        Messaging.messaging().delegate = self
        SensorBridge.shared.startMonitoring()
        return true
    }

    func application(
        _ application: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        PushHandler.handle(userInfo: userInfo)
        completionHandler(.newData)
    }

    func application(
        _ application: UIApplication,
        didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data
    ) {
        Messaging.messaging().apnsToken = deviceToken
    }
}

extension AppDelegate: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        Task {
            await TokenBridge.shared.onTokenRefreshed(token: token)
        }
    }
}
