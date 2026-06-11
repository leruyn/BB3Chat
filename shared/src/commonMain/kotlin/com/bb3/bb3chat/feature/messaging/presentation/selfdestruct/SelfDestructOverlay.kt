package com.bb3.bb3chat.feature.messaging.presentation.selfdestruct

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import kotlinx.coroutines.delay

/**
 * Full-screen overlay hiển thị đếm ngược tự huỷ message.
 *
 * Gọi khi user nhấn vào message có [selfDestruct = true].
 * Khi đếm về 0 → gọi [onExpired] để ViewModel xoá message khỏi DB + Firestore.
 *
 * @param totalSecs  tổng thời gian tự huỷ (vd: 10s, 30s)
 * @param onExpired  callback khi hết giờ
 * @param onDismiss  callback khi user nhấn ngoài (message vẫn bị tính là đã đọc)
 */
@Composable
fun SelfDestructOverlay(
    totalSecs : Int,
    onExpired : () -> Unit,
    onDismiss : () -> Unit = {}
) {
    var remaining by remember { mutableIntStateOf(totalSecs) }
    val sweepAnim = remember { Animatable(360f) }

    // Tick mỗi giây
    LaunchedEffect(totalSecs) {
        while (remaining > 0) {
            delay(1_000)
            remaining -= 1
            sweepAnim.animateTo(
                targetValue    = (remaining.toFloat() / totalSecs) * 360f,
                animationSpec  = tween(900, easing = LinearEasing)
            )
        }
        onExpired()
    }

    val dangerFraction = remaining.toFloat() / totalSecs
    val ringColor = when {
        dangerFraction > 0.5f -> Color(0xFF30D158)   // green
        dangerFraction > 0.25f -> Color(0xFFFF9F0A)  // amber
        else                   -> BB3Danger           // red
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xDD000000)),           // dim background
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(BB3Card)
                .padding(40.dp)
        ) {
            Text("💣", fontSize = 36.sp)
            Spacer(Modifier.height(20.dp))

            // Ring countdown
            Box(
                Modifier
                    .size(110.dp)
                    .drawBehind {
                        // Track
                        drawArc(
                            color       = Color(0xFF3A3A3C),
                            startAngle  = -90f,
                            sweepAngle  = 360f,
                            useCenter   = false,
                            style       = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Progress
                        drawArc(
                            color       = ringColor,
                            startAngle  = -90f,
                            sweepAngle  = sweepAnim.value,
                            useCenter   = false,
                            style       = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text       = remaining.toString(),
                    color      = BB3TextPrim,
                    fontSize   = 38.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(18.dp))
            Text("Tin nhắn tự huỷ sau", color = BB3TextSec, fontSize = 14.sp)
            Text(
                formatSeconds(remaining),
                color      = ringColor,
                fontSize   = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatSeconds(s: Int): String = when {
    s >= 60  -> "${s / 60}m ${s % 60}s"
    else     -> "${s}s"
}
