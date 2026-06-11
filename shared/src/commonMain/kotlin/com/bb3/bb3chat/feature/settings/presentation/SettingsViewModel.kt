package com.bb3.bb3chat.feature.settings.presentation

import com.bb3.bb3chat.core.disguise.DisguiseConfig
import com.bb3.bb3chat.core.disguise.DisguiseType
import com.bb3.bb3chat.core.platform.FakePushNotifier
import com.bb3.bb3chat.core.platform.FakePushTemplates
import com.bb3.bb3chat.core.platform.NotificationPermission
import com.bb3.bb3chat.core.security.SecurityPreferences
import com.bb3.bb3chat.core.vip.VipEntitlements
import com.bb3.bb3chat.feature.auth.domain.repository.PinAuthRepository
import com.bb3.bb3chat.presentation.base.BaseViewModel

class SettingsViewModel(
    private val disguiseConfig: DisguiseConfig,
    private val securityPrefs: SecurityPreferences,
    private val vipEntitlements: VipEntitlements,
    private val pinRepository: PinAuthRepository,
    private val fakePushNotifier: FakePushNotifier,
    private val notificationPermission: NotificationPermission
) : BaseViewModel<SettingsUiState, SettingsUiEvent, SettingsUiEffect>(
    SettingsUiState(
        secretCode          = disguiseConfig.getSecretCode(),
        disguiseType        = disguiseConfig.getDisguiseType(),
        safeHoursEnabled    = securityPrefs.isSafeHoursEnabled(),
        safeHoursStart      = securityPrefs.getSafeHoursStart(),
        safeHoursEnd        = securityPrefs.getSafeHoursEnd(),
        panicFlipEnabled    = securityPrefs.isPanicFlipEnabled(),
        panicWipeEnabled    = securityPrefs.isPanicWipeEnabled(),
        fakePushEnabled     = securityPrefs.isFakePushEnabled(),
        deadmanEnabled      = securityPrefs.isDeadmanEnabled(),
        deadmanHours        = securityPrefs.getDeadmanTriggerHours(),
        hasSafeHoursVip     = vipEntitlements.hasSafeHours(),
        hasPremiumDisguises = vipEntitlements.hasPremiumDisguises(),
        hasFakePushVip      = vipEntitlements.hasFakePush(),
        hasDeadmanVip       = vipEntitlements.hasDeadmanSwitch()
    )
) {

    override suspend fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.SecretCodeChanged ->
                updateState { copy(secretCode = event.value, saved = false, error = null) }
            is SettingsUiEvent.DecoyPinChanged ->
                updateState { copy(decoyPin = event.value, saved = false, error = null) }
            is SettingsUiEvent.DisguiseTypeSelected -> {
                if (event.type != DisguiseType.CALCULATOR && !currentState.hasPremiumDisguises) return
                updateState { copy(disguiseType = event.type, saved = false, error = null) }
            }
            is SettingsUiEvent.SafeHoursToggled ->
                updateState { copy(safeHoursEnabled = event.enabled, saved = false, error = null) }
            is SettingsUiEvent.SafeHoursStartChanged ->
                updateState { copy(safeHoursStart = event.hour.coerceIn(0, 23), saved = false, error = null) }
            is SettingsUiEvent.SafeHoursEndChanged ->
                updateState { copy(safeHoursEnd = event.hour.coerceIn(0, 23), saved = false, error = null) }
            is SettingsUiEvent.PanicFlipToggled ->
                updateState { copy(panicFlipEnabled = event.enabled, saved = false, error = null) }
            is SettingsUiEvent.PanicWipeToggled ->
                updateState { copy(panicWipeEnabled = event.enabled, saved = false, error = null) }
            is SettingsUiEvent.FakePushToggled -> {
                if (!currentState.hasFakePushVip && event.enabled) return
                updateState { copy(fakePushEnabled = event.enabled, saved = false, error = null) }
                if (currentState.hasFakePushVip) {
                    securityPrefs.setFakePushEnabled(event.enabled)
                    if (event.enabled) {
                        notificationPermission.ensureGranted()
                    }
                }
            }
            is SettingsUiEvent.DeadmanToggled -> {
                if (!currentState.hasDeadmanVip && event.enabled) return
                updateState { copy(deadmanEnabled = event.enabled, saved = false, error = null) }
            }
            is SettingsUiEvent.DeadmanHoursChanged ->
                updateState {
                    copy(deadmanHours = event.hours.coerceIn(12, 72), saved = false, error = null)
                }
            is SettingsUiEvent.TestFakePush -> sendTestFakePush()
            is SettingsUiEvent.Save -> save()
            is SettingsUiEvent.Dismiss -> emitEffect(SettingsUiEffect.Dismissed)
        }
    }

    private suspend fun save() {
        val code = currentState.secretCode.trim()
        if (code.length < 4) {
            updateState { copy(error = "Mã kích hoạt phải có ít nhất 4 ký tự") }
            return
        }

        val decoy = currentState.decoyPin.trim()
        if (decoy.isNotEmpty()) {
            if (decoy.length < 4) {
                updateState { copy(error = "PIN giả phải có ít nhất 4 số") }
                return
            }
            if (pinRepository.isSameAsRealPin(decoy)) {
                updateState { copy(error = "PIN giả không được trùng PIN thật") }
                return
            }
            pinRepository.setDecoyPin(decoy)
        }

        val disguise = if (currentState.disguiseType == DisguiseType.CALCULATOR ||
            currentState.hasPremiumDisguises
        ) {
            currentState.disguiseType
        } else {
            DisguiseType.CALCULATOR
        }

        disguiseConfig.setSecretCode(code)
        disguiseConfig.setDisguiseType(disguise)
        securityPrefs.setPanicFlipEnabled(currentState.panicFlipEnabled)
        securityPrefs.setPanicWipeEnabled(currentState.panicWipeEnabled)

        if (currentState.hasFakePushVip) {
            securityPrefs.setFakePushEnabled(currentState.fakePushEnabled)
        }

        if (currentState.hasDeadmanVip) {
            securityPrefs.setDeadmanEnabled(currentState.deadmanEnabled)
            securityPrefs.setDeadmanTriggerHours(currentState.deadmanHours)
            if (currentState.deadmanEnabled) {
                securityPrefs.recordActivity()
            }
        }

        if (currentState.hasSafeHoursVip) {
            securityPrefs.setSafeHoursEnabled(currentState.safeHoursEnabled)
            securityPrefs.setSafeHoursRange(currentState.safeHoursStart, currentState.safeHoursEnd)
        }

        val shouldDemoPush = currentState.hasFakePushVip && currentState.fakePushEnabled
        updateState {
            copy(
                saved        = true,
                error        = null,
                decoyPin     = "",
                disguiseType = disguise,
                fakePushSent = false
            )
        }
        if (shouldDemoPush) {
            sendTestFakePush()
        }
    }

    private suspend fun sendTestFakePush() {
        if (!notificationPermission.areGranted()) {
            val granted = notificationPermission.ensureGranted()
            if (!granted) {
                updateState {
                    copy(
                        error = "Cần bật quyền thông báo cho BB3Chat trong Cài đặt hệ thống"
                    )
                }
                return
            }
        }
        fakePushNotifier.show(FakePushTemplates.random())
        if (!notificationPermission.areGranted()) {
            updateState {
                copy(
                    error = "Quyền thông báo chưa bật — kiểm tra Cài đặt → Ứng dụng → BB3Chat → Thông báo",
                    fakePushSent = false
                )
            }
            return
        }
        updateState { copy(fakePushSent = true, error = null) }
    }
}
