@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bb3.bb3chat.core.platform

import com.bb3.bb3chat.core.crypto.CryptoManager
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy

actual class ImageProcessor actual constructor() {

    actual fun compressAndResize(bytes: ByteArray, maxDimension: Int, quality: Int): ByteArray {
        val image = UIImage.imageWithData(bytes.toNSData()) ?: return bytes
        val w = image.size.useContents { width.toInt() }
        val h = image.size.useContents { height.toInt() }
        val (nw, nh) = scaledDimensions(w, h, maxDimension)

        val output = if (nw == w && nh == h) {
            image
        } else {
            UIGraphicsImageRenderer(size = CGSizeMake(nw.toDouble(), nh.toDouble()))
                .imageWithActions { _ ->
                    image.drawInRect(CGRectMake(0.0, 0.0, nw.toDouble(), nh.toDouble()))
                }
        }

        val jpeg = UIImageJPEGRepresentation(output, quality / 100.0) ?: return bytes
        return jpeg.toByteArray()
    }

    actual fun generateBlurHash(bytes: ByteArray): String = ""

    @OptIn(ExperimentalEncodingApi::class)
    actual fun decryptAndDecode(encBase64: String, ivBase64: String, key: ByteArray): ByteArray {
        val cipher = Base64.Default.decode(encBase64)
        val iv     = Base64.Default.decode(ivBase64)
        return CryptoManager.decrypt(cipher, iv, key)
    }

    private fun scaledDimensions(w: Int, h: Int, max: Int): Pair<Int, Int> {
        if (w <= max && h <= max) return w to h
        val ratio = min(max.toFloat() / w, max.toFloat() / h)
        return (w * ratio).roundToInt() to (h * ratio).roundToInt()
    }
}

private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

private fun NSData.toByteArray(): ByteArray {
    val length = this.length.toInt()
    if (length == 0) return ByteArray(0)
    return ByteArray(length).apply {
        usePinned { pinned ->
            memcpy(pinned.addressOf(0), bytes, length.toULong())
        }
    }
}
