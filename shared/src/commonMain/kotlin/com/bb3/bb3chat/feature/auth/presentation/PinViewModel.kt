package com.bb3.bb3chat.feature.auth.presentation

import com.bb3.bb3chat.feature.auth.domain.model.PinValidationResult
import com.bb3.bb3chat.feature.auth.domain.repository.PinAuthRepository
import com.bb3.bb3chat.feature.auth.domain.usecase.SetupPinUseCase
import com.bb3.bb3chat.feature.auth.domain.usecase.ValidatePinUseCase
import com.bb3.bb3chat.presentation.base.BaseViewModel
import kotlinx.coroutines.delay

private const val PIN_LENGTH = 4

class PinViewModel(
    private val validatePin: ValidatePinUseCase,
    private val setupPin: SetupPinUseCase,
    private val repository: PinAuthRepository
) : BaseViewModel<PinUiState, PinUiEvent, PinUiEffect>(
    PinUiState(isSetupMode = false)
) {
    init {
        when {
            repository.hasPendingRealPin() ->
                updateState { copy(isSetupMode = true, setupStep = SetupStep.CONFIRM_REAL) }
            !repository.isPinConfigured() ->
                updateState { copy(isSetupMode = true, setupStep = SetupStep.ENTER_REAL) }
            !repository.isDecoyPinConfigured() ->
                updateState { copy(isSetupMode = true, setupStep = SetupStep.ENTER_DECOY) }
        }
    }

    override suspend fun onEvent(event: PinUiEvent) {
        when (event) {
            is PinUiEvent.DigitPressed -> {
                val current = currentState.enteredDigits
                if (current.length < PIN_LENGTH) {
                    val updated = current + event.digit
                    updateState { copy(enteredDigits = updated, shakeError = false) }
                    if (updated.length == PIN_LENGTH) {
                        onEvent(PinUiEvent.SubmitPin)
                    }
                }
            }
            is PinUiEvent.BackspacePressed ->
                updateState { copy(enteredDigits = enteredDigits.dropLast(1)) }

            is PinUiEvent.ClearPressed ->
                updateState { copy(enteredDigits = "") }

            is PinUiEvent.SubmitPin -> {
                if (currentState.isSetupMode) handleSetup()
                else handleValidation()
            }
        }
    }

    private suspend fun handleValidation() {
        val pin = currentState.enteredDigits
        if (pin.length < PIN_LENGTH) return
        updateState { copy(isLoading = true) }

        try {
            when (validatePin(pin)) {
                is PinValidationResult.RealAccess  -> emitEffect(PinUiEffect.NavigateToRealInbox)
                is PinValidationResult.DecoyAccess -> emitEffect(PinUiEffect.NavigateToDecoySpace)
                is PinValidationResult.InvalidPin  -> triggerError()
            }
        } catch (e: Exception) {
            emitEffect(PinUiEffect.ShowError("Không thể mở phiên — thử lại"))
            triggerError()
        }
        updateState { copy(isLoading = false, enteredDigits = "") }
    }

    private suspend fun handleSetup() {
        val pin = currentState.enteredDigits
        if (pin.length < PIN_LENGTH) return

        when (currentState.setupStep) {
            SetupStep.ENTER_REAL -> {
                repository.savePendingRealPin(pin)
                updateState {
                    copy(
                        pendingRealPin = pin,
                        setupStep = SetupStep.CONFIRM_REAL,
                        enteredDigits = ""
                    )
                }
            }
            SetupStep.CONFIRM_REAL -> {
                val confirmed = repository.confirmPendingRealPin(pin)
                if (!confirmed) {
                    triggerError()
                    return
                }
                updateState {
                    copy(
                        pendingRealPin = pin,
                        setupStep = SetupStep.ENTER_DECOY,
                        enteredDigits = ""
                    )
                }
            }
            SetupStep.ENTER_DECOY -> {
                val realPin = currentState.pendingRealPin
                if (repository.isSameAsRealPin(pin)) {
                    triggerError()
                    return
                }
                try {
                    setupPin.setDecoyPin(pin)
                    finishSetup(realPin)
                } catch (e: Exception) {
                    triggerError()
                }
            }
            SetupStep.DONE -> emitEffect(PinUiEffect.NavigateToRealInbox)
        }
    }

    private suspend fun finishSetup(realPin: String) {
        updateState {
            copy(
                setupStep = SetupStep.DONE,
                isSetupMode = false,
                enteredDigits = "",
                pendingRealPin = ""
            )
        }

        if (realPin.isEmpty()) return

        try {
            when (validatePin(realPin)) {
                is PinValidationResult.RealAccess -> emitEffect(PinUiEffect.NavigateToRealInbox)
                else -> emitEffect(PinUiEffect.ShowError("Không thể mở phiên — nhập lại PIN thật"))
            }
        } catch (e: Exception) {
            emitEffect(PinUiEffect.ShowError("Không thể mở phiên — nhập lại PIN thật"))
        }
    }

    private suspend fun triggerError() {
        updateState { copy(shakeError = true, enteredDigits = "", wrongAttempts = wrongAttempts + 1) }
        delay(600)
        updateState { copy(shakeError = false) }
    }
}
