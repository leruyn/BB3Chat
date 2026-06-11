package com.bb3.bb3chat.feature.messaging.presentation.inbox

import com.bb3.bb3chat.core.security.SecurityPreferences
import com.bb3.bb3chat.core.security.SafeHoursSession
import com.bb3.bb3chat.feature.messaging.domain.model.InboxRoom
import com.bb3.bb3chat.feature.messaging.domain.repository.RoomRepository
import com.bb3.bb3chat.feature.security.domain.usecase.CaptureIntruderUseCase
import com.bb3.bb3chat.feature.security.domain.usecase.ExecutePanicUseCase
import com.bb3.bb3chat.presentation.base.BaseViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class InboxViewModel(
    private val roomRepository: RoomRepository,
    private val executePanic: ExecutePanicUseCase,
    private val captureIntruder: CaptureIntruderUseCase,
    private val securityPrefs: SecurityPreferences
) : BaseViewModel<InboxUiState, InboxUiEvent, InboxUiEffect>(InboxUiState()) {

    init {
        refreshIntruderBanner()
        observeRooms()
        refreshInbox()
    }

    private fun refreshIntruderBanner() {
        val count = captureIntruder.snapshotCount()
        updateState {
            copy(
                intruderCount       = count,
                showIntruderBanner  = count > 0 && !securityPrefs.isIntruderBannerSeen()
            )
        }
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
                SafeHoursSession.reset()
                executePanic()
                emitEffect(InboxUiEffect.NavigateToCalculator)
            }
            is InboxUiEvent.ViewIntruderGallery -> emitEffect(InboxUiEffect.NavigateToIntruderGallery)
            is InboxUiEvent.DismissIntruderBanner -> {
                securityPrefs.markIntruderBannerSeen()
                updateState { copy(showIntruderBanner = false) }
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
