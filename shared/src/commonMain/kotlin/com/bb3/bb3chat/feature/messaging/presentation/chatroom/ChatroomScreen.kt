package com.bb3.bb3chat.feature.messaging.presentation.chatroom

import com.bb3.bb3chat.core.util.formatHourMinute
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.core.platform.rememberImagePicker
import com.bb3.bb3chat.feature.messaging.presentation.selfdestruct.SelfDestructOverlay
import com.bb3.bb3chat.ui.components.ChatImageContent
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

@Composable
fun ChatroomScreen(
    viewModel           : ChatroomViewModel,
    onNavigateBack      : () -> Unit,
    onNavigateToCalculator: () -> Unit
) {
    val state      by viewModel.state.collectAsState()
    val listState   = rememberLazyListState()
    val pickImage   = rememberImagePicker { bytes ->
        viewModel.handleEvent(ChatroomUiEvent.AttachImage(bytes))
    }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is ChatroomUiEffect.NavigateBack         -> onNavigateBack()
                is ChatroomUiEffect.NavigateToCalculator -> onNavigateToCalculator()
                is ChatroomUiEffect.RoomDestroyed        -> onNavigateBack()
                else -> {}
            }
        }
    }

    // Auto-scroll to last message
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BB3Black)
            .imePadding()
    ) {
        Column(Modifier.fillMaxSize()) {

            // ── Top bar ────────────────────────────────────────────────────
            ChatroomTopBar(
                alias       = state.roomAlias,
                onBack      = { viewModel.handleEvent(ChatroomUiEvent.NavigateBack) },
                onPanic     = { viewModel.handleEvent(ChatroomUiEvent.TriggerPanic) }
            )

            Box(Modifier.fillMaxWidth().height(0.5.dp).background(BB3Border))

            // ── Message list ────────────────────────────────────────────────
            LazyColumn(
                state           = listState,
                modifier        = Modifier.weight(1f),
                contentPadding  = PaddingValues(vertical = 12.dp, horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(state.messages, key = { it.id }) { msg ->
                    MessageBubble(
                        msg         = msg,
                        onLongPress = { viewModel.handleEvent(ChatroomUiEvent.MessageLongPress(msg.id)) }
                    )
                }
            }

            // ── Attach preview bar ────────────────────────────────────────
            AnimatedVisibility(
                visible = state.attachPreview != null,
                enter   = slideInVertically { it } + fadeIn(),
                exit    = slideOutVertically { it } + fadeOut()
            ) {
                state.attachPreview?.let { preview ->
                    AttachPreviewBar(
                        preview   = preview,
                        onSend    = { viewModel.handleEvent(ChatroomUiEvent.SendImage) },
                        onCancel  = { viewModel.handleEvent(ChatroomUiEvent.CancelAttach) }
                    )
                }
            }

            // ── Input bar ──────────────────────────────────────────────────
            if (state.attachPreview == null) {
                InputBar(
                    text      = state.inputText,
                    isSending = state.isSending,
                    onTextChange = { viewModel.handleEvent(ChatroomUiEvent.InputChanged(it)) },
                    onSend    = { viewModel.handleEvent(ChatroomUiEvent.SendText) },
                    onAttach  = pickImage
                )
            }
        }

        if (state.isDestroyed) {
            DestroyedOverlay(onClose = onNavigateBack)
        }

        state.selfDestructSecs?.let { secs ->
            SelfDestructOverlay(
                totalSecs = secs,
                onExpired = { viewModel.handleEvent(ChatroomUiEvent.SelfDestructExpired) },
                onDismiss = { viewModel.handleEvent(ChatroomUiEvent.DismissSelfDestruct) }
            )
        }
    }
}

// ─────────────────────────── Top Bar ────────────────────────────────────────

