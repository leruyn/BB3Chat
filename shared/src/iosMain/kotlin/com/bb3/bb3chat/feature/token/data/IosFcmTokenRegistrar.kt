package com.bb3.bb3chat.feature.token.data

import com.bb3.bb3chat.core.bridge.FcmTokenBridgeHolder
import com.bb3.bb3chat.feature.token.domain.repository.FcmTokenRegistrar

class IosFcmTokenRegistrar : FcmTokenRegistrar {
    override suspend fun registerToken(rawToken: String) {
        FcmTokenBridgeHolder.registerToken(rawToken, platform = "ios")
    }
}
