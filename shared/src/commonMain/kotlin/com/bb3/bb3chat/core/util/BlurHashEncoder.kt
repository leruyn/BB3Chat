package com.bb3.bb3chat.core.util

import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sign
import kotlin.math.pow
import kotlin.math.sqrt

object BlurHashEncoder {

    fun encode(pixels: IntArray, width: Int, height: Int, cx: Int = 4, cy: Int = 3): String {
        val sb = StringBuilder()
        val sizeFlag = (cx - 1) + (cy - 1) * 9
        sb.append(encode83(sizeFlag, 1))
        val components = mutableListOf<Triple<Float, Float, Float>>()
        for (j in 0 until cy) for (i in 0 until cx) {
            val r = if (i == 0 && j == 0) 1f else (2f / (width * height))
            var R = 0f; var G = 0f; var B = 0f
            for (y in 0 until height) for (x in 0 until width) {
                val c = pixels[y * width + x]
                val cosX = cos(kotlin.math.PI * i * x / width).toFloat()
                val cosY = cos(kotlin.math.PI * j * y / height).toFloat()
                R += r * sRGBToLinear((c shr 16) and 0xFF) * cosX * cosY
                G += r * sRGBToLinear((c shr 8) and 0xFF) * cosX * cosY
                B += r * sRGBToLinear(c and 0xFF) * cosX * cosY
            }
            components.add(Triple(R, G, B))
        }
        val maxAC = components.drop(1).flatMap { listOf(it.first, it.second, it.third) }
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
        return if (f <= 0.04045f) f / 12.92f else {
            val x = (f + 0.055f) / 1.055f
            x * x * x
        }
    }

    private fun linearToSRGB(v: Float): Int =
        (if (v <= 0.0031308f) v * 12.92f else 1.055f * v.toDouble().pow(1.0 / 2.4).toFloat() - 0.055f)
            .coerceIn(0f, 1f).let { (it * 255 + 0.5f).toInt() }

    private fun encodeDC(r: Float, g: Float, b: Float): Int =
        (linearToSRGB(r) shl 16) or (linearToSRGB(g) shl 8) or linearToSRGB(b)

    private fun encodeAC(r: Float, g: Float, b: Float, max: Float): Int {
        fun quant(v: Float) = max(0, min(18, (sign(v) * sqrt(kotlin.math.abs(v.toDouble()) / max) * 9 + 9.5).toInt()))
        return quant(r) * 19 * 19 + quant(g) * 19 + quant(b)
    }

    private val CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz#\$%*+,-.:;=?@[]^_{|}~"
    private fun encode83(value: Int, length: Int): String {
        var v = value; val sb = StringBuilder()
        repeat(length) { sb.insert(0, CHARS[v % 83]); v /= 83 }
        return sb.toString()
    }
}
