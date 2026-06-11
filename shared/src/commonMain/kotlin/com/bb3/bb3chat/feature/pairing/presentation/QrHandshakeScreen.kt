package com.bb3.bb3chat.feature.pairing.presentation

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
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Border
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3Primary
import com.bb3.bb3chat.ui.theme.BB3Surface
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
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.height(56.dp))

        // Header
        Row(
            Modifier.fillMaxWidth(),
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

            Text("Ghép Đôi", color = BB3TextPrim, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Box(Modifier.width(56.dp))  // spacer for centering
        }

        Spacer(Modifier.height(32.dp))

        // Tab: My QR  |  Scan
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BB3Card),
            horizontalArrangement = Arrangement.Center
        ) {
            TabBtn(
                label    = "Mã của tôi",
                selected = !state.scanMode,
                modifier = Modifier.weight(1f),
                onClick  = { if (state.scanMode) viewModel.handleEvent(QrHandshakeUiEvent.ToggleScanMode) }
            )
            TabBtn(
                label    = "Quét mã",
                selected = state.scanMode,
                modifier = Modifier.weight(1f),
                onClick  = { if (!state.scanMode) viewModel.handleEvent(QrHandshakeUiEvent.ToggleScanMode) }
            )
        }

        Spacer(Modifier.height(32.dp))

        if (!state.scanMode) {
            // ── My QR code panel ─────────────────────────────────────────
            MyQrPanel(
                code      = state.myRoomCode,
                onRefresh = { viewModel.handleEvent(QrHandshakeUiEvent.GenerateCode) }
            )
        } else {
            // ── Scan / Manual entry panel ─────────────────────────────────
            ScanPanel(
                manualCode    = state.scannedCode,
                isConnecting  = state.isConnecting,
                error         = state.error,
                onCodeChange  = { viewModel.handleEvent(QrHandshakeUiEvent.ManualCodeEntered(it)) },
                onConfirm     = { viewModel.handleEvent(QrHandshakeUiEvent.ConfirmConnect) }
            )
        }
    }
}

// ─── My QR Panel ─────────────────────────────────────────────────────────────

@Composable
private fun MyQrPanel(code: String, onRefresh: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // QR placeholder (real impl: platform QRCode generator → Bitmap → Image())
        Box(
            Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White)
                .border(3.dp, BB3Border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("▪▪▪▪▪▪▪", color = Color.Black, fontSize = 20.sp)
                Text("▪  BB3  ▪", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text("▪  $code  ▪", color = Color.Black, fontSize = 13.sp)
                Text("▪▪▪▪▪▪▪", color = Color.Black, fontSize = 20.sp)
            }
        }

        Spacer(Modifier.height(20.dp))

        // Code display
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            code.chunked(4).forEach { chunk ->
                Box(
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BB3Card)
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(chunk, color = BB3Primary, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        letterSpacing = 3.sp)
                }
                if (chunk.length == 4 && code.indexOf(chunk) < code.length - 4) {
                    Text("–", color = BB3TextSec, fontSize = 18.sp)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Cho đối phương quét hoặc nhập mã này", color = BB3TextSec, fontSize = 13.sp,
            textAlign = TextAlign.Center)

        Spacer(Modifier.height(20.dp))

        // Refresh code
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(BB3Card)
                .clickable(onClick = onRefresh)
                .padding(horizontal = 24.dp, vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("🔄  Tạo mã mới", color = BB3TextSec, fontSize = 14.sp)
        }

        Spacer(Modifier.height(16.dp))
        Text("Mã hết hạn sau 5 phút", color = BB3TextSec, fontSize = 11.sp)
    }
}

// ─── Scan / Manual Panel ──────────────────────────────────────────────────────

@Composable
private fun ScanPanel(
    manualCode   : String,
    isConnecting : Boolean,
    error        : String?,
    onCodeChange : (String) -> Unit,
    onConfirm    : () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Camera placeholder — real impl uses CameraX + ML Kit BarcodeScanner
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111111))
                .border(1.dp, BB3Border, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("📷", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("Camera scanner", color = BB3TextSec, fontSize = 14.sp)
                Text("(platform: CameraX + MLKit)", color = BB3TextSec, fontSize = 11.sp)
            }
            // Corner brackets
            QrViewFinder()
        }

        Spacer(Modifier.height(24.dp))

        Text("— hoặc nhập thủ công —", color = BB3TextSec, fontSize = 13.sp)
        Spacer(Modifier.height(12.dp))

        // Manual code input
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(BB3Card)
                .border(1.dp, if (error != null) BB3Danger else BB3Border, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            if (manualCode.isEmpty()) {
                Text("Nhập mã 8 ký tự của đối phương...", color = BB3TextSec, fontSize = 16.sp)
            }
            BasicTextField(
                value         = manualCode,
                onValueChange = onCodeChange,
                textStyle     = TextStyle(
                    color       = BB3TextPrim,
                    fontSize    = 18.sp,
                    fontWeight  = FontWeight.SemiBold,
                    textAlign   = TextAlign.Center,
                    letterSpacing = 4.sp
                ),
                cursorBrush   = SolidColor(BB3Primary),
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
        }

        if (error != null) {
            Spacer(Modifier.height(6.dp))
            Text(error, color = BB3Danger, fontSize = 13.sp)
        }

        Spacer(Modifier.height(24.dp))

        // Connect button
        val canConnect = manualCode.length >= 6 && !isConnecting
        Box(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (canConnect) BB3Primary else BB3Card)
                .clickable(enabled = canConnect, onClick = onConfirm),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isConnecting) "Đang kết nối..." else "Kết nối",
                color      = if (canConnect) Color.Black else BB3TextSec,
                fontSize   = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun QrViewFinder() {
    // Decorative corner brackets inside camera preview
    Box(Modifier.fillMaxSize().padding(24.dp)) {
        val c = BB3Primary
        // Top-left
        Box(Modifier.align(Alignment.TopStart).size(30.dp, 3.dp).background(c))
        Box(Modifier.align(Alignment.TopStart).size(3.dp, 30.dp).background(c))
        // Top-right
        Box(Modifier.align(Alignment.TopEnd).size(30.dp, 3.dp).background(c))
        Box(Modifier.align(Alignment.TopEnd).size(3.dp, 30.dp).background(c))
        // Bottom-left
        Box(Modifier.align(Alignment.BottomStart).size(30.dp, 3.dp).background(c))
        Box(Modifier.align(Alignment.BottomStart).size(3.dp, 30.dp).background(c))
        // Bottom-right
        Box(Modifier.align(Alignment.BottomEnd).size(30.dp, 3.dp).background(c))
        Box(Modifier.align(Alignment.BottomEnd).size(3.dp, 30.dp).background(c))
    }
}

@Composable
private fun TabBtn(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) BB3Primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color      = if (selected) Color.Black else BB3TextSec,
            fontSize   = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
