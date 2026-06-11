package com.bb3.bb3chat.core.platform

import com.bb3.bb3chat.core.bridge.QrCodeGeneratorBridgeHolder

actual class QrCodeGenerator actual constructor() {
    actual fun generate(content: String, sizePx: Int): ByteArray? =
        QrCodeGeneratorBridgeHolder.generate(content, sizePx)
}
