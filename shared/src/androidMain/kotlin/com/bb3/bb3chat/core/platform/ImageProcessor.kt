package com.bb3.bb3chat.core.platform

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.bb3.bb3chat.core.crypto.CryptoManager
import java.io.ByteArrayOutputStream
import java.util.Base64
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

actual class ImageProcessor actual constructor() {

    actual fun compressAndResize(bytes: ByteArray, maxDimension: Int, quality: Int): ByteArray {
        val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            ?: return bytes

        val (w, h) = scaledDimensions(original.width, original.height, maxDimension)
        val scaled = if (w == original.width && h == original.height) {
            original
        } else {
            Bitmap.createScaledBitmap(original, w, h, true)
        }

        return ByteArrayOutputStream().use { out ->
            scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
            out.toByteArray()
        }.also {
            if (scaled !== original) scaled.recycle()
            original.recycle()
        }
    }

    actual fun generateBlurHash(bytes: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return ""
        val small  = Bitmap.createScaledBitmap(bitmap, 32, 32, true)
        return encodeBlurHash(small, 4, 3).also {
            small.recycle(); bitmap.recycle()
        }
    }

    actual fun decryptAndDecode(encBase64: String, ivBase64: String, key: ByteArray): ByteArray {
        val cipher = Base64.getDecoder().decode(encBase64)
        val iv     = Base64.getDecoder().decode(ivBase64)
        return CryptoManager.decrypt(cipher, iv, key)
    }

    private fun scaledDimensions(w: Int, h: Int, max: Int): Pair<Int, Int> {
        if (w <= max && h <= max) return w to h
        val ratio = min(max.toFloat() / w, max.toFloat() / h)
        return (w * ratio).roundToInt() to (h * ratio).roundToInt()
    }

    private fun encodeBlurHash(bitmap: Bitmap, cx: Int, cy: Int): String {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val sb = StringBuilder()
        // Simplified BlurHash — component count prefix
        val sizeFlag = (cx - 1) + (cy - 1) * 9
        sb.append(encode83(sizeFlag, 1))
        val maxAC: Float
        val components = mutableListOf<Triple<Float, Float, Float>>()
        for (j in 0 until cy) for (i in 0 until cx) {
            val r = if (i == 0 && j == 0) 1f else (2f / (bitmap.width * bitmap.height))
            var R = 0f; var G = 0f; var B = 0f
            for (y in 0 until bitmap.height) for (x in 0 until bitmap.width) {
                val c = pixels[y * bitmap.width + x]
                val cos = kotlin.math.cos(Math.PI * i * x / bitmap.width).toFloat() *
                          kotlin.math.cos(Math.PI * j * y / bitmap.height).toFloat()
                R += r * sRGBToLinear((c shr 16) and 0xFF) * cos
                G += r * sRGBToLinear((c shr 8) and 0xFF) * cos
                B += r * sRGBToLinear(c and 0xFF) * cos
            }
            components.add(Triple(R, G, B))
        }
        maxAC = components.drop(1).flatMap { listOf(it.first, it.second, it.third) }
            .maxOrNull()?.coerceAtLeast(1e-6f) ?: 1e-6f
        sb.append(encode83((maxAC * 166 - 0.5f).toInt().coerceIn(0, 82), 1))
        components.forEachIndexed { idx, (r, g, b) ->
            if (idx == 0) sb.append(encode83(encodeDC(r, g, b), 4))
            else sb.append(encode83(encodeAC(r, g, b, maxAC), 2))
        }
        return sb.toString()
    }

    private fun sRGBToLinear(c: Int): Float {
        val f = c / 255f
        return if (f <= 0.04045f) f / 12.92f else ((f + 0.055f) / 1.055f).let { x ->
            x * x * x
        }
    }
    private fun linearToSRGB(v: Float): Int =
        (if (v <= 0.0031308f) v * 12.92f else 1.055f * Math.pow(v.toDouble(), 1 / 2.4).toFloat() - 0.055f)
            .coerceIn(0f, 1f).let { (it * 255 + 0.5f).toInt() }

    private fun encodeDC(r: Float, g: Float, b: Float): Int =
        (linearToSRGB(r) shl 16) or (linearToSRGB(g) shl 8) or linearToSRGB(b)

    private fun encodeAC(r: Float, g: Float, b: Float, max: Float): Int {
        fun quant(v: Float) = (max(0, min(18, (kotlin.math.sign(v) * kotlin.math.sqrt(kotlin.math.abs(v.toDouble()) / max) * 9 + 9.5).toInt())))
        return quant(r) * 19 * 19 + quant(g) * 19 + quant(b)
    }

    private val CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#\$%*+,-.:;=?@[]^_{|}~"
    private fun encode83(value: Int, length: Int): String {
        var v = value; val sb = StringBuilder()
        repeat(length) { sb.insert(0, CHARS[v % 83]); v /= 83 }
        return sb.toString()
    }
}
