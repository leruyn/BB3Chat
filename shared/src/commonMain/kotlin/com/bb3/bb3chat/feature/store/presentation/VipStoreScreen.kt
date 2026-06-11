package com.bb3.bb3chat.feature.store.presentation

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bb3.bb3chat.ui.theme.BB3Amber
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
fun VipStoreScreen(
    viewModel : VipStoreViewModel = koinInject(),
    onDismiss : () -> Unit,
    onPayment : (String) -> Unit
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is VipStoreUiEffect.OpenPayment -> onPayment(effect.planId)
                is VipStoreUiEffect.Dismissed   -> onDismiss()
            }
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(BB3Black)
    ) {
        // Header
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF1A1200), BB3Black)
                    )
                )
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("👑", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("BB3 VIP", color = BB3Amber, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text("Bảo mật không giới hạn", color = BB3TextSec, fontSize = 14.sp)
            }
            // Close
            Box(
                Modifier.align(Alignment.TopEnd)
                    .size(32.dp).clip(CircleShape).background(BB3Card)
                    .clickable { viewModel.handleEvent(VipStoreUiEvent.Dismiss) },
                contentAlignment = Alignment.Center
            ) { Text("✕", color = BB3TextSec, fontSize = 16.sp) }
        }

        // Current tier badge
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Gói hiện tại:", color = BB3TextSec, fontSize = 13.sp)
            Spacer(Modifier.width(6.dp))
            TierBadge(state.currentTier)
        }

        Box(Modifier.fillMaxWidth().height(0.5.dp).background(BB3Border))

        // Plan cards
        LazyColumn(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)
        ) {
            items(state.plans) { plan ->
                PlanCard(
                    plan      = plan,
                    isOwned   = plan.id in state.owned,
                    isCurrent = plan.tier == state.currentTier,
                    onClick   = { viewModel.handleEvent(VipStoreUiEvent.SelectPlan(plan.id)) }
                )
            }
        }

        // Footer
        Text(
            "Thanh toán qua VNPay / Momo / Stripe\nGia hạn tự động · Huỷ bất kỳ lúc nào",
            color     = BB3TextSec,
            fontSize  = 12.sp,
            textAlign = TextAlign.Center,
            modifier  = Modifier.fillMaxWidth().padding(16.dp)
        )
    }
}

@Composable
private fun PlanCard(
    plan      : VipPlan,
    isOwned   : Boolean,
    isCurrent : Boolean,
    onClick   : () -> Unit
) {
    val highlightBorder = when {
        plan.highlighted -> BB3Amber
        isCurrent        -> BB3Primary
        else             -> BB3Border
    }
    val cardBg = if (plan.highlighted) Color(0xFF1A1200) else BB3Card

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardBg)
            .border(
                width = if (plan.highlighted || isCurrent) 1.5.dp else 0.5.dp,
                color = highlightBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(enabled = !isCurrent, onClick = onClick)
            .padding(20.dp)
    ) {
        // POPULAR badge
        if (plan.highlighted) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(bottomStart = 12.dp))
                    .background(BB3Amber)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("PHỔ BIẾN", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column {
            // Title row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(plan.badge, fontSize = 28.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(plan.name, color = BB3TextPrim, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(plan.price, color = if (plan.highlighted) BB3Amber else BB3TextSec, fontSize = 14.sp)
                }
                Spacer(Modifier.weight(1f))
                if (isCurrent) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(BB3Primary.copy(alpha = 0.18f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) { Text("Đang dùng", color = BB3Primary, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Features
            plan.features.forEach { feature ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 3.dp)
                ) {
                    Text("✓", color = BB3Primary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(feature, color = BB3TextPrim, fontSize = 14.sp)
                }
            }

            // CTA button (not shown for FREE or current plan)
            if (!isCurrent && plan.tier != VipTier.FREE) {
                Spacer(Modifier.height(16.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (plan.highlighted)
                                Brush.horizontalGradient(listOf(Color(0xFFFFD60A), Color(0xFFFF9F0A)))
                            else
                                Brush.horizontalGradient(listOf(BB3Primary, Color(0xFF25A244)))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Nâng cấp lên ${plan.name}",
                        color      = Color.Black,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun TierBadge(tier: VipTier) {
    val (label, color) = when (tier) {
        VipTier.FREE     -> "Miễn phí 🔓" to BB3TextSec
        VipTier.SILVER   -> "Silver 🥈"   to Color(0xFFB0B0C0)
        VipTier.GOLD     -> "Gold 🥇"     to BB3Amber
        VipTier.PLATINUM -> "Platinum 💎" to Color(0xFF64D2FF)
    }
    Text(label, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}
