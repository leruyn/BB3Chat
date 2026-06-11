package com.bb3.bb3chat.feature.settings.presentation

import com.bb3.bb3chat.core.disguise.DisguiseType
import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

data class SettingsUiState(
    val secretCode           : String       = "",
    val decoyPin             : String       = "",
    val disguiseType         : DisguiseType = DisguiseType.CALCULATOR,
    val safeHoursEnabled     : Boolean      = false,
    val safeHoursStart       : Int          = 8,
    val safeHoursEnd         : Int          = 22,
    val panicFlipEnabled     : Boolean      = false,
    val panicWipeEnabled     : Boolean      = false,
    val fakePushEnabled      : Boolean      = false,
    val deadmanEnabled       : Boolean      = false,
    val deadmanHours         : Int          = 36,
    val hasSafeHoursVip      : Boolean      = false,
    val hasPremiumDisguises  : Boolean      = false,
    val hasFakePushVip       : Boolean      = false,
    val hasDeadmanVip        : Boolean      = false,
    val saved                : Boolean      = false,
    val fakePushSent         : Boolean      = false,
    val error                : String?      = null
) : UiState

sealed class SettingsUiEvent : UiEvent {
    data class SecretCodeChanged(val value: String) : SettingsUiEvent()
    data class DecoyPinChanged(val value: String) : SettingsUiEvent()
    data class DisguiseTypeSelected(val type: DisguiseType) : SettingsUiEvent()
    data class SafeHoursToggled(val enabled: Boolean) : SettingsUiEvent()
    data class SafeHoursStartChanged(val hour: Int) : SettingsUiEvent()
    data class SafeHoursEndChanged(val hour: Int) : SettingsUiEvent()
    data class PanicFlipToggled(val enabled: Boolean) : SettingsUiEvent()
    data class PanicWipeToggled(val enabled: Boolean) : SettingsUiEvent()
    data class FakePushToggled(val enabled: Boolean) : SettingsUiEvent()
    data class DeadmanToggled(val enabled: Boolean) : SettingsUiEvent()
    data class DeadmanHoursChanged(val hours: Int) : SettingsUiEvent()
    object TestFakePush                                    : SettingsUiEvent()
    object Save                                            : SettingsUiEvent()
    object Dismiss                                         : SettingsUiEvent()
}

sealed class SettingsUiEffect : UiEffect {
    object Dismissed : SettingsUiEffect()
}
