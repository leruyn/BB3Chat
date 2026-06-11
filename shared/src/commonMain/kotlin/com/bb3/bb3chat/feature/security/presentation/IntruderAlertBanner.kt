package com.bb3.bb3chat.feature.security.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3TextPrim

/**
 * Banner hiện ở đầu Inbox sau khi PIN thật mở thành công
 * nếu có intruder snapshots chưa được xem.
 *
 * @param snapshotCount  số lần thâm nhập bị bắt
 * @param visible        hiện/ẩn
 * @param onView         mở màn hình xem ảnh intruder
 * @param onDismiss      đóng banner (mark as seen)
 */
@Composable
fun IntruderAlertBanner(
    snapshotCount : Int,
    visible       : Boolean,
    onView        : () -> Unit,
    onDismiss     : () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter   = expandVertically(),
        exit    = shrinkVertically()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .background(Color(0xFF3D0F0C))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("🚨", fontSize = 18.sp)
                Text(
                    "$snapshotCount lần thâm nhập bị chụp",
                    color      = BB3TextPrim,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Xem",
                    color     = BB3Danger,
                    fontSize  = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier  = Modifier.clickable(onClick = onView)
                )
                Text(
                    "✕",
                    color    = Color(0xFF8E8E93),
                    fontSize = 16.sp,
                    modifier = Modifier.clickable(onClick = onDismiss)
                )
            }
        }
    }
}
