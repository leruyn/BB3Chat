package com.bb3.bb3chat.presentation.navigation

import androidx.compose.ui.graphics.Color
import com.bb3.bb3chat.core.disguise.DisguiseType
import com.bb3.bb3chat.core.platform.AppStatusBarStyle
import com.bb3.bb3chat.ui.theme.BB3Black

data class ScreenChrome(
    val statusBarStyle: AppStatusBarStyle,
    val statusBarBackground: Color,
)

fun Screen.chrome(disguiseType: DisguiseType = DisguiseType.CALCULATOR): ScreenChrome = when (this) {
    Screen.DecoyInbox -> ScreenChrome(
        statusBarStyle      = AppStatusBarStyle.LightBackground,
        statusBarBackground = Color(0xFFFFD60A),
    )
    Screen.Calculator -> ScreenChrome(
        statusBarStyle      = AppStatusBarStyle.DarkBackground,
        statusBarBackground = disguiseStatusBarColor(disguiseType),
    )
    else -> ScreenChrome(
        statusBarStyle      = AppStatusBarStyle.DarkBackground,
        statusBarBackground = BB3Black,
    )
}

private fun disguiseStatusBarColor(type: DisguiseType): Color = when (type) {
    DisguiseType.CALCULATOR  -> BB3Black
    DisguiseType.WEATHER     -> Color(0xFF1E3A8A)
    DisguiseType.MUSIC       -> Color(0xFF4C0519)
    DisguiseType.NEWS        -> Color(0xFF7F1D1D)
    DisguiseType.DICTIONARY  -> Color(0xFF14532D)
    DisguiseType.BANKING     -> Color(0xFF064E3B)
}
