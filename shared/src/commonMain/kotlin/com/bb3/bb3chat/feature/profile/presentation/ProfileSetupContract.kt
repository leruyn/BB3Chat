package com.bb3.bb3chat.feature.profile.presentation

import com.bb3.bb3chat.presentation.base.UiEffect
import com.bb3.bb3chat.presentation.base.UiEvent
import com.bb3.bb3chat.presentation.base.UiState

data class ProfileSetupUiState(
    val alias       : String  = "",
    val avatarIndex : Int     = 0,
    val error       : String? = null,
    val isSaving    : Boolean = false
) : UiState

sealed class ProfileSetupUiEvent : UiEvent {
    data class AliasChanged(val value: String) : ProfileSetupUiEvent()
    data class AvatarSelected(val index: Int) : ProfileSetupUiEvent()
    object Save : ProfileSetupUiEvent()
}

sealed class ProfileSetupUiEffect : UiEffect {
    object Completed : ProfileSetupUiEffect()
}
