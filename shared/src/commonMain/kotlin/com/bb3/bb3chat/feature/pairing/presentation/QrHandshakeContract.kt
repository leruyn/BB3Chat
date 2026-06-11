package com.bb3.bb3chat.feature.pairing.presentation

import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

data class QrHandshakeUiState(
    val myRoomCode   : String  = "",      // 8-char alphanumeric code để peer scan
    val qrBitmap     : Any?    = null,    // platform-specific QR bitmap (expect/actual)
    val scanMode     : Boolean = false,   // true = camera scanner active
    val scannedCode  : String  = "",
    val isConnecting : Boolean = false,
    val error        : String? = null
) : UiState

sealed class QrHandshakeUiEvent : UiEvent {
    object GenerateCode                          : QrHandshakeUiEvent()
    object ToggleScanMode                        : QrHandshakeUiEvent()
    data class OnCodeScanned(val code: String)   : QrHandshakeUiEvent()
    data class ManualCodeEntered(val code: String): QrHandshakeUiEvent()
    object ConfirmConnect                        : QrHandshakeUiEvent()
    object Dismiss                               : QrHandshakeUiEvent()
}

sealed class QrHandshakeUiEffect : UiEffect {
    data class RoomCreated(val roomId: String) : QrHandshakeUiEffect()
    object Dismissed                           : QrHandshakeUiEffect()
    data class ShowError(val msg: String)      : QrHandshakeUiEffect()
}
