package com.bb3.bb3chat.core.platform

/** Generates a PNG-encoded QR code bitmap for the given payload. */
expect class QrCodeGenerator() {
    fun generate(content: String, sizePx: Int = 512): ByteArray?
}
