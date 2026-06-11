@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.bb3.bb3chat.core.platform

import com.bb3.bb3chat.core.crypto.CryptoManager
import com.bb3.bb3chat.core.util.BlurHashEncoder
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Image as SkiaImage
import org.jetbrains.skia.Rect
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

    actual fun generateBlurHash(bytes: ByteArray): String {
        val image = SkiaImage.makeFromEncoded(bytes) ?: return ""
        val w = 32
        val h = 32
        val bitmap = Bitmap()
        bitmap.allocN32Pixels(w, h)
        Canvas(bitmap).drawImageRect(image, Rect.makeWH(w.toFloat(), h.toFloat()))
        val pixmap = bitmap.peekPixels() ?: return ""
        val pixels = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            val color = pixmap.getColor(x, y)
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            pixels[y * w + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return BlurHashEncoder.encode(pixels, w, h)
    }

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
