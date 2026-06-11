package com.bb3.bb3chat.core.platform

interface IntruderCapture {
    suspend fun capture(attemptCount: Int)
}
