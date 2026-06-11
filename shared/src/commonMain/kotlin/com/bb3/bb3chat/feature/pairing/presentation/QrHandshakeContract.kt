package com.bb3.bb3chat.feature.pairing.presentation

import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

enum class HubTab { ROOM_CODE, QR, MY_ID }

data class QrHandshakeUiState(
    val hubTab             : HubTab  = HubTab.QR,
    val myRoomCode         : String  = "",
    val qrImageBytes       : ByteArray? = null,
    val scanMode           : Boolean = false,
    val scannedCode        : String  = "",
    val roomPhrase         : String  = "",
    val isRoomMatching     : Boolean = false,
    val roomMatchSecondsLeft: Int    = 0,
    val userAlias          : String  = "",
    val isConnecting       : Boolean = false,
    val isWaitingForPeer   : Boolean = false,
    val isExpired          : Boolean = false,
    val error              : String? = null
) : UiState

sealed class QrHandshakeUiEvent : UiEvent {
    data class SelectTab(val tab: HubTab)              : QrHandshakeUiEvent()
    data class RoomPhraseChanged(val value: String)    : QrHandshakeUiEvent()
    object GenerateRoomPhrase                          : QrHandshakeUiEvent()
    object StartRoomCodeMatch                          : QrHandshakeUiEvent()
    object CancelRoomCodeMatch                         : QrHandshakeUiEvent()
    object GenerateCode                                : QrHandshakeUiEvent()
    object ToggleScanMode                              : QrHandshakeUiEvent()
    data class OnCodeScanned(val code: String)         : QrHandshakeUiEvent()
    data class ManualCodeEntered(val code: String)     : QrHandshakeUiEvent()
    object ConfirmConnect                              : QrHandshakeUiEvent()
    object Dismiss                                     : QrHandshakeUiEvent()
}

sealed class QrHandshakeUiEffect : UiEffect {
    data class RoomCreated(val roomId: String) : QrHandshakeUiEffect()
    object Dismissed                           : QrHandshakeUiEffect()
    data class ShowError(val msg: String)      : QrHandshakeUiEffect()
}
