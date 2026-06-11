import UIKit
import UserNotifications
import FirebaseCore
import FirebaseMessaging
import shared

/// Hosts Compose content and forces light status bar icons on the dark app theme.
final class StatusBarHostingViewController: UIViewController {
    private let content: UIViewController

    init(content: UIViewController) {
        self.content = content
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(content)
        content.view.frame = view.bounds
        content.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(content.view)
        content.didMove(toParent: self)

        StatusBarBridgeHolder.shared.register { [weak self] in
            self?.setNeedsStatusBarAppearanceUpdate()
        }
    }

    override var preferredStatusBarStyle: UIStatusBarStyle {
        StatusBarBridgeHolder.shared.isLightContent() ? .lightContent : .darkContent
    }
}

@main
class AppDelegate: UIResponder, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    var window: UIWindow?

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        FirebaseApp.configure()
        IosAppBootstrap.shared.start()
        CryptoBridge.shared.registerWithKotlin()
        AuthBridge.shared.registerWithKotlin()
        FirestoreBridge.shared.registerWithKotlin()
        FcmTokenBridge.shared.registerWithKotlin()
        ImagePickerBridge.shared.registerWithKotlin()
        QrCodeGeneratorBridge.shared.registerWithKotlin()
        QrScannerBridge.shared.registerWithKotlin()
        UNUserNotificationCenter.current().delegate = self
        LocalNotificationBridge.shared.registerWithKotlin()
        VoiceBridge.shared.registerWithKotlin()

        Task {
            let ok = try await AuthBridgeHolder.shared.ensureSignedIn()
            if !ok.boolValue {
                NSLog("BB3: Firebase anonymous auth failed — enable Anonymous Auth in Firebase Console")
            }
        }

        window = UIWindow(frame: UIScreen.main.bounds)
        window?.rootViewController = StatusBarHostingViewController(
            content: MainViewControllerKt.MainViewController()
        )
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

    func applicationDidEnterBackground(_ application: UIApplication) {
        AppBackgroundCallbacks.shared.notifyAppBackground()
    }
}

extension AppDelegate {
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound, .badge])
    }
}

extension AppDelegate: MessagingDelegate {
    func messaging(_ messaging: Messaging, didReceiveRegistrationToken fcmToken: String?) {
        guard let token = fcmToken else { return }
        Task {
            try? await TokenBridge.shared.onTokenRefreshed(token: token)
        }
    }
}
