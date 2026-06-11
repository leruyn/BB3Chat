package com.bb3.bb3chat.feature.messaging.presentation.inbox

import com.bb3.bb3chat.feature.messaging.domain.model.InboxRoom
import com.bb3.bb3chat.feature.messaging.domain.repository.RoomRepository
import com.bb3.bb3chat.feature.security.domain.usecase.TriggerLocalPanicUseCase
import com.bb3.bb3chat.presentation.base.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class InboxViewModel(
    private val roomRepository: RoomRepository,
    private val triggerPanic: TriggerLocalPanicUseCase
) : BaseViewModel<InboxUiState, InboxUiEvent, InboxUiEffect>(InboxUiState()) {

    init {
        observeRooms()
        refreshInbox()
    }

    private fun observeRooms() {
        scope.launch {
            roomRepository.observeActiveRooms()
                .catch { updateState { copy(isLoading = false) } }
                .collect { rooms ->
                    updateState {
                        copy(
                            rooms     = rooms.map { it.toRoomItem() },
                            isLoading = false
                        )
                    }
                }
        }
    }

    private fun refreshInbox() {
        scope.launch {
            updateState { copy(isSyncing = true) }
            runCatching { roomRepository.syncInboxFromRemote() }
            updateState { copy(isSyncing = false) }
        }
    }

    override suspend fun onEvent(event: InboxUiEvent) {
        when (event) {
            is InboxUiEvent.OpenRoom     -> emitEffect(InboxUiEffect.NavigateToChatroom(event.roomId))
            is InboxUiEvent.OpenPairing  -> emitEffect(InboxUiEffect.NavigateToPairing)
            is InboxUiEvent.Refresh      -> refreshInbox()
            is InboxUiEvent.OpenSettings -> emitEffect(InboxUiEffect.NavigateToSettings)
            is InboxUiEvent.OpenStore    -> emitEffect(InboxUiEffect.NavigateToStore)
            is InboxUiEvent.TriggerPanic -> {
                triggerPanic()
                emitEffect(InboxUiEffect.NavigateToCalculator)
            }
        }
    }

    private fun InboxRoom.toRoomItem() = RoomItem(
        id            = id,
        alias         = alias,
        avatarIndex   = avatarIndex,
        lastSnippet   = lastSnippet,
        lastTimestamp = lastTimestamp,
        unreadCount   = unreadCount,
        isPinned      = isPinned,
        isMuted       = isMuted
    )
}
