package com.bb3.bb3chat.feature.messaging.presentation.chatroom

import com.bb3.bb3chat.feature.messaging.domain.model.Message
import com.bb3.bb3chat.feature.messaging.domain.model.MessageContent
import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

// ─── State ───────────────────────────────────────────────────────────────────

data class ChatroomUiState(
    val roomId          : String           = "",
    val roomAlias       : String           = "",
    val messages        : List<UiMessage>  = emptyList(),
    val isLoading       : Boolean          = true,
    val isSending       : Boolean          = false,
    val inputText       : String           = "",
    val isDestroyed     : Boolean          = false,   // Pull-Destruct triggered
    val selfDestructSecs: Int?             = null,
    val selfDestructMsgId: String?         = null,
    val attachPreview   : AttachPreview?   = null
) : UiState

/** Flattened, display-ready message (already decrypted) */
data class UiMessage(
    val id          : String,
    val senderId    : String,
    val isMine      : Boolean,
    val content     : UiMessageContent,
    val timestampMs : Long,
    val readByPeer  : Boolean    = false,
    val selfDestruct: Boolean    = false,
    val destructSecs: Int        = 0
)

sealed class UiMessageContent {
    data class Text(val text: String)                                         : UiMessageContent()
    data class Image(val decryptedBytes: ByteArray?, val blurHash: String)   : UiMessageContent()
    data class Voice(val durationSecs: Int)                                   : UiMessageContent()
    data class SystemEvent(val text: String)                                  : UiMessageContent()
}

data class AttachPreview(
    val rawBytes   : ByteArray,
    val blurHash   : String,
    val sizeBytes  : Int
)

// ─── Events ──────────────────────────────────────────────────────────────────

sealed class ChatroomUiEvent : UiEvent {
    data class InputChanged(val text: String)       : ChatroomUiEvent()
    object SendText                                 : ChatroomUiEvent()
    data class AttachImage(val bytes: ByteArray)    : ChatroomUiEvent()
    object SendImage                                : ChatroomUiEvent()
    object CancelAttach                             : ChatroomUiEvent()
    object TriggerPanic                             : ChatroomUiEvent()
    object NavigateBack                             : ChatroomUiEvent()
    data class MessageLongPress(val msgId: String)  : ChatroomUiEvent()
    object SelfDestructExpired                      : ChatroomUiEvent()
    object DismissSelfDestruct                      : ChatroomUiEvent()
}

// ─── Effects ─────────────────────────────────────────────────────────────────

sealed class ChatroomUiEffect : UiEffect {
    object NavigateBack                                 : ChatroomUiEffect()
    object NavigateToCalculator                         : ChatroomUiEffect()  // panic
    object RoomDestroyed                                : ChatroomUiEffect()
    data class ShowToast(val message: String)           : ChatroomUiEffect()
    data class OpenMessageMenu(val msgId: String)       : ChatroomUiEffect()
}
