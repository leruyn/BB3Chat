package com.bb3.bb3chat.feature.onboarding.presentation

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Primary
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec

private data class OnboardingStep(val emoji: String, val title: String, val body: String)

private val STEPS = listOf(
    OnboardingStep("🧮", "Vỏ bọc ngụy trang", "BB3Chat ẩn sau ứng dụng Máy tính. Nhập mã bí mật + = để mở cổng PIN."),
    OnboardingStep("🔑", "PIN kép", "PIN thật → Hộp thư mật. PIN giả → Sổ ghi chú công việc bình thường."),
    OnboardingStep("🔗", "Ghép đôi an toàn", "Dùng mã phòng chung, QR hoặc quét camera — khóa E2EE chỉ có trên thiết bị."),
    OnboardingStep("🚨", "Panic & tự huỷ", "Nút ! quay về vỏ bọc ngay. Tin 💣 tự xoá sau vài giây khi đọc.")
)

@Composable
fun OnboardingScreen(onComplete: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }
    val current = STEPS[step]

    Column(
        Modifier
            .fillMaxSize()
            .background(BB3Black)
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(48.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            STEPS.indices.forEach { i ->
                Box(
                    Modifier
                        .height(4.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) BB3Primary else BB3Card)
                )
            }
        }
        Spacer(Modifier.height(48.dp))
        Text(current.emoji, fontSize = 56.sp)
        Spacer(Modifier.height(24.dp))
        Text(current.title, color = BB3TextPrim, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Text(
            current.body,
            color = BB3TextSec,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
        Spacer(Modifier.weight(1f))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (step > 0) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(BB3Card)
                        .clickable { step -= 1 }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("← Quay lại", color = BB3TextSec, fontSize = 16.sp)
                }
            }
            Box(
                Modifier
                    .weight(if (step > 0) 1f else 1f)
                    .fillMaxWidth(if (step == 0) 1f else 0.5f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BB3Primary)
                    .clickable {
                        if (step < STEPS.lastIndex) step += 1 else onComplete()
                    }
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (step < STEPS.lastIndex) "Tiếp theo →" else "Bắt đầu",
                    color = Color.Black,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}
