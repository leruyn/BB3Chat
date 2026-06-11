package com.bb3.bb3chat.feature.messaging.presentation.chatroom

import com.bb3.bb3chat.core.crypto.AliasCodec
import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.platform.FakePushCoordinator
import com.bb3.bb3chat.core.platform.ImageProcessor
import com.bb3.bb3chat.core.platform.VoicePlayer
import com.bb3.bb3chat.core.platform.VoiceRecorder
import com.bb3.bb3chat.core.util.fireAtMsForToday
import com.bb3.bb3chat.core.util.formatHourMinute
import com.bb3.bb3chat.core.vip.VipEntitlements
import com.bb3.bb3chat.feature.messaging.data.ScheduledMessageStore
import com.bb3.bb3chat.feature.messaging.domain.model.DestructConfig
import com.bb3.bb3chat.feature.messaging.domain.model.DestructMode
import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.feature.messaging.domain.usecase.OpenChatroomResult
import com.bb3.bb3chat.feature.messaging.domain.usecase.OpenChatroomUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendImageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendMessageUseCase
import com.bb3.bb3chat.feature.messaging.domain.usecase.SendVoiceUseCase
import com.bb3.bb3chat.feature.security.domain.usecase.ExecutePanicUseCase
import com.bb3.bb3chat.presentation.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class ChatroomViewModel(
    private val roomId          : String,
    private val sessionManager  : SessionManager,
    private val messageRepo     : MessageRepository,
    private val openChatroom    : OpenChatroomUseCase,
    private val sendMessage     : SendMessageUseCase,
    private val sendImage       : SendImageUseCase,
    private val sendVoice       : SendVoiceUseCase,
    private val imageProcessor  : ImageProcessor,
    private val executePanic    : ExecutePanicUseCase,
    private val vipEntitlements : VipEntitlements,
    private val scheduledStore  : ScheduledMessageStore,
    private val voiceRecorder   : VoiceRecorder,
    private val voicePlayer     : VoicePlayer,
    private val fakePushCoordinator: FakePushCoordinator
) : BaseViewModel<ChatroomUiState, ChatroomUiEvent, ChatroomUiEffect>(
    ChatroomUiState(roomId = roomId)
) {

    init {
        val options = vipEntitlements.allowedSelfDestructOptions()
        updateState {
            copy(
                selfDestructOptions = options,
                selfDestructSeconds = options.first(),
                scheduledPending    = scheduledStore.listForRoom(roomId).map { it.toUi() }
            )
        }
    }

    private val myAlias: String
        get() = runCatching {
            sessionManager.requireDatabase()
                .appConfigQueries.selectByKey("user_alias")
                .executeAsOneOrNull() ?: ""
        }.getOrDefault("")

    init {
        checkRoomAndLoad()
        startScheduledTicker()
        startVoiceRecordTicker()
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
            processDueScheduled()
        }
    }

    private fun startScheduledTicker() {
        scope.launch {
            while (true) {
                delay(5_000)
                processDueScheduled()
            }
        }
    }

    private fun startVoiceRecordTicker() {
        scope.launch {
            while (true) {
                delay(200)
                if (voiceRecorder.isRecording()) {
                    val secs = (voiceRecorder.elapsedMs() / 1000).toInt()
                    updateState { copy(isRecordingVoice = true, voiceRecordSecs = secs) }
                    if (secs >= 30) {
                        onEvent(ChatroomUiEvent.ToggleVoiceRecord)
                    }
                }
            }
        }
    }

    private suspend fun processDueScheduled() {
        val now = Clock.System.now().toEpochMilliseconds()
        val due = scheduledStore.due(now).filter { it.roomId == roomId }
        due.forEach { item ->
            runCatching {
                sendMessage(roomId = roomId, plainText = item.plainText)
                scheduledStore.remove(item.id)
            }
        }
        if (due.isNotEmpty()) {
            refreshScheduledUi()
        }
    }

    private fun refreshScheduledUi() {
        updateState {
            copy(scheduledPending = scheduledStore.listForRoom(roomId).map { it.toUi() })
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
            is ChatroomUiEvent.InputChanged -> {
                fakePushCoordinator.updateDraft(event.text)
                updateState { copy(inputText = event.text) }
            }
            is ChatroomUiEvent.SendText           -> doSendText()
            is ChatroomUiEvent.AttachImage        -> doStageImage(event.bytes)
            is ChatroomUiEvent.SendImage          -> doSendImage()
            is ChatroomUiEvent.CancelAttach       -> updateState { copy(attachPreview = null) }
            is ChatroomUiEvent.TriggerPanic       -> {
                voicePlayer.stop()
                voiceRecorder.cancelRecording()
                fakePushCoordinator.maybeSendForUnsentDraft()
                executePanic()
                emitEffect(ChatroomUiEffect.NavigateToCalculator)
            }
            is ChatroomUiEvent.NavigateBack       -> {
                fakePushCoordinator.maybeSendForUnsentDraft()
                emitEffect(ChatroomUiEffect.NavigateBack)
            }
            is ChatroomUiEvent.MessageLongPress   -> onMessageLongPress(event.msgId)
            is ChatroomUiEvent.SelfDestructExpired -> onSelfDestructExpired()
            is ChatroomUiEvent.DismissSelfDestruct -> updateState {
                copy(selfDestructSecs = null, selfDestructMsgId = null)
            }
            is ChatroomUiEvent.ToggleSelfDestructSend -> updateState {
                copy(selfDestructSend = !selfDestructSend)
            }
            is ChatroomUiEvent.CycleSelfDestructSeconds -> {
                val options = currentState.selfDestructOptions
                if (options.size <= 1) return
                val idx = options.indexOf(currentState.selfDestructSeconds)
                val next = options[(idx + 1) % options.size]
                updateState { copy(selfDestructSeconds = next) }
            }
            is ChatroomUiEvent.ToggleSchedulePanel -> updateState {
                copy(showSchedulePanel = !showSchedulePanel)
            }
            is ChatroomUiEvent.ScheduleHourChanged -> updateState {
                copy(scheduleHour = event.hour.coerceIn(0, 23))
            }
            is ChatroomUiEvent.ScheduleMinuteChanged -> updateState {
                copy(scheduleMinute = event.minute.coerceIn(0, 59))
            }
            is ChatroomUiEvent.ConfirmSchedule    -> doScheduleMessage()
            is ChatroomUiEvent.CancelSchedule     -> updateState { copy(showSchedulePanel = false) }
            is ChatroomUiEvent.ToggleVoiceRecord  -> toggleVoiceRecord()
            is ChatroomUiEvent.PlayVoice          -> playVoice(event.msgId)
            is ChatroomUiEvent.StopVoicePlayback  -> {
                voicePlayer.stop()
                updateState { copy(playingVoiceMsgId = null) }
            }
        }
    }

    private suspend fun doScheduleMessage() {
        val text = currentState.inputText.trim()
        if (text.isEmpty()) {
            emitEffect(ChatroomUiEffect.ShowToast("Nhập nội dung trước khi hẹn giờ"))
            return
        }
        val fireAt = fireAtMsForToday(currentState.scheduleHour, currentState.scheduleMinute)
        scheduledStore.schedule(roomId, text, fireAt)
        fakePushCoordinator.clearDraft()
        updateState { copy(inputText = "", showSchedulePanel = false) }
        refreshScheduledUi()
        emitEffect(ChatroomUiEffect.ShowToast("Đã hẹn gửi lúc ${formatHourMinute(currentState.scheduleHour, currentState.scheduleMinute)}"))
    }

    private suspend fun toggleVoiceRecord() {
        if (voiceRecorder.isRecording()) {
            val clip = voiceRecorder.stopRecording()
            updateState { copy(isRecordingVoice = false, voiceRecordSecs = 0) }
            if (clip == null) {
                emitEffect(ChatroomUiEffect.ShowToast("Ghi âm thất bại"))
                return
            }
            updateState { copy(isSending = true) }
            runCatching {
                sendVoice(roomId, clip.bytes, clip.durationMs)
            }.onFailure {
                emitEffect(ChatroomUiEffect.ShowToast("Gửi voice thất bại"))
            }
            updateState { copy(isSending = false) }
        } else {
            if (!voiceRecorder.startRecording()) {
                emitEffect(ChatroomUiEffect.ShowToast("Không mở được micro"))
            } else {
                updateState { copy(isRecordingVoice = true, voiceRecordSecs = 0) }
            }
        }
    }

    private fun playVoice(msgId: String) {
        val msg = currentState.messages.firstOrNull { it.id == msgId } ?: return
        val voice = msg.content as? UiMessageContent.Voice ?: return
        val bytes = voice.audioBytes ?: return
        voicePlayer.play(bytes)
        updateState { copy(playingVoiceMsgId = msgId) }
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
        fakePushCoordinator.clearDraft()
        updateState { copy(inputText = "", isSending = true) }
        val destruct = if (currentState.selfDestructSend) {
            DestructConfig(
                mode              = DestructMode.COUNTDOWN,
                countdownSeconds  = currentState.selfDestructSeconds
            )
        } else null
        runCatching {
            sendMessage(roomId = roomId, plainText = text, destructConfig = destruct)
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

    @OptIn(ExperimentalEncodingApi::class)
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
                    val cipher = Base64.Default.decode(c.encryptedBase64)
                    val iv     = Base64.Default.decode(c.iv)
                    CryptoManager.decrypt(cipher, iv, roomKey)
                }.getOrNull()
                UiMessageContent.Image(decryptedBytes = bytes, blurHash = c.blurHash)
            }
            is MessageContent.Voice -> {
                val bytes = runCatching {
                    val cipher = Base64.Default.decode(c.encryptedBase64)
                    val iv     = Base64.Default.decode(c.iv)
                    CryptoManager.decrypt(cipher, iv, roomKey)
                }.getOrNull()
                UiMessageContent.Voice(
                    durationSecs = (c.durationMs / 1000).toInt().coerceAtLeast(1),
                    audioBytes   = bytes
                )
            }
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

    private fun com.bb3.bb3chat.feature.messaging.data.ScheduledMessage.toUi(): ScheduledUiItem {
        val dt = Instant.fromEpochMilliseconds(fireAtMs)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        return ScheduledUiItem(
            id        = id,
            preview   = plainText.take(40),
            timeLabel = formatHourMinute(dt.hour, dt.minute)
        )
    }
}
