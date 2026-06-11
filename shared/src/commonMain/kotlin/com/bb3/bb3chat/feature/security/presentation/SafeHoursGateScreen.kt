package com.bb3.bb3chat.feature.security.presentation

import com.bb3.bb3chat.core.util.formatTwoDigits
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Border
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3Primary
import com.bb3.bb3chat.ui.theme.BB3Surface
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import kotlinx.coroutines.delay

/**
 * Safe Hours Gate — chặn truy cập ngoài khung giờ an toàn.
 *
 * Hiển thị khi [isBlocked] = true (giờ hiện tại nằm ngoài [allowedStart]..[allowedEnd]).
 * User không thể vào Inbox; chỉ hiện đồng hồ đếm ngược đến giờ mở khóa tiếp theo.
 *
 * Không cần ViewModel riêng — logic tính giờ đơn giản, dùng state nội bộ.
 */
@Composable
fun SafeHoursGateScreen(
    currentHour   : Int,       // 0..23, truyền từ platform
    currentMinute : Int,
    allowedStart  : Int = 8,   // 08:00
    allowedEnd    : Int = 22,  // 22:00
    onUnlocked    : () -> Unit, // khi đã đến giờ → tự động navigate
    onEmergency   : () -> Unit  // bypass khẩn cấp (yêu cầu xác nhận PIN lần 2)
) {
    val isAllowed = currentHour in allowedStart until allowedEnd

    LaunchedEffect(isAllowed) {
        if (isAllowed) onUnlocked()
    }

    // Tính phút còn lại đến giờ mở
    val minutesUntilOpen = remember(currentHour, currentMinute) {
        val openMinTotal  = allowedStart * 60
        val nowMinTotal   = currentHour * 60 + currentMinute
        val nextOpen      = if (nowMinTotal < openMinTotal) openMinTotal
                            else openMinTotal + 24 * 60   // ngày hôm sau
        nextOpen - nowMinTotal
    }

    var countdown by remember { mutableIntStateOf(minutesUntilOpen * 60) }

    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1_000)
            countdown -= 1
        }
        onUnlocked()
    }

    val hours   = countdown / 3600
    val minutes = (countdown % 3600) / 60
    val seconds = countdown % 60

    Column(
        Modifier
            .fillMaxSize()
            .background(BB3Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Lock icon
        Text("🔒", fontSize = 56.sp)
        Spacer(Modifier.height(20.dp))

        Text(
            "Ngoài Giờ An Toàn",
            color      = BB3TextPrim,
            fontSize   = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Ứng dụng khóa từ ${formatHour(allowedEnd)}:00 – ${formatHour(allowedStart)}:00",
            color     = BB3TextSec,
            fontSize  = 14.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.padding(horizontal = 40.dp)
        )

        Spacer(Modifier.height(40.dp))

        // Countdown clock
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimeUnit(value = hours,   label = "GIỜ")
            TimeSeparator()
            TimeUnit(value = minutes, label = "PHÚT")
            TimeSeparator()
            TimeUnit(value = seconds, label = "GIÂY")
        }

        Spacer(Modifier.height(48.dp))

        // Emergency bypass
        Box(
            Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(BB3Card)
                .clickable(onClick = onEmergency)
                .padding(horizontal = 28.dp, vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🚨  Mở khẩn cấp", color = BB3Danger, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text("Yêu cầu PIN thật lần 2", color = BB3TextSec, fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            "Mọi lần mở khẩn cấp đều được ghi log",
            color    = BB3TextSec,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun TimeUnit(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(width = 72.dp, height = 72.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BB3Card),
            contentAlignment = Alignment.Center
        ) {
            Text(
                formatTwoDigits(value),
                color      = BB3Primary,
                fontSize   = 34.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(label, color = BB3TextSec, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TimeSeparator() {
    Text(":", color = BB3TextSec, fontSize = 30.sp, fontWeight = FontWeight.Light,
        modifier = Modifier.padding(bottom = 16.dp))
}

private fun formatHour(h: Int): String = formatTwoDigits(h)
