package com.bb3.bb3chat.feature.auth.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Blue
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3Primary
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import org.koin.compose.koinInject

private val DIGIT_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("",  "0", "⌫"),
)

@Composable
fun PinScreen(
    viewModel: PinViewModel,
    onRealAccess: () -> Unit,
    onDecoyAccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val shakeOffset = remember { Animatable(0f) }

    // Handle effects
    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is PinUiEffect.NavigateToRealInbox  -> onRealAccess()
                is PinUiEffect.NavigateToDecoySpace -> onDecoyAccess()
                is PinUiEffect.NavigateToSetup      -> { /* stay — setup mode */ }
                is PinUiEffect.ShowError            -> { /* handled via shakeError state */ }
            }
        }
    }

    // Shake animation khi sai PIN
    LaunchedEffect(state.shakeError) {
        if (state.shakeError) {
            repeat(4) {
                shakeOffset.animateTo(14f, tween(60))
                shakeOffset.animateTo(-14f, tween(60))
            }
            shakeOffset.animateTo(0f, tween(60))
        }
    }

    val titleText = when {
        state.isSetupMode && state.setupStep == SetupStep.ENTER_REAL   -> "Tạo mã PIN thật (1/3)"
        state.isSetupMode && state.setupStep == SetupStep.CONFIRM_REAL -> "Xác nhận PIN thật (2/3)"
        state.isSetupMode && state.setupStep == SetupStep.ENTER_DECOY  -> "Tạo mã PIN giả (3/3)"
        else                                                            -> "Nhập mã PIN"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BB3Black)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(80.dp))

        // Title
        Text(
            text       = titleText,
            color      = BB3TextSec,
            fontSize   = 17.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(Modifier.height(32.dp))

        // PIN dots — shake khi sai
        Row(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.graphicsLayer { translationX = shakeOffset.value }
        ) {
            repeat(4) { index ->
                val filled = index < state.enteredDigits.length
                val dotColor = when {
                    filled && state.shakeError -> BB3Danger
                    filled                     -> BB3Primary
                    else                       -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(BB3Black)
                        .border(
                            width = 1.5.dp,
                            color = if (filled && state.shakeError) BB3Danger
                                    else if (filled) BB3Primary
                                    else BB3TextSec,
                            shape = CircleShape
                        )
                ) {
                    if (filled) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(dotColor)
                                .align(Alignment.Center)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(48.dp))

        // Numpad
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            DIGIT_ROWS.forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    row.forEach { label ->
                        PinKey(
                            label = label,
                            onClick = {
                                when (label) {
                                    "⌫" -> viewModel.handleEvent(PinUiEvent.BackspacePressed)
                                    ""  -> { /* nút trống */ }
                                    else -> viewModel.handleEvent(PinUiEvent.DigitPressed(label))
                                }
                            }
                        )
                    }
                }
            }
        }

        // Setup hints
        when {
            state.isSetupMode && state.setupStep == SetupStep.CONFIRM_REAL -> {
                Text(
                    text     = "Nhập lại PIN thật để xác nhận",
                    color    = BB3TextSec,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            state.isSetupMode && state.setupStep == SetupStep.ENTER_DECOY -> {
                Text(
                    text     = "PIN giả phải khác PIN thật → hiện ghi chú an toàn",
                    color    = BB3TextSec,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PinKey(label: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(CircleShape)
            .background(
                when {
                    label.isEmpty() -> Color.Transparent
                    isPressed       -> Color(0xFF3A3A3C)
                    else            -> Color(0xFF1C1C1E)
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                enabled           = label.isNotEmpty(),
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (label.isNotEmpty()) {
            Text(
                text       = label,
                color      = BB3TextPrim,
                fontSize   = if (label == "⌫") 22.sp else 30.sp,
                fontWeight = FontWeight.Light
            )
        }
    }
}
