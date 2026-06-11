package com.bb3.bb3chat.core.bridge

/** Swift CIQRCodeGenerator produces PNG bytes for the shared pairing QR. */
object QrCodeGeneratorBridgeHolder {
    private var generateFn: ((String, Int) -> ByteArray?)? = null

    fun bindGenerator(generate: (String, Int) -> ByteArray?) {
        generateFn = generate
    }

    fun generate(content: String, sizePx: Int): ByteArray? =
        generateFn?.invoke(content, sizePx)
}
