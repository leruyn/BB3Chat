package com.bb3.bb3chat.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bb3.bb3chat.feature.auth.presentation.PinScreen
import com.bb3.bb3chat.feature.disguise.presentation.CalculatorMaskScreen
import com.bb3.bb3chat.feature.messaging.presentation.chatroom.ChatroomScreen
import com.bb3.bb3chat.feature.messaging.presentation.chatroom.ChatroomViewModel
import com.bb3.bb3chat.feature.messaging.presentation.inbox.InboxScreen
import com.bb3.bb3chat.feature.pairing.presentation.QrHandshakeScreen
import com.bb3.bb3chat.feature.store.presentation.VipStoreScreen
import com.bb3.bb3chat.feature.auth.presentation.PinViewModel
import org.koin.compose.koinInject

/**
 * Root composable — owns the entire navigation back stack.
 *
 * Flow:
 *   CalculatorMask  ──unlock──▶  PinScreen  ──realPIN──▶  Inbox  ──room──▶  Chatroom
 *                                                   └──decoyPIN──▶  DecoyInbox (TBD)
 *
 * Panic from ANY screen → back to CalculatorMask (session key wiped by TriggerLocalPanicUseCase)
 */
@Composable
fun BB3ChatApp(
    secretCode: String = "14022026"   // in prod read from EncryptedSharedPrefs
) {
    var screen by remember { mutableStateOf<Screen>(Screen.Calculator) }

    when (val s = screen) {

        // ── S9 Calculator Mask ─────────────────────────────────────────────
        Screen.Calculator -> {
            CalculatorMaskScreen(
                secretCode       = secretCode,
                onSecretUnlocked = { screen = Screen.Pin }
            )
        }

        // ── S10 PIN Screen ─────────────────────────────────────────────────
        Screen.Pin -> {
            val pinViewModel: PinViewModel = koinInject()
            PinScreen(
                viewModel     = pinViewModel,
                onRealAccess  = { screen = Screen.Inbox },
                onDecoyAccess = { screen = Screen.DecoyInbox }
            )
        }

        // ── S11 Inbox ──────────────────────────────────────────────────────
        Screen.Inbox -> {
            InboxScreen(
                onOpenRoom              = { roomId -> screen = Screen.Chatroom(roomId) },
                onOpenPairing           = { screen = Screen.QrHandshake },
                onNavigateToCalculator  = { screen = Screen.Calculator }
            )
        }

        // ── Decoy Inbox (placeholder) ──────────────────────────────────────
        Screen.DecoyInbox -> {
            InboxScreen(
                onOpenRoom              = { /* decoy rooms are empty */ },
                onNavigateToCalculator  = { screen = Screen.Calculator }
            )
        }

        // ── S11 Chatroom ───────────────────────────────────────────────────
        is Screen.Chatroom -> {
            val factory: ChatroomViewModelFactory = koinInject()
            val viewModel = remember(s.roomId) { factory.create(s.roomId) }
            ChatroomScreen(
                viewModel              = viewModel,
                onNavigateBack         = { screen = Screen.Inbox },
                onNavigateToCalculator = { screen = Screen.Calculator }
            )
        }

        // ── S15 QR Handshake ────────────────────────────────────────────────
        Screen.QrHandshake -> {
            QrHandshakeScreen(
                onRoomCreated = { roomId -> screen = Screen.Chatroom(roomId) },
                onDismiss     = { screen = Screen.Inbox }
            )
        }

        // ── S17 VIP Store ───────────────────────────────────────────────────
        Screen.VipStore -> {
            VipStoreScreen(
                onDismiss = { screen = Screen.Inbox },
                onPayment = { /* TODO: deep-link to payment provider */ }
            )
        }
    }
}

// ─────────────────────────── Screen destinations ────────────────────────────

sealed class Screen {
    object Calculator : Screen()
    object Pin        : Screen()
    object Inbox      : Screen()
    object DecoyInbox : Screen()
    object QrHandshake: Screen()
    object VipStore   : Screen()
    data class Chatroom(val roomId: String) : Screen()
}
