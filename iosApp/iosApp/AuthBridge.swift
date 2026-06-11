import FirebaseAuth
import Foundation
import shared

final class AuthBridge {

    static let shared = AuthBridge()
    private init() {}

    func registerWithKotlin() {
        AuthBridgeHolder.shared.register(
            getCurrentUid: { Auth.auth().currentUser?.uid },
            isSignedIn: { Auth.auth().currentUser != nil },
            signOut: { try? Auth.auth().signOut() },
            ensureSignedIn: { onComplete, onError in
                if Auth.auth().currentUser != nil {
                    onComplete()
                    return
                }
                Auth.auth().signInAnonymously { _, error in
                    if let error {
                        onError(error.localizedDescription)
                    } else {
                        onComplete()
                    }
                }
            }
        )
    }
}
