package com.bb3.bb3chat.feature.messaging.presentation.inbox

import com.bb3.bb3chat.core.util.formatHourMinute
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.feature.security.presentation.IntruderAlertBanner
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Border
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3Primary
import com.bb3.bb3chat.ui.theme.BB3Surface
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.koinInject

private val AVATAR_COLORS = listOf(
    Color(0xFF5E5CE6), Color(0xFF0A84FF), Color(0xFF30D158),
    Color(0xFFFF9F0A), Color(0xFFFF453A), Color(0xFFFF375F),
    Color(0xFF64D2FF), Color(0xFFFFD60A)
)

@Composable
fun InboxScreen(
    viewModel: InboxViewModel = koinInject(),
    onOpenRoom: (String) -> Unit,
    onOpenPairing: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onOpenStore: () -> Unit = {},
    onOpenIntruderGallery: () -> Unit = {},
    onNavigateToCalculator: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is InboxUiEffect.NavigateToChatroom   -> onOpenRoom(effect.roomId)
                is InboxUiEffect.NavigateToPairing    -> onOpenPairing()
                is InboxUiEffect.NavigateToSettings   -> onOpenSettings()
                is InboxUiEffect.NavigateToStore      -> onOpenStore()
                is InboxUiEffect.NavigateToCalculator    -> onNavigateToCalculator()
                is InboxUiEffect.NavigateToIntruderGallery -> onOpenIntruderGallery()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BB3Black)
    ) {
        // TopBar
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Hộp Thư Ẩn", color = BB3TextPrim, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircleIconBtn(label = "+") { viewModel.handleEvent(InboxUiEvent.OpenPairing) }
                CircleIconBtn(label = if (state.isSyncing) "↻" else "⟳") {
                    viewModel.handleEvent(InboxUiEvent.Refresh)
                }
                CircleIconBtn(label = "⚙️") { viewModel.handleEvent(InboxUiEvent.OpenSettings) }
                // Nút Store
                CircleIconBtn(label = "👑") { viewModel.handleEvent(InboxUiEvent.OpenStore) }
                // PANIC button
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(BB3Danger)
                        .clickable { viewModel.handleEvent(InboxUiEvent.TriggerPanic) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        IntruderAlertBanner(
            snapshotCount = state.intruderCount,
            visible       = state.showIntruderBanner,
            onView        = { viewModel.handleEvent(InboxUiEvent.ViewIntruderGallery) },
            onDismiss     = { viewModel.handleEvent(InboxUiEvent.DismissIntruderBanner) }
        )

        Box(Modifier.fillMaxWidth().height(0.5.dp).background(BB3Border))

        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Đang tải...", color = BB3TextSec)
            }
        } else if (state.rooms.isEmpty()) {
            EmptyInbox(onPairClick = { viewModel.handleEvent(InboxUiEvent.OpenPairing) })
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(state.rooms, key = { it.id }) { room ->
                    RoomRow(
                        room    = room,
                        onClick = { viewModel.handleEvent(InboxUiEvent.OpenRoom(room.id)) }
                    )
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 80.dp)
                            .height(0.5.dp)
                            .background(BB3Border)
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomRow(room: RoomItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar
        Box(
            Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(AVATAR_COLORS[room.avatarIndex % AVATAR_COLORS.size]),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text       = room.alias.take(1).uppercase(),
                color      = Color.White,
                fontSize   = 22.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text       = room.alias,
                        color      = BB3TextPrim,
                        fontSize   = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 1
                    )
                    if (room.isPinned) {
                        Spacer(Modifier.width(4.dp))
                        Text("📌", fontSize = 12.sp)
                    }
                    if (room.isMuted) {
                        Spacer(Modifier.width(4.dp))
                        Text("🔕", fontSize = 11.sp)
                    }
                }
                Text(
                    text     = formatTimestamp(room.lastTimestamp),
                    color    = if (room.unreadCount > 0) BB3Primary else BB3TextSec,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(3.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text      = room.lastSnippet.ifEmpty { "Chưa có tin nhắn" },
                    color     = BB3TextSec,
                    fontSize  = 15.sp,
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.weight(1f)
                )
                if (room.unreadCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .clip(CircleShape)
                            .background(BB3Primary)
                            .padding(horizontal = 7.dp, vertical = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text      = if (room.unreadCount > 99) "99+" else room.unreadCount.toString(),
                            color     = Color.White,
                            fontSize  = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyInbox(onPairClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("👻", fontSize = 52.sp)
        Spacer(Modifier.height(16.dp))
        Text("Chưa có phòng chat", color = BB3TextPrim, fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        Text("Ghép đôi qua Mã Phòng hoặc QR", color = BB3TextSec, fontSize = 14.sp)
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BB3Primary)
                .clickable { onPairClick() }
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text("Ghép phòng", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CircleIconBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(BB3Card)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 16.sp)
    }
}

private fun formatTimestamp(ms: Long): String {
    if (ms == 0L) return ""
    return try {
        val dt = Instant.fromEpochMilliseconds(ms)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        formatHourMinute(dt.hour, dt.minute)
    } catch (e: Exception) { "" }
}
