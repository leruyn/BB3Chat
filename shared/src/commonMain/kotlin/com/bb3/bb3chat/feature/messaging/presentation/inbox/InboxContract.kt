package com.bb3.bb3chat.feature.messaging.presentation.inbox

import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

data class InboxUiState(
    val rooms: List<RoomItem>  = emptyList(),
    val isLoading: Boolean     = true,
    val isSyncing: Boolean     = false,
    val userAlias: String      = "",
    val intruderCount: Int     = 0,
    val showIntruderBanner: Boolean = false
) : UiState

data class RoomItem(
    val id: String,
    val alias: String,
    val avatarIndex: Int,
    val lastSnippet: String,
    val lastTimestamp: Long,
    val unreadCount: Int,
    val isPinned: Boolean,
    val isMuted: Boolean
)

sealed class InboxUiEvent : UiEvent {
    data class OpenRoom(val roomId: String) : InboxUiEvent()
    object OpenPairing                      : InboxUiEvent()
    object Refresh                          : InboxUiEvent()
    object OpenSettings                     : InboxUiEvent()
    object OpenStore                        : InboxUiEvent()
    object TriggerPanic                     : InboxUiEvent()
    object ViewIntruderGallery              : InboxUiEvent()
    object DismissIntruderBanner            : InboxUiEvent()
}

sealed class InboxUiEffect : UiEffect {
    data class NavigateToChatroom(val roomId: String) : InboxUiEffect()
    object NavigateToPairing                            : InboxUiEffect()
    object NavigateToSettings                         : InboxUiEffect()
    object NavigateToStore                            : InboxUiEffect()
    object NavigateToCalculator                       : InboxUiEffect()
    object NavigateToIntruderGallery                  : InboxUiEffect()
}
