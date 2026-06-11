package com.bb3.bb3chat.feature.disguise.presentation

import androidx.compose.runtime.Composable
import com.bb3.bb3chat.core.disguise.DisguiseType

@Composable
fun DisguiseMaskScreen(
    disguiseType: DisguiseType,
    secretCode: String,
    onSecretUnlocked: () -> Unit
) {
    when (disguiseType) {
        DisguiseType.CALCULATOR -> CalculatorMaskScreen(
            secretCode       = secretCode,
            onSecretUnlocked = onSecretUnlocked
        )
        DisguiseType.WEATHER,
        DisguiseType.MUSIC,
        DisguiseType.NEWS,
        DisguiseType.DICTIONARY,
        DisguiseType.BANKING -> SearchUnlockMaskScreen(
            variant          = disguiseType,
            secretCode       = secretCode,
            onSecretUnlocked = onSecretUnlocked
        )
    }
}
