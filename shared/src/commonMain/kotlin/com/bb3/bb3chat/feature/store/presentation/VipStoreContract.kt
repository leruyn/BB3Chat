package com.bb3.bb3chat.feature.store.presentation

import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

data class VipStoreUiState(
    val plans       : List<VipPlan>  = VipPlan.catalog(),
    val owned       : Set<String>    = emptySet(),    // planId set from storage
    val isLoading   : Boolean        = false,
    val currentTier : VipTier        = VipTier.FREE
) : UiState

enum class VipTier { FREE, SILVER, GOLD, PLATINUM }

data class VipPlan(
    val id          : String,
    val tier        : VipTier,
    val name        : String,
    val price       : String,          // e.g. "99.000₫/tháng"
    val features    : List<String>,
    val badge       : String,          // emoji
    val highlighted : Boolean = false
) {
    companion object {
        fun catalog() = listOf(
            VipPlan(
                id = "free", tier = VipTier.FREE,
                name = "Miễn Phí", price = "0₫",
                badge = "🔓",
                features = listOf("2 phòng chat", "Tự huỷ 60s", "Mã hoá AES-256")
            ),
            VipPlan(
                id = "silver", tier = VipTier.SILVER,
                name = "Silver", price = "49.000₫/tháng",
                badge = "🥈",
                features = listOf("10 phòng chat", "Tự huỷ tùy chỉnh", "Bắt thâm nhập",
                    "Safe Hours", "Không quảng cáo")
            ),
            VipPlan(
                id = "gold", tier = VipTier.GOLD,
                name = "Gold", price = "99.000₫/tháng",
                badge = "🥇", highlighted = true,
                features = listOf("Không giới hạn phòng", "Gửi video 100MB",
                    "Relay VPS riêng", "Ưu tiên FCM token", "Ẩn danh nâng cao",
                    "Pull-Destruct hàng loạt")
            ),
            VipPlan(
                id = "platinum", tier = VipTier.PLATINUM,
                name = "Platinum", price = "199.000₫/tháng",
                badge = "💎",
                features = listOf("Tất cả Gold", "Nhóm chat kín 5 người",
                    "Key rotation tự động 24h", "Hỗ trợ ưu tiên",
                    "Burn-on-sight mode")
            )
        )
    }
}

sealed class VipStoreUiEvent : UiEvent {
    data class SelectPlan(val planId: String) : VipStoreUiEvent()
    object Dismiss                            : VipStoreUiEvent()
}

sealed class VipStoreUiEffect : UiEffect {
    data class OpenPayment(val planId: String) : VipStoreUiEffect()
    object Dismissed                           : VipStoreUiEffect()
}
