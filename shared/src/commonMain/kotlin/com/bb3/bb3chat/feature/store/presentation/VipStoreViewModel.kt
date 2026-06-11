package com.bb3.bb3chat.feature.store.presentation

import com.bb3.bb3chat.core.storage.KeyValueStorage
import com.bb3.bb3chat.core.storage.StorageKeys
import com.bb3.bb3chat.presentation.base.BaseViewModel
import kotlinx.coroutines.launch

class VipStoreViewModel(
    private val storage: KeyValueStorage
) : BaseViewModel<VipStoreUiState, VipStoreUiEvent, VipStoreUiEffect>(VipStoreUiState()) {

    init {
        scope.launch {
            val tierStr = storage.getString(StorageKeys.VIP_TIER) ?: "FREE"
            val tier    = runCatching { VipTier.valueOf(tierStr) }.getOrDefault(VipTier.FREE)
            val owned   = storage.getString(StorageKeys.VIP_OWNED_PLANS)
                ?.split(",")?.toSet() ?: emptySet()
            updateState { copy(currentTier = tier, owned = owned) }
        }
    }

    override suspend fun onEvent(event: VipStoreUiEvent) {
        when (event) {
            is VipStoreUiEvent.SelectPlan -> emitEffect(VipStoreUiEffect.OpenPayment(event.planId))
            is VipStoreUiEvent.Dismiss    -> emitEffect(VipStoreUiEffect.Dismissed)
        }
    }

    /** Called by payment callback after successful purchase */
    fun onPurchaseSuccess(planId: String) {
        scope.launch {
            val plan   = currentState.plans.firstOrNull { it.id == planId } ?: return@launch
            val owned  = currentState.owned + planId
            storage.putString(StorageKeys.VIP_TIER, plan.tier.name)
            storage.putString(StorageKeys.VIP_OWNED_PLANS, owned.joinToString(","))
            updateState { copy(currentTier = plan.tier, owned = owned) }
        }
    }
}
