package com.bb3.bb3chat.feature.settings.presentation

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import com.bb3.bb3chat.core.disguise.DisguiseType
import com.bb3.bb3chat.core.util.formatTwoDigits
import com.bb3.bb3chat.ui.theme.BB3Black
import com.bb3.bb3chat.ui.theme.BB3Border
import com.bb3.bb3chat.ui.theme.BB3Card
import com.bb3.bb3chat.ui.theme.BB3Danger
import com.bb3.bb3chat.ui.theme.BB3Primary
import com.bb3.bb3chat.ui.theme.BB3TextPrim
import com.bb3.bb3chat.ui.theme.BB3TextSec
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel : SettingsViewModel = koinInject(),
    onDismiss : () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val scroll  = rememberScrollState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.Dismissed -> onDismiss()
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
                    .clickable { viewModel.handleEvent(SettingsUiEvent.Dismiss) }
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text("← Quay lại", color = BB3TextSec, fontSize = 15.sp)
            }
            Text("Cấu hình", color = BB3TextPrim, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Box(Modifier.padding(horizontal = 14.dp))
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(horizontal = 24.dp)
        ) {
            SectionTitle("Vỏ bọc ứng dụng")
            if (!state.hasPremiumDisguises) {
                Text(
                    "Nâng cấp Gold để đổi vỏ Thời tiết, Nhạc, Tin tức...",
                    color = BB3TextSec,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
            }
            DisguiseTypeGrid(
                selected = state.disguiseType,
                lockedTypes = if (state.hasPremiumDisguises) emptySet() else
                    DisguiseType.entries.filter { it != DisguiseType.CALCULATOR }.toSet(),
                onSelect = { viewModel.handleEvent(SettingsUiEvent.DisguiseTypeSelected(it)) }
            )
            Spacer(Modifier.height(8.dp))
            SectionHint(disguiseHint(state.disguiseType, state.secretCode))

            Spacer(Modifier.height(16.dp))
            SectionTitle("Mã kích hoạt vỏ bọc")
            TextFieldBox(
                value         = state.secretCode,
                onValueChange = { viewModel.handleEvent(SettingsUiEvent.SecretCodeChanged(it)) },
                hasError      = state.error != null
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("PIN giả (bẫy)")
            SectionHint("Để trống nếu không đổi. PIN giả mở Sổ Ghi Chú giả lập.")
            TextFieldBox(
                value         = state.decoyPin,
                onValueChange = { viewModel.handleEvent(SettingsUiEvent.DecoyPinChanged(it)) },
                hasError      = state.error != null
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Giờ an toàn (Safe Hours)")
            if (!state.hasSafeHoursVip) {
                Text(
                    "Nâng cấp Silver trở lên để bật khóa theo khung giờ.",
                    color = BB3TextSec,
                    fontSize = 13.sp
                )
            } else {
                ToggleRow(
                    label   = "Bật khóa ngoài giờ",
                    checked = state.safeHoursEnabled,
                    onToggle = { viewModel.handleEvent(SettingsUiEvent.SafeHoursToggled(it)) }
                )
                if (state.safeHoursEnabled) {
                    Spacer(Modifier.height(12.dp))
                    HourStepper(
                        label = "Mở từ",
                        hour  = state.safeHoursStart,
                        onChange = { viewModel.handleEvent(SettingsUiEvent.SafeHoursStartChanged(it)) }
                    )
                    Spacer(Modifier.height(8.dp))
                    HourStepper(
                        label = "Đến",
                        hour  = state.safeHoursEnd,
                        onChange = { viewModel.handleEvent(SettingsUiEvent.SafeHoursEndChanged(it)) }
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Panic")
            ToggleRow(
                label   = "Lật úp điện thoại → panic",
                checked = state.panicFlipEnabled,
                onToggle = { viewModel.handleEvent(SettingsUiEvent.PanicFlipToggled(it)) }
            )
            Spacer(Modifier.height(8.dp))
            ToggleRow(
                label   = "Xóa dữ liệu khi panic",
                checked = state.panicWipeEnabled,
                onToggle = { viewModel.handleEvent(SettingsUiEvent.PanicWipeToggled(it)) }
            )

            Spacer(Modifier.height(24.dp))
            SectionTitle("Thông báo giả (Fake Push)")
            if (!state.hasFakePushVip) {
                Text(
                    "Nâng cấp Silver để bật tự động. Vẫn có thể gửi thử bên dưới.",
                    color = BB3TextSec,
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(8.dp))
            } else {
                ToggleRow(
                    label   = "Bật thông báo giả",
                    checked = state.fakePushEnabled,
                    onToggle = { viewModel.handleEvent(SettingsUiEvent.FakePushToggled(it)) }
                )
                Spacer(Modifier.height(8.dp))
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BB3Card)
                    .clickable { viewModel.handleEvent(SettingsUiEvent.TestFakePush) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Gửi thử thông báo", color = BB3Primary, fontSize = 15.sp)
            }
            if (state.fakePushSent) {
                Spacer(Modifier.height(8.dp))
                Text("Đã gửi — kéo từ mép trên màn hình để xem.", color = BB3Primary, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Dead-man switch")
            if (!state.hasDeadmanVip) {
                Text("Nâng cấp Gold để tự panic khi không mở app quá lâu.", color = BB3TextSec, fontSize = 13.sp)
            } else {
                ToggleRow(
                    label   = "Bật dead-man",
                    checked = state.deadmanEnabled,
                    onToggle = { viewModel.handleEvent(SettingsUiEvent.DeadmanToggled(it)) }
                )
                if (state.deadmanEnabled) {
                    Spacer(Modifier.height(8.dp))
                    DeadmanHoursStepper(
                        hours    = state.deadmanHours,
                        onChange = { viewModel.handleEvent(SettingsUiEvent.DeadmanHoursChanged(it)) }
                    )
                }
            }

            if (state.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(state.error!!, color = BB3Danger, fontSize = 13.sp)
            }
            if (state.saved) {
                Spacer(Modifier.height(12.dp))
                Text("Đã lưu cấu hình.", color = BB3Primary, fontSize = 13.sp)
            }

            Spacer(Modifier.height(24.dp))
            SaveButton { viewModel.handleEvent(SettingsUiEvent.Save) }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = BB3Primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SectionHint(text: String) {
    Text(text, color = BB3TextSec, fontSize = 13.sp)
    Spacer(Modifier.height(12.dp))
}

@Composable
private fun TextFieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    hasError: Boolean
) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(BB3Card)
            .border(1.dp, if (hasError) BB3Danger else BB3Border, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            textStyle     = TextStyle(
                color      = BB3TextPrim,
                fontSize   = 18.sp,
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush   = SolidColor(BB3Primary),
            singleLine    = true,
            modifier      = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onToggle: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = BB3TextPrim, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked         = checked,
            onCheckedChange = onToggle,
            colors          = SwitchDefaults.colors(
                checkedThumbColor   = BB3Primary,
                checkedTrackColor   = BB3Primary.copy(alpha = 0.35f),
                uncheckedThumbColor = BB3TextSec,
                uncheckedTrackColor = BB3Card
            )
        )
    }
}

@Composable
private fun HourStepper(label: String, hour: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = BB3TextSec, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StepBtn("−") { onChange((hour - 1).coerceIn(0, 23)) }
            Text("${formatTwoDigits(hour)}:00", color = BB3TextPrim, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            StepBtn("+") { onChange((hour + 1).coerceIn(0, 23)) }
        }
    }
}

@Composable
private fun DeadmanHoursStepper(hours: Int, onChange: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Ngưỡng (giờ)", color = BB3TextSec, fontSize = 14.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            StepBtn("−") { onChange((hours - 6).coerceIn(12, 72)) }
            Text("$hours h", color = BB3TextPrim, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            StepBtn("+") { onChange((hours + 6).coerceIn(12, 72)) }
        }
    }
}

@Composable
private fun StepBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BB3Card)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = BB3TextPrim, fontSize = 18.sp)
    }
}

@Composable
private fun DisguiseTypeGrid(
    selected: DisguiseType,
    lockedTypes: Set<DisguiseType>,
    onSelect: (DisguiseType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        DisguiseType.entries.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { type ->
                    val locked = type in lockedTypes
                    val active = type == selected
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (active) BB3Primary.copy(alpha = 0.2f) else BB3Card)
                            .border(
                                1.dp,
                                if (active) BB3Primary else BB3Border,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable(enabled = !locked) { onSelect(type) }
                            .padding(vertical = 10.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (locked) "🔒 ${type.label}" else type.label,
                            color = if (locked) BB3TextSec else BB3TextPrim,
                            fontSize = 11.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                repeat(3 - row.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

private fun disguiseHint(type: DisguiseType, code: String): String = when (type) {
    DisguiseType.CALCULATOR -> "Nhập \"$code\" rồi = trên máy tính để mở cổng PIN."
    DisguiseType.BANKING    -> "Nhập \"$code\" vào số tiền chuyển khoản để mở."
    else                    -> "Nhập \"$code\" vào ô tìm kiếm, hoặc chạm 5 lần vào vùng ẩn."
}

@Composable
private fun SaveButton(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BB3Primary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text("Lưu", color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    }
}