@Composable
private fun ChatroomTopBar(
    alias  : String,
    onBack : () -> Unit,
    onPanic: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back chevron
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(BB3Card)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Text("‹", color = BB3Primary, fontSize = 24.sp, fontWeight = FontWeight.Light)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(alias, color = BB3TextPrim, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("Mã hóa đầu-cuối", color = BB3TextSec, fontSize = 12.sp)
        }

        // Panic
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BB3Danger)
                .clickable(onClick = onPanic),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ─────────────────────────── Message Bubble ─────────────────────────────────

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun MessageBubble(msg: UiMessage, onLongPress: () -> Unit) {
    val isMe   = msg.isMine
    val align  = if (isMe) Alignment.End else Alignment.Start
    val bgCol  = if (isMe) BB3Primary else BB3Card
    val txtCol = if (isMe) Color.Black  else BB3TextPrim
    val shape  = if (isMe)
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    else
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)

    when (val content = msg.content) {
        is UiMessageContent.SystemEvent -> {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    content.text,
                    color     = BB3TextSec,
                    fontSize  = 12.sp,
                    textAlign = TextAlign.Center,
                    modifier  = Modifier.padding(vertical = 4.dp)
                )
            }
            return
        }
        else -> {}
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = align
    ) {
        Box(
            Modifier
                .widthIn(max = 280.dp)
                .clip(shape)
                .background(bgCol)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (val content = msg.content) {
                is UiMessageContent.Text -> {
                    Text(content.text, color = txtCol, fontSize = 16.sp)
                }
                is UiMessageContent.Image -> {
                    ChatImageContent(
                        bytes    = content.decryptedBytes,
                        modifier = Modifier
                            .size(200.dp, 150.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BB3Surface)
                    )
                }
                is UiMessageContent.Voice -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🎙", fontSize = 18.sp)
                        Spacer(Modifier.width(6.dp))
                        Text("${content.durationSecs}s", color = txtCol, fontSize = 14.sp)
                    }
                }
                else -> {}
            }
        }

        // Timestamp + read receipt
        Row(
            Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                formatTimestamp(msg.timestampMs),
                color    = BB3TextSec,
                fontSize = 11.sp
            )
            if (isMe) {
                Text(
                    if (msg.readByPeer) "✓✓" else "✓",
                    color    = if (msg.readByPeer) BB3Primary else BB3TextSec,
                    fontSize = 11.sp
                )
            }
            if (msg.selfDestruct) {
                Text("💣", fontSize = 10.sp)
            }
        }
    }
}

// ─────────────────────────── Input Bar ──────────────────────────────────────

@Composable
private fun InputBar(
    text        : String,
    isSending   : Boolean,
    onTextChange: (String) -> Unit,
    onSend      : () -> Unit,
    onAttach    : () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BB3Surface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // Attach button
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(BB3Card)
                .clickable(onClick = onAttach),
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = BB3TextPrim, fontSize = 22.sp, fontWeight = FontWeight.Light)
        }

        Spacer(Modifier.width(8.dp))

        // Text field
        Box(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(20.dp))
                .background(BB3Card)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            if (text.isEmpty()) {
                Text("Nhắn tin...", color = BB3TextSec, fontSize = 16.sp)
            }
            BasicTextField(
                value         = text,
                onValueChange = onTextChange,
                textStyle     = TextStyle(color = BB3TextPrim, fontSize = 16.sp),
                cursorBrush   = SolidColor(BB3Primary),
                maxLines      = 5,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.width(8.dp))

        // Send button
        val canSend = text.trim().isNotEmpty() && !isSending
        Box(
            Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(if (canSend) BB3Primary else BB3Card)
                .clickable(enabled = canSend, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Text("↑", color = if (canSend) Color.Black else BB3TextSec, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────── Attach Preview Bar ─────────────────────────────

@Composable
private fun AttachPreviewBar(
    preview  : AttachPreview,
    onSend   : () -> Unit,
    onCancel : () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BB3Surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(BB3Card),
            contentAlignment = Alignment.Center
        ) {
            Text("🖼", fontSize = 28.sp)
        }

        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text("Ảnh đã chọn", color = BB3TextPrim, fontSize = 15.sp)
            Text("${preview.sizeBytes / 1024} KB", color = BB3TextSec, fontSize = 12.sp)
        }

        // Cancel
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BB3Card)
                .clickable(onClick = onCancel),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", color = BB3Danger, fontSize = 16.sp)
        }

        Spacer(Modifier.width(8.dp))

        // Send
        Box(
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(BB3Primary)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Text("↑", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ─────────────────────────── Destroyed Overlay ──────────────────────────────

@Composable
private fun DestroyedOverlay(onClose: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xCC000000)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(BB3Card)
                .padding(32.dp)
        ) {
            Text("💥", fontSize = 52.sp)
            Text("Phòng đã bị huỷ", color = BB3TextPrim, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("Tất cả dữ liệu đã được xoá", color = BB3TextSec, fontSize = 14.sp)
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BB3Danger)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 32.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Đóng", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─────────────────────────── Helpers ────────────────────────────────────────

private fun formatTimestamp(ms: Long): String {
    if (ms == 0L) return ""
    return try {
        val dt = Instant.fromEpochMilliseconds(ms)
            .toLocalDateTime(TimeZone.currentSystemDefault())
        formatHourMinute(dt.hour, dt.minute)
    } catch (e: Exception) { "" }
}
