package com.ragchat.sdk.storage

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Utility functions for symmetric int8 scalar quantization of dense float embeddings.
 */
public object Quantization {

    /**
     * Quantizes a float array into signed 8-bit integers (`ByteArray`) and a float scale factor.
     * Scale is determined by max absolute coordinate value divided by 127.
     */
    public fun quantize(values: List<Float>): Pair<Float, ByteArray> {
        if (values.isEmpty()) return 1.0f to ByteArray(0)
        var maxAbs = 0f
        for (v in values) {
            val a = abs(v)
            if (a > maxAbs) maxAbs = a
        }
        val scale = if (maxAbs == 0f) 1.0f else maxAbs / 127.0f
        val bytes = ByteArray(values.size)
        for (i in values.indices) {
            val q = (values[i] / scale).roundToInt().coerceIn(-128, 127)
            bytes[i] = q.toByte()
        }
        return scale to bytes
    }

    /**
     * Dequantizes an int8 [ByteArray] back into a [List] of [Float] using the specified [scale].
     */
    public fun dequantize(bytes: ByteArray, scale: Float): List<Float> {
        val result = ArrayList<Float>(bytes.size)
        for (b in bytes) {
            result.add(b.toFloat() * scale)
        }
        return result
    }
}
