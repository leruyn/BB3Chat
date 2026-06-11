package com.bb3.bb3chat.presentation.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import com.bb3.bb3chat.core.platform.ApplySystemStatusBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.bb3.bb3chat.core.disguise.DisguiseConfig
import com.bb3.bb3chat.core.profile.UserProfileRepository
import com.bb3.bb3chat.core.security.SafeHoursSession
import com.bb3.bb3chat.core.security.SecurityPreferences
import com.bb3.bb3chat.core.util.currentLocalTime
import com.bb3.bb3chat.core.util.isWithinSafeHours
import com.bb3.bb3chat.core.vip.VipEntitlements
import com.bb3.bb3chat.feature.auth.presentation.PinScreen
import com.bb3.bb3chat.feature.auth.presentation.PinViewModel
import com.bb3.bb3chat.feature.disguise.presentation.DecoyNotesScreen
import com.bb3.bb3chat.feature.disguise.presentation.DisguiseMaskScreen
import com.bb3.bb3chat.feature.messaging.presentation.chatroom.ChatroomScreen
import com.bb3.bb3chat.feature.messaging.presentation.chatroom.ChatroomViewModel
import com.bb3.bb3chat.feature.messaging.presentation.inbox.InboxScreen
import com.bb3.bb3chat.feature.onboarding.presentation.OnboardingScreen
import com.bb3.bb3chat.feature.pairing.presentation.QrHandshakeScreen
import com.bb3.bb3chat.feature.profile.presentation.ProfileSetupScreen
import com.bb3.bb3chat.feature.security.domain.usecase.CheckDeadmanUseCase
import com.bb3.bb3chat.feature.security.domain.usecase.ExecutePanicUseCase
import com.bb3.bb3chat.feature.security.presentation.IntruderGalleryScreen
import com.bb3.bb3chat.feature.security.presentation.PanicSensorListener
import com.bb3.bb3chat.feature.security.presentation.SafeHoursGateScreen
import com.bb3.bb3chat.feature.settings.presentation.SettingsScreen
import com.bb3.bb3chat.feature.store.presentation.VipStoreScreen
import com.bb3.bb3chat.feature.store.presentation.VipStoreViewModel
import org.koin.compose.koinInject

