package com.bb3.bb3chat.feature.auth.domain.usecase

import com.bb3.bb3chat.feature.auth.domain.model.PinValidationResult
import com.bb3.bb3chat.feature.auth.domain.repository.PinAuthRepository

class ValidatePinUseCase(private val repository: PinAuthRepository) {
    suspend operator fun invoke(pin: String): PinValidationResult =
        repository.validatePin(pin)
}
