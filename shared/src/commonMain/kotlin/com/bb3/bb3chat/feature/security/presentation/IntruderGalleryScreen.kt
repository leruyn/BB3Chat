package com.bb3.bb3chat.feature.security.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.feature.security.domain.usecase.CaptureIntruderUseCase
import com.bb3.bb3chat.ui.components.ChatImageContent
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import org.koin.compose.koinInject

@Composable
fun IntruderGalleryScreen(
    captureUseCase: CaptureIntruderUseCase = koinInject(),
    onDismiss: () -> Unit
) {
    val snapshots = remember { captureUseCase.getDecryptedSnapshots() }

    Column(
        Modifier
            .fillMaxSize()
            .background(BB3Black)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BB3Card)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("← Quay lại", color = BB3TextSec, fontSize = 15.sp)
            }
            Text("Ảnh xâm nhập", color = BB3TextPrim, fontSize = 18.sp)
            Box(Modifier.size(48.dp))
        }

        Spacer(Modifier.height(24.dp))

        if (snapshots.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Chưa có ảnh nào được lưu.", color = BB3TextSec, fontSize = 15.sp)
            }
        } else {
            Text(
                "${snapshots.size} lần thử PIN sai đã được ghi lại",
                color = BB3Danger,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                itemsIndexed(snapshots) { index, bytes ->
                    Column {
                        Text(
                            "Lần ${index + 1}",
                            color = BB3TextSec,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(8.dp))
                        ChatImageContent(
                            bytes    = bytes,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BB3Card)
                        )
                    }
                }
            }
        }
    }
}
