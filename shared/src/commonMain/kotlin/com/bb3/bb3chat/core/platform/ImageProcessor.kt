package com.bb3.bb3chat.core.platform

expect class ImageProcessor() {
    fun compressAndResize(bytes: ByteArray, maxDimension: Int, quality: Int): ByteArray
    fun generateBlurHash(bytes: ByteArray): String
    fun decryptAndDecode(encBase64: String, ivBase64: String, key: ByteArray): ByteArray
}
