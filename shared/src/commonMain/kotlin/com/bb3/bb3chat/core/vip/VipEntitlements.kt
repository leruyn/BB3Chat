package com.bb3.bb3chat.core.vip

import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys
import com.bb3.bb3chat.feature.store.presentation.VipTier

class VipEntitlements(private val storage: KeyValueStorage) {

    fun currentTier(): VipTier {
        val tierStr = storage.getString(StorageKeys.VIP_TIER) ?: return VipTier.FREE
        return runCatching { VipTier.valueOf(tierStr) }.getOrDefault(VipTier.FREE)
    }

    fun ownedPlans(): Set<String> =
        storage.getString(StorageKeys.VIP_OWNED_PLANS)
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet()
            ?: emptySet()

    fun hasIntruderCatch(): Boolean =
        currentTier() >= VipTier.SILVER || "silver" in ownedPlans() || "gold" in ownedPlans() || "platinum" in ownedPlans()

    fun hasSafeHours(): Boolean = hasIntruderCatch()

    fun hasCustomSelfDestruct(): Boolean = hasIntruderCatch()

    fun maxSelfDestructSeconds(): Int =
        if (hasCustomSelfDestruct()) 60 else 60

    fun allowedSelfDestructOptions(): List<Int> =
        if (hasCustomSelfDestruct()) listOf(10, 30, 60) else listOf(60)

    fun hasPremiumDisguises(): Boolean =
        currentTier() >= VipTier.GOLD || "gold" in ownedPlans() || "platinum" in ownedPlans()

    fun hasFakePush(): Boolean = hasIntruderCatch()

    fun hasDeadmanSwitch(): Boolean = hasPremiumDisguises()
}

private operator fun VipTier.compareTo(other: VipTier): Int =
    ordinal.compareTo(other.ordinal)
