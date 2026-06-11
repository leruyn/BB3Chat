package com.bb3.bb3chat.feature.messaging.data

typealias FirestoreCancel = () -> Unit

/** Swift Firestore SDK — JSON string boundary. */
object FirestoreBridgeHolder {
    private var observeFn: ((String, (String) -> Unit) -> FirestoreCancel)? = null
    private var sendMessageFn: ((
        roomId: String,
        messageId: String,
        payloadJson: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null
    private var updateRoomActivityFn: ((
        roomId: String,
        contentType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null
    private var checkRoomStatusFn: ((
        roomId: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null
    private var getRoomJsonFn: ((
        roomId: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null
    private var createRoomFn: ((
        roomId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null
    private var joinRoomFn: ((
        roomId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null
    private var markReadFn: ((
        roomId: String,
        messageId: String,
        alias: String,
        timestamp: Long,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null
    private var deleteMsgFn: ((
        roomId: String,
        messageId: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null
    private var fetchRecentFn: ((
        roomId: String,
        limit: Int,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) -> Unit)? = null

    fun register(
        observeMessages: (String, (String) -> Unit) -> FirestoreCancel,
        sendMessage: (
            roomId: String,
            messageId: String,
            payloadJson: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) -> Unit,
        updateRoomActivity: (
            roomId: String,
            contentType: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) -> Unit,
        checkRoomStatus: (
            roomId: String,
            onResult: (String) -> Unit,
            onError: (String) -> Unit
        ) -> Unit,
        getRoomJson: (
            roomId: String,
            onResult: (String) -> Unit,
            onError: (String) -> Unit
        ) -> Unit,
        createRoom: (
            roomId: String,
            uid: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) -> Unit,
        joinRoom: (
            roomId: String,
            uid: String,
            onSuccess: () -> Unit,
            onError: (String) -> Unit
        ) -> Unit,
        markMessageRead: (
            roomId: String,
            messageId: String,
            alias: String,
            timestamp: Long,
            onDone: () -> Unit,
            onError: (String) -> Unit
        ) -> Unit,
        deleteMessage: (
            roomId: String,
            messageId: String,
            onDone: () -> Unit,
            onError: (String) -> Unit
        ) -> Unit,
        fetchRecentMessages: (
            roomId: String,
            limit: Int,
            onResult: (String) -> Unit,
            onError: (String) -> Unit
        ) -> Unit
    ) {
        observeFn = observeMessages
        sendMessageFn = sendMessage
        updateRoomActivityFn = updateRoomActivity
        checkRoomStatusFn = checkRoomStatus
        getRoomJsonFn = getRoomJson
        createRoomFn = createRoom
        joinRoomFn = joinRoom
        markReadFn = markMessageRead
        deleteMsgFn = deleteMessage
        fetchRecentFn = fetchRecentMessages
    }

    fun observeMessages(roomId: String, onUpdate: (String) -> Unit): FirestoreCancel =
        observeFn?.invoke(roomId, onUpdate)
            ?: error("FirestoreBridge chưa đăng ký")

    fun sendMessage(
        roomId: String,
        messageId: String,
        payloadJson: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        sendMessageFn?.invoke(roomId, messageId, payloadJson, onSuccess, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }

    fun updateRoomActivity(
        roomId: String,
        contentType: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        updateRoomActivityFn?.invoke(roomId, contentType, onSuccess, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }

    fun checkRoomStatus(
        roomId: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        checkRoomStatusFn?.invoke(roomId, onResult, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }

    fun getRoomJson(
        roomId: String,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        getRoomJsonFn?.invoke(roomId, onResult, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }

    fun createRoom(
        roomId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        createRoomFn?.invoke(roomId, uid, onSuccess, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }

    fun joinRoom(
        roomId: String,
        uid: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        joinRoomFn?.invoke(roomId, uid, onSuccess, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }

    fun markMessageRead(
        roomId: String,
        messageId: String,
        alias: String,
        timestamp: Long,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        markReadFn?.invoke(roomId, messageId, alias, timestamp, onDone, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }

    fun deleteMessage(
        roomId: String,
        messageId: String,
        onDone: () -> Unit,
        onError: (String) -> Unit
    ) {
        deleteMsgFn?.invoke(roomId, messageId, onDone, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }

    fun fetchRecentMessages(
        roomId: String,
        limit: Int,
        onResult: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        fetchRecentFn?.invoke(roomId, limit, onResult, onError)
            ?: onError("FirestoreBridge chưa đăng ký")
    }
}
