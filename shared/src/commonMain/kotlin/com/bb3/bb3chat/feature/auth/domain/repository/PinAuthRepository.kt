package com.bb3.bb3chat.feature.auth.domain.repository

import com.bb3.bb3chat.feature.auth.domain.model.PinValidationResult

interface PinAuthRepository {
    suspend fun validatePin(inputPin: String): PinValidationResult
    suspend fun setRealPin(newPin: String)
    suspend fun setDecoyPin(newPin: String)
    suspend fun savePendingRealPin(pin: String)
    suspend fun confirmPendingRealPin(pin: String): Boolean
    fun hasPendingRealPin(): Boolean
    fun isSameAsRealPin(pin: String): Boolean
    fun isPinConfigured(): Boolean
    fun isDecoyPinConfigured(): Boolean
}
