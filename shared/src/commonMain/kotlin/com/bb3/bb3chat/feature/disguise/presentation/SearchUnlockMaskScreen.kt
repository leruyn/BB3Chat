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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.core.disguise.DisguiseType
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import kotlinx.coroutines.delay

@Composable
fun SearchUnlockMaskScreen(
    variant: DisguiseType,
    secretCode: String,
    onSecretUnlocked: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var tapCount by remember { mutableIntStateOf(0) }
    var showFlash by remember { mutableStateOf(false) }

    fun tryUnlock(value: String) {
        if (value.trim() == secretCode) {
            showFlash = true
        }
    }

    LaunchedEffect(showFlash) {
        if (showFlash) {
            delay(120)
            onSecretUnlocked()
        }
    }

    val bg = when (variant) {
        DisguiseType.WEATHER   -> Brush.verticalGradient(listOf(Color(0xFF1E3A8A), Color(0xFF0F172A)))
        DisguiseType.MUSIC     -> Brush.verticalGradient(listOf(Color(0xFF4C0519), BB3Black))
        DisguiseType.NEWS      -> Brush.verticalGradient(listOf(Color(0xFF7F1D1D), BB3Black))
        DisguiseType.DICTIONARY-> Brush.verticalGradient(listOf(Color(0xFF14532D), BB3Black))
        DisguiseType.BANKING   -> Brush.verticalGradient(listOf(Color(0xFF064E3B), BB3Black))
        else                   -> Brush.verticalGradient(listOf(BB3Black, BB3Black))
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(bg)
            .padding(20.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Header(variant)
            Spacer(Modifier.height(20.dp))
            when (variant) {
                DisguiseType.BANKING -> BankingBody(
                    amount = query,
                    onAmountChange = {
                        query = it.filter { c -> c.isDigit() }
                        tryUnlock(query)
                    }
                )
                else -> SearchField(
                    placeholder = searchPlaceholder(variant),
                    value = query,
                    onValueChange = {
                        query = it
                        tryUnlock(it)
                    }
                )
            }
            Spacer(Modifier.height(24.dp))
            VariantContent(variant, onEasterEggTap = {
                tapCount++
                if (tapCount >= 5) showFlash = true
            })
        }
    }
}

@Composable
private fun Header(variant: DisguiseType) {
    val (title, subtitle) = when (variant) {
        DisguiseType.WEATHER    -> "Thời tiết" to "Hà Nội · Cập nhật 5 phút trước"
        DisguiseType.MUSIC      -> "Lofi Chill" to "Đang phát · BB3 Radio"
        DisguiseType.NEWS       -> "VNEXPRESS" to "Tin nóng trong ngày"
        DisguiseType.DICTIONARY -> "Từ điển Anh-Việt" to "Streak 12 ngày 🔥"
        DisguiseType.BANKING    -> "VCB Digibank" to "Xin chào, Nguyễn Văn A"
        else                    -> "" to ""
    }
    Text(title, color = BB3TextPrim, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    Text(subtitle, color = BB3TextSec, fontSize = 13.sp)
}

@Composable
private fun SearchField(placeholder: String, value: String, onValueChange: (String) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BB3Card.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = BB3TextPrim, fontSize = 16.sp),
            cursorBrush = SolidColor(BB3TextPrim),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                if (value.isEmpty()) {
                    Text(placeholder, color = BB3TextSec, fontSize = 16.sp)
                }
                inner()
            }
        )
    }
}

@Composable
private fun BankingBody(amount: String, onAmountChange: (String) -> Unit) {
    Text("Chuyển tiền", color = BB3TextSec, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BB3Card.copy(alpha = 0.55f))
            .padding(horizontal = 14.dp, vertical = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("₫", color = BB3TextSec, fontSize = 20.sp)
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                textStyle = TextStyle(color = BB3TextPrim, fontSize = 24.sp, fontWeight = FontWeight.Bold),
                cursorBrush = SolidColor(BB3TextPrim),
                singleLine = true,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    Text("Người nhận: 09xx xxx xxx", color = BB3TextSec, fontSize = 14.sp)
}

@Composable
private fun VariantContent(variant: DisguiseType, onEasterEggTap: () -> Unit) {
    when (variant) {
        DisguiseType.WEATHER -> {
            Text("28°C", color = BB3TextPrim, fontSize = 56.sp, fontWeight = FontWeight.Light)
            Text("Nhiều mây · Độ ẩm 72%", color = BB3TextSec, fontSize = 15.sp)
            Spacer(Modifier.height(16.dp))
            Box(
                Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(BB3Card.copy(alpha = 0.4f))
                    .clickable(onClick = onEasterEggTap)
                    .padding(16.dp)
            ) {
                Text("Khí áp 1012 hPa", color = BB3TextPrim, fontSize = 14.sp)
            }
        }
        DisguiseType.MUSIC -> {
            Box(
                Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF831843))
                    .clickable(onClick = onEasterEggTap),
                contentAlignment = Alignment.Center
            ) {
                Text("♫", fontSize = 48.sp, color = Color.White)
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Text("⏮", fontSize = 28.sp, color = BB3TextSec)
                Text("▶", fontSize = 36.sp, color = BB3TextPrim)
                Text("⏭", fontSize = 28.sp, color = BB3TextSec)
            }
        }
        DisguiseType.NEWS -> {
            repeat(3) { i ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        listOf(
                            "Thời tiết miền Bắc chuyển rét đậm",
                            "Giá vàng hôm nay tăng nhẹ",
                            "Công nghệ AI trong giáo dục"
                        )[i],
                        color = BB3TextPrim,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("2 giờ trước", color = BB3TextSec, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "VNEXPRESS",
                color = Color(0xFFDC2626),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable(onClick = onEasterEggTap)
            )
        }
        DisguiseType.DICTIONARY -> {
            Text("hello", color = BB3TextPrim, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text("/həˈləʊ/ · interjection", color = BB3TextSec, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            Text("xin chào", color = BB3TextPrim, fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                "🔥 Streak 12",
                color = Color(0xFFF97316),
                fontSize = 14.sp,
                modifier = Modifier.clickable(onClick = onEasterEggTap)
            )
        }
        DisguiseType.BANKING -> {
            Text("Số dư khả dụng", color = BB3TextSec, fontSize = 13.sp)
            Text("12.450.000 ₫", color = BB3TextPrim, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
        else -> Unit
    }
}

private fun searchPlaceholder(variant: DisguiseType): String = when (variant) {
    DisguiseType.WEATHER     -> "Tìm thành phố..."
    DisguiseType.MUSIC       -> "Tìm bài hát, nghệ sĩ..."
    DisguiseType.NEWS        -> "Tìm bài viết..."
    DisguiseType.DICTIONARY  -> "Nhập từ cần tra..."
    else                     -> "Tìm kiếm..."
}
