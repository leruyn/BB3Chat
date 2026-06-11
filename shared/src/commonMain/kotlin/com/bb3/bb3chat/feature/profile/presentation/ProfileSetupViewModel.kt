package com.bb3.bb3chat.feature.profile.presentation

import com.bb3.bb3chat.core.profile.UserProfileRepository
import com.bb3.bb3chat.presentation.base.BaseViewModel

class ProfileSetupViewModel(
    private val userProfile: UserProfileRepository
) : BaseViewModel<ProfileSetupUiState, ProfileSetupUiEvent, ProfileSetupUiEffect>(
    ProfileSetupUiState(alias = userProfile.getAlias())
) {

    override suspend fun onEvent(event: ProfileSetupUiEvent) {
        when (event) {
            is ProfileSetupUiEvent.AliasChanged ->
                updateState { copy(alias = event.value, error = null) }
            is ProfileSetupUiEvent.AvatarSelected ->
                updateState { copy(avatarIndex = event.index, error = null) }
            is ProfileSetupUiEvent.Save -> save()
        }
    }

    private suspend fun save() {
        val alias = currentState.alias.trim()
        if (alias.length < 2) {
            updateState { copy(error = "Biệt danh cần ít nhất 2 ký tự") }
            return
        }
        updateState { copy(isSaving = true, error = null) }
        runCatching {
            userProfile.saveProfile(alias, currentState.avatarIndex)
        }.onSuccess {
            updateState { copy(isSaving = false) }
            emitEffect(ProfileSetupUiEffect.Completed)
        }.onFailure { err ->
            updateState {
                copy(isSaving = false, error = err.message ?: "Không lưu được hồ sơ")
            }
        }
    }
}
