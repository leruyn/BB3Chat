import FirebaseFirestore
import Foundation
import shared

final class FirestoreBridge {

    static let shared = FirestoreBridge()
    private let db = Firestore.firestore()
    private init() {}

    func registerWithKotlin() {
        FirestoreBridgeHolder.shared.register(
            observeMessages: { roomId, onUpdate in
                let reg = self.db.collection("rooms/\(roomId)/messages")
                    .order(by: "sentAt")
                    .addSnapshotListener { snapshot, error in
                        if error != nil {
                            onUpdate("[]")
                            return
                        }
                        guard let docs = snapshot?.documents else {
                            onUpdate("[]")
                            return
                        }
                        let arr = docs.map { $0.toJsonDict() }
                        guard let data = try? JSONSerialization.data(withJSONObject: arr),
                              let str = String(data: data, encoding: .utf8) else {
                            onUpdate("[]")
                            return
                        }
                        onUpdate(str)
                    }
                return {
                    reg.remove()
                    return KotlinUnit()
                }
            },
            sendMessage: { roomId, messageId, payloadJson, onSuccess, onError in
                guard let payloadData = payloadJson.data(using: .utf8),
                      let dict = try? JSONSerialization.jsonObject(with: payloadData) as? [String: Any] else {
                    onError("invalid payload")
                    return
                }
                let firestoreData = self.toFirestoreData(dict)
                self.db.collection("rooms/\(roomId)/messages")
                    .document(messageId)
                    .setData(firestoreData) { err in
                        if let err { onError(err.localizedDescription) }
                        else { onSuccess() }
                    }
            },
            updateRoomActivity: { roomId, contentType, onSuccess, onError in
                self.db.collection("rooms").document(roomId).updateData([
                    "lastActivityAt": FieldValue.serverTimestamp(),
                    "lastMsgType": contentType
                ]) { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            checkRoomStatus: { roomId, onResult, onError in
                self.db.collection("rooms").document(roomId).getDocument { doc, err in
                    if let err { onError(err.localizedDescription); return }
                    let status = doc?.data()?["status"] as? String ?? "ACTIVE"
                    onResult(status)
                }
            },
            getRoomJson: { roomId, onResult, onError in
                self.db.collection("rooms").document(roomId).getDocument { doc, err in
                    if let err { onError(err.localizedDescription); return }
                    guard let doc, doc.exists, let data = doc.data() else {
                        onResult("null")
                        return
                    }
                    var dict = data.mapValues { jsonSafe($0) }
                    dict["id"] = doc.documentID
                    guard let jsonData = try? JSONSerialization.data(withJSONObject: dict),
                          let str = String(data: jsonData, encoding: .utf8) else {
                        onResult("null")
                        return
                    }
                    onResult(str)
                }
            },
            createRoom: { roomId, uid, onSuccess, onError in
                self.db.collection("rooms").document(roomId).setData([
                    "creatorUid": uid,
                    "memberUids": [uid],
                    "status": "ACTIVE",
                    "createdAt": FieldValue.serverTimestamp(),
                    "lastActivityAt": FieldValue.serverTimestamp()
                ]) { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            joinRoom: { roomId, uid, onSuccess, onError in
                self.db.collection("rooms").document(roomId).updateData([
                    "memberUids": FieldValue.arrayUnion([uid])
                ]) { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            markMessageRead: { roomId, messageId, alias, timestamp, onDone, onError in
                self.db.collection("rooms/\(roomId)/messages").document(messageId)
                    .updateData(["readBy.\(alias)": timestamp]) { err in
                        if let err { onError(err.localizedDescription) }
                        else { onDone() }
                    }
            },
            deleteMessage: { roomId, messageId, onDone, onError in
                self.db.collection("rooms/\(roomId)/messages").document(messageId)
                    .delete { err in
                        if let err { onError(err.localizedDescription) }
                        else { onDone() }
                    }
            },
            fetchRecentMessages: { roomId, limit, onResult, onError in
                self.db.collection("rooms/\(roomId)/messages")
                    .order(by: "sentAt", descending: true)
                    .limit(to: limit.intValue)
                    .getDocuments { snapshot, err in
                        if let err { onError(err.localizedDescription); return }
                        let arr = snapshot?.documents.map { $0.toJsonDict() } ?? []
                        guard let data = try? JSONSerialization.data(withJSONObject: arr),
                              let str = String(data: data, encoding: .utf8) else {
                            onResult("[]")
                            return
                        }
                        onResult(str)
                    }
            },
            registerPairingHost: { hostCode, hostUid, onSuccess, onError in
                self.db.collection("pairing_sessions").document(hostCode).setData([
                    "hostUid": hostUid,
                    "status": "WAITING",
                    "createdAt": FieldValue.serverTimestamp()
                ]) { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            announcePairingJoin: { hostCode, peerCode, roomId, joinerUid, onSuccess, onError in
                self.db.collection("pairing_sessions").document(hostCode).updateData([
                    "peerCode": peerCode,
                    "roomId": roomId,
                    "joinerUid": joinerUid,
                    "status": "CONNECTED",
                    "connectedAt": FieldValue.serverTimestamp()
                ]) { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            observePairingSession: { hostCode, onUpdate in
                let reg = self.db.collection("pairing_sessions").document(hostCode)
                    .addSnapshotListener { snapshot, error in
                        if error != nil {
                            onUpdate("null")
                            return
                        }
                        guard let snapshot, snapshot.exists, let data = snapshot.data() else {
                            onUpdate("null")
                            return
                        }
                        var dict = data.mapValues { jsonSafe($0) }
                        dict["id"] = snapshot.documentID
                        guard let jsonData = try? JSONSerialization.data(withJSONObject: dict),
                              let str = String(data: jsonData, encoding: .utf8) else {
                            onUpdate("null")
                            return
                        }
                        onUpdate(str)
                    }
                return {
                    reg.remove()
                    return KotlinUnit()
                }
            },
            clearPairingSession: { hostCode, onSuccess, onError in
                self.db.collection("pairing_sessions").document(hostCode).delete { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            getPairingSessionJson: { hostCode, onResult, onError in
                self.db.collection("pairing_sessions").document(hostCode).getDocument { doc, err in
                    if let err { onError(err.localizedDescription); return }
                    guard let doc, doc.exists, let data = doc.data() else {
                        onResult("null")
                        return
                    }
                    var dict = data.mapValues { jsonSafe($0) }
                    dict["id"] = doc.documentID
                    guard let jsonData = try? JSONSerialization.data(withJSONObject: dict),
                          let str = String(data: jsonData, encoding: .utf8) else {
                        onResult("null")
                        return
                    }
                    onResult(str)
                }
            },
            createRoomCodeLobbyWaiting: { docId, uid, myCode, onSuccess, onError in
                self.db.collection("room_code_lobby").document(docId).setData([
                    "peer1Uid": uid,
                    "peer1Code": myCode,
                    "status": "WAITING",
                    "createdAt": FieldValue.serverTimestamp()
                ]) { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            connectRoomCodeLobby: { docId, uid, myCode, roomId, onSuccess, onError in
                self.db.collection("room_code_lobby").document(docId).updateData([
                    "peer2Uid": uid,
                    "peer2Code": myCode,
                    "roomId": roomId,
                    "status": "CONNECTED",
                    "connectedAt": FieldValue.serverTimestamp()
                ]) { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            observeRoomCodeLobby: { docId, onUpdate in
                let reg = self.db.collection("room_code_lobby").document(docId)
                    .addSnapshotListener { snapshot, error in
                        if error != nil {
                            onUpdate("null")
                            return
                        }
                        guard let snapshot, snapshot.exists, let data = snapshot.data() else {
                            onUpdate("null")
                            return
                        }
                        var dict = data.mapValues { jsonSafe($0) }
                        dict["id"] = snapshot.documentID
                        guard let jsonData = try? JSONSerialization.data(withJSONObject: dict),
                              let str = String(data: jsonData, encoding: .utf8) else {
                            onUpdate("null")
                            return
                        }
                        onUpdate(str)
                    }
                return {
                    reg.remove()
                    return KotlinUnit()
                }
            },
            clearRoomCodeLobby: { docId, onSuccess, onError in
                self.db.collection("room_code_lobby").document(docId).delete { err in
                    if let err { onError(err.localizedDescription) }
                    else { onSuccess() }
                }
            },
            getRoomCodeLobbyJson: { docId, onResult, onError in
                self.db.collection("room_code_lobby").document(docId).getDocument { doc, err in
                    if let err { onError(err.localizedDescription); return }
                    guard let doc, doc.exists, let data = doc.data() else {
                        onResult("null")
                        return
                    }
                    var dict = data.mapValues { jsonSafe($0) }
                    dict["id"] = doc.documentID
                    guard let jsonData = try? JSONSerialization.data(withJSONObject: dict),
                          let str = String(data: jsonData, encoding: .utf8) else {
                        onResult("null")
                        return
                    }
                    onResult(str)
                }
            }
        )
    }

    private func toFirestoreData(_ dict: [String: Any]) -> [String: Any] {
        var result: [String: Any] = [:]
        for (key, value) in dict {
            if key == "serverAt", let str = value as? String, str == "__SERVER_TIMESTAMP__" {
                result[key] = FieldValue.serverTimestamp()
                continue
            }
            if key == "sentAt", let ms = (value as? NSNumber)?.int64Value {
                let seconds = ms / 1000
                let nanos = Int32((ms % 1000) * 1_000_000)
                result[key] = Timestamp(seconds: seconds, nanoseconds: nanos)
                continue
            }
            result[key] = value
        }
        return result
    }
}

private extension QueryDocumentSnapshot {
    func toJsonDict() -> [String: Any] {
        var out: [String: Any] = [:]
        for (k, v) in data() {
            out[k] = jsonSafe(v)
        }
        out["id"] = documentID
        return out
    }
}

private func jsonSafe(_ value: Any) -> Any {
    if let ts = value as? Timestamp {
        return ts.seconds * 1000 + Int64(ts.nanoseconds) / 1_000_000
    }
    if let arr = value as? [Any] {
        return arr.map { jsonSafe($0) }
    }
    if let dict = value as? [String: Any] {
        return dict.mapValues { jsonSafe($0) }
    }
    return value
}
