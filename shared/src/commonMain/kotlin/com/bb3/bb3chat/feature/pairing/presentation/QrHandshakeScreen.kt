package com.bb3.bb3chat.feature.pairing.presentation

import com.bb3.bb3chat.core.util.formatTwoDigits
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
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
import com.bb3.bb3chat.core.platform.QrScannerView
import com.bb3.bb3chat.ui.components.ChatImageContent
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Border
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3Primary
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import org.koin.compose.koinInject

@Composable
fun QrHandshakeScreen(
    viewModel    : QrHandshakeViewModel = koinInject(),
    onRoomCreated: (String) -> Unit,
    onDismiss    : () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scroll  = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is QrHandshakeUiEffect.RoomCreated -> onRoomCreated(effect.roomId)
                is QrHandshakeUiEffect.Dismissed   -> onDismiss()
                else                               -> {}
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BB3Black)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(BB3Card)
                    .clickable { viewModel.handleEvent(QrHandshakeUiEvent.Dismiss) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) { Text("Huỷ", color = BB3Danger, fontSize = 15.sp) }
            Text("Trung Tâm Kết Nối", color = BB3TextPrim, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.width(56.dp))
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BB3Card),
        ) {
            HubTabBtn("Mã phòng", state.hubTab == HubTab.ROOM_CODE, Modifier.weight(1f)) {
                viewModel.handleEvent(QrHandshakeUiEvent.SelectTab(HubTab.ROOM_CODE))
            }
            HubTabBtn("QR", state.hubTab == HubTab.QR, Modifier.weight(1f)) {
                viewModel.handleEvent(QrHandshakeUiEvent.SelectTab(HubTab.QR))
            }
            HubTabBtn("ID của tôi", state.hubTab == HubTab.MY_ID, Modifier.weight(1f)) {
                viewModel.handleEvent(QrHandshakeUiEvent.SelectTab(HubTab.MY_ID))
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(24.dp))
            when (state.hubTab) {
                HubTab.ROOM_CODE -> RoomCodePanel(
                    phrase      = state.roomPhrase,
                    isMatching  = state.isRoomMatching,
                    secondsLeft = state.roomMatchSecondsLeft,
                    isConnecting = state.isConnecting,
                    error       = state.error,
                    onPhraseChange = { viewModel.handleEvent(QrHandshakeUiEvent.RoomPhraseChanged(it)) },
                    onRandom    = { viewModel.handleEvent(QrHandshakeUiEvent.GenerateRoomPhrase) },
                    onStart     = { viewModel.handleEvent(QrHandshakeUiEvent.StartRoomCodeMatch) },
                    onCancel    = { viewModel.handleEvent(QrHandshakeUiEvent.CancelRoomCodeMatch) }
                )
                HubTab.QR -> QrHubPanel(state, viewModel)
                HubTab.MY_ID -> MyIdPanel(alias = state.userAlias, deviceCode = state.myRoomCode)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun QrHubPanel(state: QrHandshakeUiState, viewModel: QrHandshakeViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BB3Card),
    ) {
        TabBtn("Mã của tôi", !state.scanMode, Modifier.weight(1f)) {
            if (state.scanMode) viewModel.handleEvent(QrHandshakeUiEvent.ToggleScanMode)
        }
        TabBtn("Quét mã", state.scanMode, Modifier.weight(1f)) {
            if (!state.scanMode) viewModel.handleEvent(QrHandshakeUiEvent.ToggleScanMode)
        }
    }
    Spacer(Modifier.height(24.dp))
    if (!state.scanMode) {
        MyQrPanel(
            code = state.myRoomCode,
            qrImageBytes = state.qrImageBytes,
            isExpired = state.isExpired,
            isWaitingForPeer = state.isWaitingForPeer,
            isConnecting = state.isConnecting,
            error = state.error,
            onRefresh = { viewModel.handleEvent(QrHandshakeUiEvent.GenerateCode) }
        )
    } else {
        ScanPanel(
            manualCode = state.scannedCode,
            isConnecting = state.isConnecting,
            error = state.error,
            onCodeChange = { viewModel.handleEvent(QrHandshakeUiEvent.ManualCodeEntered(it)) },
            onConfirm = { viewModel.handleEvent(QrHandshakeUiEvent.ConfirmConnect) },
            onCodeScanned = { viewModel.handleEvent(QrHandshakeUiEvent.OnCodeScanned(it)) }
        )
    }
}

