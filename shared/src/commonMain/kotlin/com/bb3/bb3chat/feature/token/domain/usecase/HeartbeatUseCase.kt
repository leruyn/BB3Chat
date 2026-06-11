package com.bb3.bb3chat.feature.token.domain.usecase

import com.bb3.bb3chat.feature.token.domain.repository.TokenRepository
import kotlinx.datetime.Clock

private const val THREE_DAYS_MS = 3L * 24 * 60 * 60 * 1000

class HeartbeatUseCase(private val repository: TokenRepository) {
    suspend operator fun invoke() {
        val now      = Clock.System.now().toEpochMilliseconds()
        val lastBeat = repository.getLastHeartbeatMs()
        if (now - lastBeat >= THREE_DAYS_MS) {
            repository.sendHeartbeat()
        }
    }
}
