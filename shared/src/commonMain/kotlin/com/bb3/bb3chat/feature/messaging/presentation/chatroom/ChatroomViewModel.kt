package com.bb3.bb3chat.feature.messaging.presentation.chatroom

import com.bb3.bb3chat.core.crypto.AliasCodec
import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.usecase.OpenChatroomResult
import com.bb3.bb3chat.feature.messaging.domain.usecase.OpenChatroomUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendImageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendMessageUseCase
import com.bb3.bb3chat.feature.security.domain.usecase.TriggerLocalPanicUseCase
import com.bb3.bb3chat.presentation.base.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class ChatroomViewModel(
    private val roomId          : String,
    private val sessionManager  : SessionManager,
    private val messageRepo     : MessageRepository,
    private val openChatroom    : OpenChatroomUseCase,
    private val sendMessage     : SendMessageUseCase,
    private val sendImage       : SendImageUseCase,
    private val imageProcessor  : ImageProcessor,
    private val triggerPanic    : TriggerLocalPanicUseCase
) : BaseViewModel<ChatroomUiState, ChatroomUiEvent, ChatroomUiEffect>(
    ChatroomUiState(roomId = roomId)
) {

    private val myAlias: String
        get() = runCatching {
            sessionManager.requireDatabase()
                .appConfigQueries.selectByKey("user_alias")
                .executeAsOneOrNull() ?: ""
        }.getOrDefault("")

    init {
        checkRoomAndLoad()
    }

    private fun checkRoomAndLoad() {
        scope.launch {
            val result = openChatroom(roomId)
            if (result is OpenChatroomResult.Destroyed) {
                updateState { copy(isDestroyed = true, isLoading = false) }
                emitEffect(ChatroomUiEffect.RoomDestroyed)
                return@launch
            }
            runCatching { sessionManager.loadRoomKey(roomId) }
            runCatching {
                sessionManager.requireDatabase().chatRoomQueries.clearUnread(roomId)
            }
            loadRoomMeta()
            observeMessages()
        }
    }

    private fun loadRoomMeta() {
        scope.launch {
            runCatching {
                val db   = sessionManager.requireDatabase()
                val row  = db.chatRoomQueries.selectAllActive().executeAsList()
                    .firstOrNull { it.id == roomId }
                if (row != null) {
                    val key   = sessionManager.requireSessionKey()
                    val alias = AliasCodec.decrypt(row.encrypted_alias, key)
                    updateState { copy(roomAlias = alias) }
                }
            }
        }
    }

    private fun observeMessages() {
        scope.launch {
            val roomKey = sessionManager.requireRoomKey(roomId)
            messageRepo.observeMessages(roomId)
                .catch { emitEffect(ChatroomUiEffect.ShowToast("Lỗi tải tin nhắn")) }
                .collect { messages ->
                    markUnreadAsRead(messages)
                    val uiMessages = messages.map { it.toUiMessage(myAlias, roomKey) }
                    updateState { copy(messages = uiMessages, isLoading = false) }
                }
        }
    }

    private suspend fun markUnreadAsRead(messages: List<Message>) {
        val alias = myAlias
        if (alias.isEmpty()) return
        messages
            .filter { it.senderAlias != alias && alias !in it.readBy }
            .forEach { msg ->
                runCatching { messageRepo.markMessageRead(roomId, msg.id, alias) }
            }
    }

    override suspend fun onEvent(event: ChatroomUiEvent) {
        when (event) {
            is ChatroomUiEvent.InputChanged       -> updateState { copy(inputText = event.text) }
            is ChatroomUiEvent.SendText           -> doSendText()
            is ChatroomUiEvent.AttachImage        -> doStageImage(event.bytes)
            is ChatroomUiEvent.SendImage          -> doSendImage()
            is ChatroomUiEvent.CancelAttach       -> updateState { copy(attachPreview = null) }
            is ChatroomUiEvent.TriggerPanic       -> {
                triggerPanic()
                emitEffect(ChatroomUiEffect.NavigateToCalculator)
            }
            is ChatroomUiEvent.NavigateBack       -> emitEffect(ChatroomUiEffect.NavigateBack)
            is ChatroomUiEvent.MessageLongPress   -> onMessageLongPress(event.msgId)
            is ChatroomUiEvent.SelfDestructExpired -> onSelfDestructExpired()
            is ChatroomUiEvent.DismissSelfDestruct -> updateState {
                copy(selfDestructSecs = null, selfDestructMsgId = null)
            }
        }
    }

    private suspend fun onMessageLongPress(msgId: String) {
        val msg = currentState.messages.firstOrNull { it.id == msgId } ?: return
        if (msg.selfDestruct && msg.destructSecs > 0) {
            updateState { copy(selfDestructSecs = msg.destructSecs, selfDestructMsgId = msgId) }
        } else {
            emitEffect(ChatroomUiEffect.OpenMessageMenu(msgId))
        }
    }

    private suspend fun onSelfDestructExpired() {
        val msgId = currentState.selfDestructMsgId ?: return
        updateState { copy(selfDestructSecs = null, selfDestructMsgId = null) }
        runCatching {
            messageRepo.deleteMessage(roomId, msgId)
        }.onFailure {
            messageRepo.triggerSelfDestruct(msgId)
        }
    }

    private suspend fun doSendText() {
        val text = currentState.inputText.trim()
        if (text.isEmpty()) return
        updateState { copy(inputText = "", isSending = true) }
        runCatching {
            sendMessage(roomId = roomId, plainText = text)
        }.onFailure {
            emitEffect(ChatroomUiEffect.ShowToast("Gửi thất bại"))
        }
        updateState { copy(isSending = false) }
    }

    private suspend fun doStageImage(bytes: ByteArray) {
        updateState { copy(isSending = true) }
        runCatching {
            val compressed = imageProcessor.compressAndResize(bytes, maxDimension = 480, quality = 70)
            val blurHash   = imageProcessor.generateBlurHash(compressed)
            val preview = AttachPreview(
                rawBytes  = compressed,
                blurHash  = blurHash,
                sizeBytes = compressed.size
            )
            updateState { copy(attachPreview = preview, isSending = false) }
        }.onFailure {
            updateState { copy(isSending = false) }
            emitEffect(ChatroomUiEffect.ShowToast("Không đọc được ảnh"))
        }
    }

    private suspend fun doSendImage() {
        val preview = currentState.attachPreview ?: return
        updateState { copy(isSending = true, attachPreview = null) }
        runCatching {
            sendImage(
                roomId        = roomId,
                rawImageBytes = preview.rawBytes,
                mimeType      = "image/jpeg"
            )
        }.onFailure {
            emitEffect(ChatroomUiEffect.ShowToast("Gửi ảnh thất bại: ${it.message}"))
        }
        updateState { copy(isSending = false) }
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    private fun Message.toUiMessage(myAlias: String, roomKey: ByteArray): UiMessage {
        val uiContent: UiMessageContent = when (val c = content) {
            is MessageContent.Text -> {
                val plain = runCatching {
                    CryptoManager.decryptString(c.encryptedBody, c.iv, roomKey)
                }.getOrDefault("[Lỗi giải mã]")
                UiMessageContent.Text(plain)
            }
            is MessageContent.Image -> {
                val bytes = runCatching {
                    val cipher = kotlin.io.encoding.Base64.Default.decode(c.encryptedBase64)
                    val iv     = kotlin.io.encoding.Base64.Default.decode(c.iv)
                    CryptoManager.decrypt(cipher, iv, roomKey)
                }.getOrNull()
                UiMessageContent.Image(decryptedBytes = bytes, blurHash = c.blurHash)
            }
            is MessageContent.Voice       -> UiMessageContent.Voice((c.durationMs / 1000).toInt())
            is MessageContent.SystemEvent -> UiMessageContent.SystemEvent(c.eventType.name)
            else                          -> UiMessageContent.Text("[Không hỗ trợ]")
        }
        return UiMessage(
            id           = id,
            senderId     = senderAlias,
            isMine       = senderAlias == myAlias,
            content      = uiContent,
            timestampMs  = sentAt,
            readByPeer   = readBy.keys.any { it != myAlias },
            selfDestruct = destructConfig != null,
            destructSecs = destructConfig?.countdownSeconds ?: 0
        )
    }
}
