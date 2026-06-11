@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bb3.bb3chat

import androidx.compose.ui.window.ComposeUIViewController
import com.bb3.bb3chat.core.bridge.PushBridge
import com.bb3.bb3chat.core.bridge.SessionManagerHolder
import com.bb3.bb3chat.core.bridge.TokenBridge
import com.bb3.bb3chat.feature.messaging.domain.repository.MessageRepository
import com.bb3.bb3chat.core.crypto.SessionManager
import com.bb3.bb3chat.core.di.authModule
import com.bb3.bb3chat.core.di.coreModule
import com.bb3.bb3chat.core.di.iosModule
import com.bb3.bb3chat.core.di.messagingModule
import com.bb3.bb3chat.core.di.pairingModule
import com.bb3.bb3chat.core.di.profileModule
import com.bb3.bb3chat.core.di.securityModule
import com.bb3.bb3chat.core.di.settingsModule
import com.bb3.bb3chat.core.di.storeModule
import com.bb3.bb3chat.core.di.tokenModule
import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository
import com.bb3.bb3chat.presentation.navigation.BB3ChatApp
import com.bb3.bb3chat.ui.theme.BB3ChatTheme
import org.koin.core.context.startKoin
import platform.UIKit.UIViewController

private var koinInitialized = false

/** Exported to Swift — top-level `initIosKoin()` is not visible in shared.h. */
object IosAppBootstrap {
    fun start() {
        if (koinInitialized) return
        koinInitialized = true
        val app = startKoin {
            modules(
                coreModule, authModule, messagingModule, securityModule,
                tokenModule, storeModule, settingsModule, profileModule, pairingModule, iosModule
            )
        }
        SessionManagerHolder.bind(app.koin.get<SessionManager>())
        TokenBridge.bind(app.koin.get<TokenRepository>())
        PushBridge.bind(
            messageRepository = app.koin.get<MessageRepository>(),
            sessionManager    = app.koin.get<SessionManager>()
        )
    }
}

fun MainViewController(): UIViewController = ComposeUIViewController {
    BB3ChatTheme {
        BB3ChatApp()
    }
}
