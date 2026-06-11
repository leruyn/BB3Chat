import FirebaseAuth
import FirebaseFirestore
import Foundation
import shared

final class FcmTokenBridge {

    static let shared = FcmTokenBridge()
    private let db = Firestore.firestore()
    private init() {}

    func registerWithKotlin() {
        FcmTokenBridgeHolder.shared.register(
            register: { token, platform, onSuccess, onError in
                guard let uid = Auth.auth().currentUser?.uid else {
                    onError("not signed in")
                    return
                }
                self.db.collection("fcm_tokens").document(uid).setData([
                    "token": token,
                    "platform": platform,
                    "updatedAt": FieldValue.serverTimestamp()
                ]) { err in
                    if let err {
                        onError(err.localizedDescription)
                    } else {
                        onSuccess()
                    }
                }
            }
        )
    }
}
