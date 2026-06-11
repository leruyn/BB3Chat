package com.bb3.bb3chat.feature.pairing.presentation

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.util.toHexUpper
import com.bb3.bb3chat.feature.pairing.domain.usecase.ConnectRoomUseCase
import com.bb3.bb3chat.presentation.base.BaseViewModel
import kotlinx.coroutines.launch

class QrHandshakeViewModel(
    private val connectRoom: ConnectRoomUseCase
) : BaseViewModel<QrHandshakeUiState, QrHandshakeUiEvent, QrHandshakeUiEffect>(
    QrHandshakeUiState()
) {

    init { generateCode() }

    override suspend fun onEvent(event: QrHandshakeUiEvent) {
        when (event) {
            is QrHandshakeUiEvent.GenerateCode         -> generateCode()
            is QrHandshakeUiEvent.ToggleScanMode       -> updateState { copy(scanMode = !scanMode, error = null) }
            is QrHandshakeUiEvent.OnCodeScanned        -> handleScanned(event.code)
            is QrHandshakeUiEvent.ManualCodeEntered    -> updateState { copy(scannedCode = event.code, error = null) }
            is QrHandshakeUiEvent.ConfirmConnect       -> doConnect()
            is QrHandshakeUiEvent.Dismiss              -> emitEffect(QrHandshakeUiEffect.Dismissed)
        }
    }

    private fun generateCode() {
        scope.launch {
            val bytes = CryptoManager.randomBytes(6)
            val code  = bytes.toHexUpper().take(8)
            updateState { copy(myRoomCode = code, error = null) }
        }
    }

    private fun handleScanned(raw: String) {
        val code = if (raw.startsWith("BB3:")) raw.removePrefix("BB3:") else raw
        if (code.length < 6) {
            updateState { copy(error = "Mã QR không hợp lệ") }
            return
        }
        updateState { copy(scannedCode = code, scanMode = false, error = null) }
    }

    private suspend fun doConnect() {
        val code = currentState.scannedCode.trim()
        if (code.isEmpty()) {
            updateState { copy(error = "Chưa nhập mã phòng") }
            return
        }
        updateState { copy(isConnecting = true, error = null) }
        runCatching {
            val myCode = currentState.myRoomCode
            val roomId = deriveRoomId(myCode, code)
            connectRoom(roomId, myCode, code)
        }.onSuccess { roomId ->
            updateState { copy(isConnecting = false) }
            emitEffect(QrHandshakeUiEffect.RoomCreated(roomId))
        }.onFailure { err ->
            updateState { copy(isConnecting = false, error = err.message ?: "Lỗi kết nối") }
            emitEffect(QrHandshakeUiEffect.ShowError(err.message ?: "Lỗi kết nối"))
        }
    }

    private fun deriveRoomId(a: String, b: String): String {
        val sorted = listOf(a, b).sorted().joinToString("|")
        return sorted.map { it.code.toString(16) }.joinToString("").take(28)
    }
}
