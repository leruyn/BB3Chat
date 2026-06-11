import FirebaseAuth
import Foundation
import shared

final class AuthBridge {

    static let shared = AuthBridge()
    private init() {}

    func registerWithKotlin() {
        AuthBridgeHolder.shared.register(
            getCurrentUid: { Auth.auth().currentUser?.uid },
            isSignedIn: { KotlinBoolean(value: Auth.auth().currentUser != nil) },
            signOut: { try? Auth.auth().signOut() },
            ensureSignedIn: { onComplete, onError in
                if Auth.auth().currentUser != nil {
                    onComplete()
                    return
                }
                Auth.auth().signInAnonymously { _, error in
                    if let error {
                        NSLog("BB3 Firebase signInAnonymously: %@", error.localizedDescription)
                        onError(error.localizedDescription)
                    } else {
                        onComplete()
                    }
                }
            }
        )
    }
}
