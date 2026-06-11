package com.bb3.bb3chat.feature.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Border
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3Primary
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import org.koin.compose.koinInject

private val AVATAR_COLORS = listOf(
    Color(0xFF5E5CE6), Color(0xFF0A84FF), Color(0xFF30D158), Color(0xFFFF9F0A),
    Color(0xFFFF453A), Color(0xFFFF375F), Color(0xFF64D2FF), Color(0xFFFFD60A)
)

private val SUGGESTED_ALIASES = listOf("Sếp Tổng", "Khách VIP", "Dịch Vụ Nhà", "Đối Tác")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileSetupScreen(
    viewModel: ProfileSetupViewModel = koinInject(),
    onComplete: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ProfileSetupUiEffect.Completed -> onComplete()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BB3Black)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(32.dp))
        Text("Định danh ẩn danh", color = BB3TextPrim, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Chọn biệt danh và avatar trung tính — không dùng ảnh thật.", color = BB3TextSec, fontSize = 14.sp)

        Spacer(Modifier.height(28.dp))
        Text("BIỆT DANH", color = BB3Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BB3Card)
                .border(1.dp, BB3Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value         = state.alias,
                onValueChange = { viewModel.handleEvent(ProfileSetupUiEvent.AliasChanged(it)) },
                textStyle     = TextStyle(color = BB3TextPrim, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush   = SolidColor(BB3Primary),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SUGGESTED_ALIASES.forEach { suggestion ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BB3Card)
                        .clickable { viewModel.handleEvent(ProfileSetupUiEvent.AliasChanged(suggestion)) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(suggestion, color = BB3TextSec, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text("AVATAR", color = BB3Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AVATAR_COLORS.indices.forEach { index ->
                val selected = index == state.avatarIndex
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(AVATAR_COLORS[index])
                        .border(
                            width = if (selected) 3.dp else 0.dp,
                            color = BB3Primary,
                            shape = CircleShape
                        )
                        .clickable { viewModel.handleEvent(ProfileSetupUiEvent.AvatarSelected(index)) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        state.alias.take(1).uppercase().ifEmpty { "?" },
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        if (state.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(state.error!!, color = BB3Danger, fontSize = 13.sp)
        }

        Spacer(Modifier.weight(1f))

        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(BB3Primary)
                .clickable(enabled = !state.isSaving) {
                    viewModel.handleEvent(ProfileSetupUiEvent.Save)
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (state.isSaving) "Đang lưu..." else "Lưu tài khoản",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
