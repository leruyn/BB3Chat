package com.bb3.bb3chat.feature.messaging.presentation.chatroom

import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

data class ChatroomUiState(
    val roomId               : String           = "",
    val roomAlias            : String           = "",
    val messages             : List<UiMessage>  = emptyList(),
    val isLoading            : Boolean          = true,
    val isSending            : Boolean          = false,
    val inputText            : String           = "",
    val isDestroyed          : Boolean          = false,
    val selfDestructSecs     : Int?             = null,
    val selfDestructMsgId    : String?          = null,
    val attachPreview        : AttachPreview?   = null,
    val selfDestructSend     : Boolean          = false,
    val selfDestructSeconds  : Int              = 60,
    val selfDestructOptions  : List<Int>        = listOf(60),
    val showSchedulePanel    : Boolean          = false,
    val scheduleHour         : Int              = 12,
    val scheduleMinute       : Int              = 0,
    val scheduledPending     : List<ScheduledUiItem> = emptyList(),
    val isRecordingVoice     : Boolean          = false,
    val voiceRecordSecs      : Int              = 0,
    val playingVoiceMsgId    : String?          = null
) : UiState

data class ScheduledUiItem(
    val id: String,
    val preview: String,
    val timeLabel: String
)

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
    data class Text(val text: String) : UiMessageContent()
    data class Image(val decryptedBytes: ByteArray?, val blurHash: String) : UiMessageContent()
    data class Voice(
        val durationSecs: Int,
        val audioBytes: ByteArray?
    ) : UiMessageContent()
    data class SystemEvent(val text: String) : UiMessageContent()
}

data class AttachPreview(
    val rawBytes  : ByteArray,
    val blurHash  : String,
    val sizeBytes : Int
)

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
    object ToggleSelfDestructSend                   : ChatroomUiEvent()
    object CycleSelfDestructSeconds                 : ChatroomUiEvent()
    object ToggleSchedulePanel                      : ChatroomUiEvent()
    data class ScheduleHourChanged(val hour: Int)   : ChatroomUiEvent()
    data class ScheduleMinuteChanged(val minute: Int): ChatroomUiEvent()
    object ConfirmSchedule                          : ChatroomUiEvent()
    object CancelSchedule                           : ChatroomUiEvent()
    object ToggleVoiceRecord                        : ChatroomUiEvent()
    data class PlayVoice(val msgId: String)         : ChatroomUiEvent()
    object StopVoicePlayback                        : ChatroomUiEvent()
}

sealed class ChatroomUiEffect : UiEffect {
    object NavigateBack                                 : ChatroomUiEffect()
    object NavigateToCalculator                         : ChatroomUiEffect()
    object RoomDestroyed                                : ChatroomUiEffect()
    data class ShowToast(val message: String)           : ChatroomUiEffect()
    data class OpenMessageMenu(val msgId: String)       : ChatroomUiEffect()
}
