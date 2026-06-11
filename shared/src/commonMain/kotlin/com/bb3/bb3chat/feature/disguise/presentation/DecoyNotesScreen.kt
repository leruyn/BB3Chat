package com.bb3.bb3chat.feature.disguise.presentation

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Border
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec

private data class DecoyNote(
    val emoji    : String,
    val title    : String,
    val preview  : String,
    val time     : String
)

private val DECOY_NOTES = listOf(
    DecoyNote("💼", "Hội thảo Dự án 2026", "Đã đọc tất cả tài liệu lưu trữ", "11/04"),
    DecoyNote("🚚", "Giao hàng Nhanh HN", "Đã hoàn tất đơn hàng số 412A", "08/10"),
    DecoyNote("📋", "Checklist tuần 24", "Gửi báo cáo tiến độ trước 17h thứ Sáu", "Hôm qua"),
    DecoyNote("☕", "Lịch họp team", "Review sprint — phòng 3B lúc 9:30", "Thứ 3")
)

@Composable
fun DecoyNotesScreen(
    onNavigateToCalculator: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Color(0xFFF2F2F7))
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFFFFD60A))
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ghi chú", color = Color.Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Box(
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(BB3Danger)
                    .clickable(onClick = onNavigateToCalculator),
                contentAlignment = Alignment.Center
            ) {
                Text("!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
            }
        }

        LazyColumn(Modifier.fillMaxSize()) {
            items(DECOY_NOTES) { note ->
                DecoyNoteRow(note)
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 72.dp)
                        .height(0.5.dp)
                        .background(BB3Border.copy(alpha = 0.3f))
                )
            }
        }
    }
}

@Composable
private fun DecoyNoteRow(note: DecoyNote) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BB3Card),
            contentAlignment = Alignment.Center
        ) {
            Text(note.emoji, fontSize = 24.sp)
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    note.title,
                    color = BB3TextPrim,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(note.time, color = BB3TextSec, fontSize = 13.sp)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                note.preview,
                color = BB3TextSec,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
