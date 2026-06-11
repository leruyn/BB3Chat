package com.bb3.bb3chat.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import com.bb3.bb3chat.core.bridge.StatusBarBridgeHolder

@Composable
actual fun ApplySystemStatusBar(style: AppStatusBarStyle) {
    SideEffect {
        StatusBarBridgeHolder.setStyle(style)
    }
}
