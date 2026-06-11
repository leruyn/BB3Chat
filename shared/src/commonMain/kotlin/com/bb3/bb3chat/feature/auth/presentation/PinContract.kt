package com.bb3.bb3chat.feature.auth.presentation

import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

data class PinUiState(
    val enteredDigits: String     = "",
    val isLoading: Boolean        = false,
    val shakeError: Boolean       = false,
    val wrongAttempts: Int        = 0,
    val isSetupMode: Boolean      = false,
    val setupStep: SetupStep      = SetupStep.ENTER_REAL,
    val pendingRealPin: String    = ""
) : UiState

enum class SetupStep { ENTER_REAL, CONFIRM_REAL, ENTER_DECOY, DONE }

sealed class PinUiEvent : UiEvent {
    data class DigitPressed(val digit: String) : PinUiEvent()
    object BackspacePressed                    : PinUiEvent()
    object ClearPressed                        : PinUiEvent()
    object SubmitPin                           : PinUiEvent()
}

sealed class PinUiEffect : UiEffect {
    object NavigateToRealInbox  : PinUiEffect()
    object NavigateToDecoySpace : PinUiEffect()
    object NavigateToSetup      : PinUiEffect()
    data class ShowError(val message: String) : PinUiEffect()
}
