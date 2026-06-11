package com.bb3.bb3chat.feature.disguise.presentation

import com.bb3.bb3chat.core.util.formatCalculatorValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3CalcFunc
import com.bb3.bb3chat.ui.theme.BB3CalcOp
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import kotlinx.coroutines.delay

// Cấu trúc bàn phím máy tính — giống iOS Calculator
private data class CalcKey(
    val label: String,
    val type: KeyType,
    val span: Int = 1         // 2 = chiếm 2 cột (nút 0)
)

private enum class KeyType { FUNC, OP, NUM }

private val KEYPAD = listOf(
    listOf(CalcKey("AC", KeyType.FUNC), CalcKey("+/-", KeyType.FUNC), CalcKey("%", KeyType.FUNC), CalcKey("÷", KeyType.OP)),
    listOf(CalcKey("7",  KeyType.NUM),  CalcKey("8",  KeyType.NUM),  CalcKey("9",  KeyType.NUM),  CalcKey("×", KeyType.OP)),
    listOf(CalcKey("4",  KeyType.NUM),  CalcKey("5",  KeyType.NUM),  CalcKey("6",  KeyType.NUM),  CalcKey("−", KeyType.OP)),
    listOf(CalcKey("1",  KeyType.NUM),  CalcKey("2",  KeyType.NUM),  CalcKey("3",  KeyType.NUM),  CalcKey("+", KeyType.OP)),
    listOf(CalcKey("0",  KeyType.NUM, span = 2),                     CalcKey(".",  KeyType.NUM),  CalcKey("=", KeyType.OP)),
)

@Composable
fun CalculatorMaskScreen(
    secretCode: String,             // từ storage, e.g. "14022026"
    onSecretUnlocked: () -> Unit    // navigate to PinScreen
) {
    var display by remember { mutableStateOf("0") }
    var inputBuffer by remember { mutableStateOf("") }  // secret code buffer (invisible)
    var lastOp by remember { mutableStateOf("") }
    var storedValue by remember { mutableStateOf(0.0) }
    var waitingNewInput by remember { mutableStateOf(false) }
    var showFlash by remember { mutableStateOf(false) }

    // Khi buffer khớp secret code → unlock
    LaunchedEffect(inputBuffer) {
        if (inputBuffer.length >= secretCode.length) {
            val tail = inputBuffer.takeLast(secretCode.length)
            if (tail == secretCode) {
                showFlash = true
                delay(180)
                onSecretUnlocked()
            }
            // Giữ buffer không quá dài
            if (inputBuffer.length > 20) inputBuffer = inputBuffer.takeLast(20)
        }
    }

    val bgColor by animateColorAsState(
        targetValue = if (showFlash) Color(0xFF1A3A1A) else BB3Black,
        animationSpec = tween(200), label = "bg"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        // Display
        Text(
            text = display,
            color = BB3TextPrim,
            fontSize = when {
                display.length > 12 -> 36.sp
                display.length > 8  -> 52.sp
                else                -> 72.sp
            },
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp, end = 4.dp)
        )

        Spacer(Modifier.height(8.dp))

        // Keypad rows
        KEYPAD.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { key ->
                    val weight = key.span.toFloat()
                    CalcButton(
                        label    = key.label,
                        keyType  = key.type,
                        modifier = Modifier.weight(weight),
                        onClick  = {
                            handleKeyPress(
                                key          = key,
                                display      = display,
                                inputBuffer  = inputBuffer,
                                lastOp       = lastOp,
                                storedValue  = storedValue,
                                waitingNew   = waitingNewInput,
                                onDisplay    = { display = it },
                                onBuffer     = { inputBuffer = it },
                                onLastOp     = { lastOp = it },
                                onStored     = { storedValue = it },
                                onWaiting    = { waitingNewInput = it }
                            )
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun CalcButton(
    label: String,
    keyType: KeyType,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = tween(80), label = "scale"
    )

    val bgColor = when (keyType) {
        KeyType.FUNC -> BB3CalcFunc
        KeyType.OP   -> BB3CalcOp
        KeyType.NUM  -> Color(0xFF333335)
    }
    val textColor = when (keyType) {
        KeyType.FUNC -> BB3TextPrim
        KeyType.OP   -> BB3Black
        KeyType.NUM  -> BB3TextPrim
    }
    val pressedColor = when (keyType) {
        KeyType.FUNC -> Color(0xFF636366)
        KeyType.OP   -> Color(0xFFFFCC02)
        KeyType.NUM  -> Color(0xFF636366)
    }

    Box(
        modifier = modifier
            .height(80.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(if (isPressed) pressedColor else bgColor)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text       = label,
            color      = textColor,
            fontSize   = if (label == "AC" || label == "+/-" || label == "%") 22.sp else 32.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

private fun handleKeyPress(
    key: CalcKey,
    display: String,
    inputBuffer: String,
    lastOp: String,
    storedValue: Double,
    waitingNew: Boolean,
    onDisplay: (String) -> Unit,
    onBuffer: (String) -> Unit,
    onLastOp: (String) -> Unit,
    onStored: (Double) -> Unit,
    onWaiting: (Boolean) -> Unit
) {
    when (key.type) {
        KeyType.NUM -> {
            val digit = key.label
            onBuffer(inputBuffer + digit.filter { it.isDigit() })

            when {
                digit == "." -> {
                    if (waitingNew) { onDisplay("0."); onWaiting(false) }
                    else if (!display.contains(".")) onDisplay(display + ".")
                }
                waitingNew || display == "0" -> {
                    onDisplay(digit)
                    onWaiting(false)
                }
                else -> {
                    if (display.replace("-","").replace(".","").length < 12)
                        onDisplay(display + digit)
                }
            }
        }

        KeyType.OP -> {
            when (key.label) {
                "=" -> {
                    val current = display.toDoubleOrNull() ?: 0.0
                    val result = compute(storedValue, current, lastOp)
                    onDisplay(formatResult(result))
                    onStored(result)
                    onLastOp("")
                    onWaiting(true)
                }
                else -> {
                    val current = display.toDoubleOrNull() ?: 0.0
                    if (lastOp.isNotEmpty() && !waitingNew) {
                        val result = compute(storedValue, current, lastOp)
                        onDisplay(formatResult(result))
                        onStored(result)
                    } else {
                        onStored(current)
                    }
                    onLastOp(key.label)
                    onWaiting(true)
                }
            }
        }

        KeyType.FUNC -> {
            when (key.label) {
                "AC"  -> {
                    onDisplay("0")
                    onBuffer("")
                    onLastOp("")
                    onStored(0.0)
                    onWaiting(false)
                }
                "+/-" -> {
                    val v = display.toDoubleOrNull() ?: 0.0
                    onDisplay(formatResult(-v))
                }
                "%"   -> {
                    val v = display.toDoubleOrNull() ?: 0.0
                    onDisplay(formatResult(v / 100.0))
                }
            }
        }
    }
}

private fun compute(a: Double, b: Double, op: String) = when (op) {
    "+"  -> a + b
    "−"  -> a - b
    "×"  -> a * b
    "÷"  -> if (b != 0.0) a / b else 0.0
    else -> b
}

private fun formatResult(v: Double): String {
    if (v.isNaN() || v.isInfinite()) return "Lỗi"
    val long = v.toLong()
    return if (v == long.toDouble() && kotlin.math.abs(v) < 1e12) {
        long.toString()
    } else {
        formatCalculatorValue(v)
    }
}