@Composable
private fun RoomCodePanel(
    phrase: String,
    isMatching: Boolean,
    secondsLeft: Int,
    isConnecting: Boolean,
    error: String?,
    onPhraseChange: (String) -> Unit,
    onRandom: () -> Unit,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            "Hai thiết bị nhập cùng một mã phòng để ghép đôi tự động.",
            color = BB3TextSec, fontSize = 13.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Mật mã phòng", color = BB3TextSec, fontSize = 12.sp)
            Text("🎲 Sinh ngẫu nhiên", color = BB3Primary, fontSize = 12.sp,
                modifier = Modifier.clickable(onClick = onRandom))
        }
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BB3Card)
                .border(1.dp, if (error != null) BB3Danger else BB3Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            BasicTextField(
                value = phrase,
                onValueChange = onPhraseChange,
                enabled = !isMatching,
                textStyle = TextStyle(
                    color = BB3Primary, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, letterSpacing = 2.sp
                ),
                cursorBrush = SolidColor(BB3Primary),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (isMatching) {
            Spacer(Modifier.height(16.dp))
            val min = secondsLeft / 60
            val sec = secondsLeft % 60
            Text(
                "Đang chờ đối tác... ${formatTwoDigits(min)}:${formatTwoDigits(sec)}",
                color = BB3Primary, fontSize = 14.sp
            )
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = BB3Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(24.dp))
        if (isMatching) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(BB3Card)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Text("Huỷ chờ", color = BB3TextSec, fontSize = 16.sp)
            }
        } else {
            val canStart = phrase.trim().length >= 4 && !isConnecting
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canStart) BB3Primary else BB3Card)
                    .clickable(enabled = canStart, onClick = onStart),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    if (isConnecting) "Đang kết nối..." else "Bắt đầu đợi (5 phút)",
                    color = if (canStart) Color.Black else BB3TextSec,
                    fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun MyIdPanel(alias: String, deviceCode: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(88.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(BB3Card),
            contentAlignment = Alignment.Center
        ) {
            Text(alias.take(2).uppercase().ifEmpty { "?" }, color = BB3Primary, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(20.dp))
        Text("BIỆT DANH", color = BB3TextSec, fontSize = 11.sp)
        Text(alias.ifEmpty { "Chưa đặt" }, color = BB3TextPrim, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Text("MÃ THIẾT BỊ (QR)", color = BB3TextSec, fontSize = 11.sp)
        Text(deviceCode.ifEmpty { "—" }, color = BB3Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Chia sẻ biệt danh hoặc quét QR để ghép phòng. Mã thiết bị đổi mỗi phiên QR.",
            color = BB3TextSec, fontSize = 13.sp, textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun MyQrPanel(
    code: String,
    qrImageBytes: ByteArray?,
    isExpired: Boolean,
    isWaitingForPeer: Boolean,
    isConnecting: Boolean,
    error: String?,
    onRefresh: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(3.dp, BB3Border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (qrImageBytes != null) {
                ChatImageContent(bytes = qrImageBytes, modifier = Modifier.fillMaxSize().padding(12.dp))
            } else {
                Text("Đang tạo mã...", color = Color.Black, fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            code.chunked(4).forEach { chunk ->
                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(BB3Card).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Text(chunk, color = BB3Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Cho đối phương quét hoặc nhập mã này", color = BB3TextSec, fontSize = 13.sp, textAlign = TextAlign.Center)
        if (isConnecting) {
            Spacer(Modifier.height(12.dp))
            Text("Đang kết nối...", color = BB3Primary, fontSize = 13.sp)
        } else if (isWaitingForPeer) {
            Spacer(Modifier.height(12.dp))
            Text("Đang chờ đối phương quét mã...", color = BB3Primary, fontSize = 13.sp)
        }
        if (error != null) {
            Spacer(Modifier.height(12.dp))
            Text(error, color = BB3Danger, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(20.dp))
        Box(
            Modifier.clip(RoundedCornerShape(12.dp)).background(BB3Card).clickable(onClick = onRefresh).padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) { Text("🔄  Tạo mã mới", color = BB3TextSec, fontSize = 14.sp) }
        Spacer(Modifier.height(16.dp))
        Text(
            if (isExpired) "Mã đã hết hạn — tạo mã mới" else "Mã hết hạn sau 5 phút",
            color = if (isExpired) BB3Danger else BB3TextSec, fontSize = 11.sp
        )
    }
}

@Composable
private fun ScanPanel(
    manualCode: String,
    isConnecting: Boolean,
    error: String?,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCodeScanned: (String) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.fillMaxWidth().height(240.dp).clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111)).border(1.dp, BB3Border, RoundedCornerShape(16.dp))
        ) {
            QrScannerView(isActive = true, onCodeDetected = onCodeScanned, modifier = Modifier.fillMaxSize())
            QrViewFinder()
        }
        Spacer(Modifier.height(24.dp))
        Text("— hoặc nhập thủ công —", color = BB3TextSec, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(BB3Card)
                .border(1.dp, if (error != null) BB3Danger else BB3Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (manualCode.isEmpty()) {
                Text("Nhập mã 8 ký tự của đối phương...", color = BB3TextSec, fontSize = 16.sp)
            }
            BasicTextField(
                value = manualCode, onValueChange = onCodeChange,
                textStyle = TextStyle(color = BB3TextPrim, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, letterSpacing = 4.sp),
                cursorBrush = SolidColor(BB3Primary), singleLine = true, modifier = Modifier.fillMaxWidth()
            )
        }
        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error, color = BB3Danger, fontSize = 13.sp)
        }
        Spacer(Modifier.height(24.dp))
        val canConnect = manualCode.length >= 6 && !isConnecting
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(14.dp))
                .background(if (canConnect) BB3Primary else BB3Card)
                .clickable(enabled = canConnect, onClick = onConfirm),
            contentAlignment = Alignment.Center
        ) {
            Text(if (isConnecting) "Đang kết nối..." else "Kết nối", color = if (canConnect) Color.Black else BB3TextSec, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QrViewFinder() {
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        val c = BB3Primary
        Box(Modifier.align(Alignment.TopStart).size(30.dp, 3.dp).background(c))
        Box(Modifier.align(Alignment.TopStart).size(3.dp, 30.dp).background(c))
        Box(Modifier.align(Alignment.TopEnd).size(30.dp, 3.dp).background(c))
        Box(Modifier.align(Alignment.TopEnd).size(3.dp, 30.dp).background(c))
        Box(Modifier.align(Alignment.BottomStart).size(30.dp, 3.dp).background(c))
        Box(Modifier.align(Alignment.BottomStart).size(3.dp, 30.dp).background(c))
        Box(Modifier.align(Alignment.BottomEnd).size(30.dp, 3.dp).background(c))
        Box(Modifier.align(Alignment.BottomEnd).size(3.dp, 30.dp).background(c))
    }
}

@Composable
private fun HubTabBtn(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) BB3Primary else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.Black else BB3TextSec, fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, textAlign = TextAlign.Center)
    }
}

@Composable
private fun TabBtn(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(12.dp)).background(if (selected) BB3Primary else Color.Transparent)
            .clickable(onClick = onClick).padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) Color.Black else BB3TextSec, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
