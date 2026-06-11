package com.bb3.bb3chat.feature.auth.domain.model

sealed class PinValidationResult {
    object RealAccess  : PinValidationResult()
    object DecoyAccess : PinValidationResult()
    object InvalidPin  : PinValidationResult()
}
