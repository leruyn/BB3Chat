import Foundation
import shared

// Gọi từ AppDelegate khi nhận silent push
@objc public class PushHandler: NSObject {

    @objc public static func handle(userInfo: [AnyHashable: Any]) {
        guard let action = userInfo["action"] as? String else { return }
        let roomId = userInfo["roomId"] as? String

        switch action {
        case "SELF_DESTRUCT":
            guard let rid = roomId else { return }
            destroyRoomLocally(roomId: rid)
        case "ROOM_DESTRUCT":
            guard let rid = roomId else { return }
            destroyRoomLocally(roomId: rid)
            deactivateRoom(roomId: rid)
        case "NEW_MESSAGE":
            Task {
                try? await PushBridge.shared.onNewMessagePush()
            }
        default:
            break
        }
    }

    private static func destroyRoomLocally(roomId: String) {
        // Bridge sang KMP SessionManager qua shared framework
        guard let db = SessionManagerHolder.shared.database else { return }
        db.messageQueries.deleteByRoomId(room_id: roomId)
    }

    private static func deactivateRoom(roomId: String) {
        guard let db = SessionManagerHolder.shared.database else { return }
        db.chatRoomQueries.deactivateRoom(room_id: roomId)
    }
}
