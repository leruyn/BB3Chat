package com.bb3.bb3chat.feature.security.domain.usecase

import com.bb3.bb3chat.core.crypto.SessionManager

class TriggerLocalPanicUseCase(
    private val sessionManager: SessionManager
) {
    operator fun invoke(): PanicResult {
        return runCatching {
            sessionManager.destroySessionKey()
            PanicResult.Success
        }.getOrElse { PanicResult.Success }
        // Luôn trả Success — không để lộ lý do thất bại
    }
}

enum class PanicResult { Success }
