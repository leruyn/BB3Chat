package com.bb3.bb3chat.feature.auth.domain.usecase

import com.bb3.bb3chat.feature.auth.domain.repository.PinAuthRepository

class SetupPinUseCase(private val repository: PinAuthRepository) {
    suspend fun setRealPin(pin: String) {
        require(pin.length >= 4) { "PIN phải có ít nhất 4 ký tự" }
        repository.setRealPin(pin)
    }

    suspend fun setDecoyPin(pin: String) {
        require(pin.length >= 4) { "Decoy PIN phải có ít nhất 4 ký tự" }
        repository.setDecoyPin(pin)
    }
}
