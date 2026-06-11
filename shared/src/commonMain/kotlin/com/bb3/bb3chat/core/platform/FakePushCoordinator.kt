package com.bb3.bb3chat.core.platform

import com.bb3.bb3chat.core.security.SecurityPreferences
import com.bb3.bb3chat.core.vip.VipEntitlements

/** Sends benign fake notifications when the user leaves with an unsent draft. */
class FakePushCoordinator(
    private val securityPrefs: SecurityPreferences,
    private val vipEntitlements: VipEntitlements,
    private val fakePushNotifier: FakePushNotifier,
    private val notificationPermission: NotificationPermission
) {
    private var draftText: String = ""

    fun start() {
        AppBackgroundCallbacks.register { maybeSendForUnsentDraft() }
    }

    fun updateDraft(text: String) {
        draftText = text.trim()
    }

    fun clearDraft() {
        draftText = ""
    }

    suspend fun maybeSendForUnsentDraft() {
        if (draftText.isBlank()) return
        if (!securityPrefs.isFakePushEnabled()) return
        if (!vipEntitlements.hasFakePush()) return
        if (!notificationPermission.areGranted()) return
        fakePushNotifier.show(FakePushTemplates.random())
        clearDraft()
    }
}
