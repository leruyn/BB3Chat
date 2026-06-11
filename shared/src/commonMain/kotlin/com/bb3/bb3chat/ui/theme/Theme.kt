package com.bb3.bb3chat.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BB3Black      = Color(0xFF0A0A0A)
val BB3Surface    = Color(0xFF1C1C1E)
val BB3Card       = Color(0xFF2C2C2E)
val BB3Border     = Color(0xFF3A3A3C)
val BB3Primary    = Color(0xFF30D158)   // green accent
val BB3Danger     = Color(0xFFFF453A)   // red panic
val BB3Amber      = Color(0xFFFFD60A)   // gold
val BB3Blue       = Color(0xFF0A84FF)
val BB3TextPrim   = Color(0xFFFFFFFF)
val BB3TextSec    = Color(0xFF8E8E93)
val BB3CalcButton = Color(0xFF1C1C1E)
val BB3CalcOp     = Color(0xFFFF9F0A)   // orange operator buttons
val BB3CalcFunc   = Color(0xFF3A3A3C)   // grey function buttons

private val DarkColors = darkColorScheme(
    primary         = BB3Primary,
    onPrimary       = BB3Black,
    background      = BB3Black,
    surface         = BB3Surface,
    onSurface       = BB3TextPrim,
    error           = BB3Danger,
)

@Composable
fun BB3ChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        content     = content
    )
}