@Composable
fun BB3ChatApp() {
    var screen by remember { mutableStateOf<Screen>(Screen.Calculator) }
    val disguiseConfig: DisguiseConfig = koinInject()
    val securityPrefs: SecurityPreferences = koinInject()
    val vipEntitlements: VipEntitlements = koinInject()
    val userProfile: UserProfileRepository = koinInject()
    val executePanic: ExecutePanicUseCase = koinInject()
    val checkDeadman: CheckDeadmanUseCase = koinInject()

    fun navigateToCalculator() {
        SafeHoursSession.reset()
        screen = Screen.Calculator
    }

    fun onPanic() {
        executePanic()
        navigateToCalculator()
    }

    fun navigateToInboxOrGate() {
        securityPrefs.recordActivity()
        screen = if (shouldShowSafeHoursGate(securityPrefs, vipEntitlements)) {
            Screen.SafeHoursGate
        } else {
            Screen.Inbox
        }
    }

    fun navigateAfterRealPin() {
        if (checkDeadman()) {
            navigateToCalculator()
            return
        }
        screen = when {
            !userProfile.isOnboardingDone()     -> Screen.Onboarding
            !userProfile.isProfileConfigured()  -> Screen.ProfileSetup
            else                                -> if (shouldShowSafeHoursGate(securityPrefs, vipEntitlements)) {
                Screen.SafeHoursGate
            } else {
                Screen.Inbox
            }
        }
        if (screen == Screen.Inbox) {
            securityPrefs.recordActivity()
        }
    }

    val panicSensorActive = screen !is Screen.Calculator &&
        screen !is Screen.Pin &&
        screen !is Screen.DecoyInbox

    PanicSensorListener(
        enabled = panicSensorActive && securityPrefs.isPanicFlipEnabled(),
        onPanic = { onPanic() }
    )

    val chrome = screen.chrome(disguiseConfig.getDisguiseType())

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(chrome.statusBarBackground))
        ApplySystemStatusBar(chrome.statusBarStyle)

        val contentModifier = if (screen is Screen.DecoyInbox) {
            Modifier.fillMaxSize()
        } else {
            Modifier.fillMaxSize().statusBarsPadding()
        }
        Box(contentModifier) {
        when (val s = screen) {

            Screen.Calculator -> {
                DisguiseMaskScreen(
                    disguiseType     = disguiseConfig.getDisguiseType(),
                    secretCode       = disguiseConfig.getSecretCode(),
                    onSecretUnlocked = { screen = Screen.Pin }
                )
            }

            Screen.Pin -> {
                val pinViewModel: PinViewModel = koinInject()
                PinScreen(
                    viewModel     = pinViewModel,
                    onRealAccess  = { navigateAfterRealPin() },
                    onDecoyAccess = { screen = Screen.DecoyInbox }
                )
            }

            Screen.Onboarding -> {
                OnboardingScreen(
                    onComplete = {
                        userProfile.setOnboardingDone()
                        screen = if (!userProfile.isProfileConfigured()) {
                            Screen.ProfileSetup
                        } else {
                            if (shouldShowSafeHoursGate(securityPrefs, vipEntitlements)) {
                                Screen.SafeHoursGate
                            } else {
                                Screen.Inbox
                            }
                        }
                        if (screen == Screen.Inbox) securityPrefs.recordActivity()
                    }
                )
            }

            Screen.ProfileSetup -> {
                ProfileSetupScreen(onComplete = { navigateToInboxOrGate() })
            }

            Screen.SafeHoursGate -> {
                val time = currentLocalTime()
                SafeHoursGateScreen(
                    currentHour   = time.hour,
                    currentMinute = time.minute,
                    allowedStart  = securityPrefs.getSafeHoursStart(),
                    allowedEnd    = securityPrefs.getSafeHoursEnd(),
                    onUnlocked    = {
                        securityPrefs.recordActivity()
                        screen = Screen.Inbox
                    },
                    onEmergency   = {
                        SafeHoursSession.emergencyBypass = true
                        securityPrefs.recordActivity()
                        screen = Screen.Inbox
                    }
                )
            }

            Screen.Inbox -> {
                InboxScreen(
                    onOpenRoom             = { roomId -> screen = Screen.Chatroom(roomId) },
                    onOpenPairing          = { screen = Screen.QrHandshake },
                    onOpenSettings         = { screen = Screen.Settings },
                    onOpenStore            = { screen = Screen.VipStore },
                    onOpenIntruderGallery  = { screen = Screen.IntruderGallery },
                    onNavigateToCalculator = { navigateToCalculator() }
                )
            }

            Screen.DecoyInbox -> {
                DecoyNotesScreen(onNavigateToCalculator = { navigateToCalculator() })
            }

            is Screen.Chatroom -> {
                val factory: ChatroomViewModelFactory = koinInject()
                val viewModel = remember(s.roomId) { factory.create(s.roomId) }
                ChatroomScreen(
                    viewModel              = viewModel,
                    onNavigateBack         = { screen = Screen.Inbox },
                    onNavigateToCalculator = { navigateToCalculator() }
                )
            }

            Screen.QrHandshake -> {
                QrHandshakeScreen(
                    onRoomCreated = { roomId -> screen = Screen.Chatroom(roomId) },
                    onDismiss     = { screen = Screen.Inbox }
                )
            }

            Screen.Settings -> {
                SettingsScreen(onDismiss = { screen = Screen.Inbox })
            }

            Screen.VipStore -> {
                val storeViewModel: VipStoreViewModel = koinInject()
                VipStoreScreen(
                    onDismiss = { screen = Screen.Inbox },
                    onPayment = { planId -> storeViewModel.onPurchaseSuccess(planId) }
                )
            }

            Screen.IntruderGallery -> {
                IntruderGalleryScreen(onDismiss = { screen = Screen.Inbox })
            }
        }
        }
    }
}

private fun shouldShowSafeHoursGate(
    securityPrefs: SecurityPreferences,
    vipEntitlements: VipEntitlements
): Boolean {
    if (!vipEntitlements.hasSafeHours()) return false
    if (!securityPrefs.isSafeHoursEnabled()) return false
    if (SafeHoursSession.emergencyBypass) return false
    val time = currentLocalTime()
    return !isWithinSafeHours(time.hour, securityPrefs.getSafeHoursStart(), securityPrefs.getSafeHoursEnd())
}

sealed class Screen {
    object Calculator       : Screen()
    object Pin              : Screen()
    object Onboarding       : Screen()
    object ProfileSetup     : Screen()
    object SafeHoursGate    : Screen()
    object Inbox            : Screen()
    object DecoyInbox       : Screen()
    object QrHandshake      : Screen()
    object Settings         : Screen()
    object VipStore         : Screen()
    object IntruderGallery  : Screen()
    data class Chatroom(val roomId: String) : Screen()
}
