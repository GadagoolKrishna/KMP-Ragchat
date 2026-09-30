package com.ragchat.sdk.storage

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class QuantizationTest {

    @Test
    fun testQuantizationRoundtrip() {
        val original = listOf(0.0f, 0.5f, -0.5f, 1.0f, -1.0f, 0.123f, -0.789f)
        val (scale, bytes) = Quantization.quantize(original)
        val restored = Quantization.dequantize(bytes, scale)

        assertEquals(original.size, restored.size)
        for (i in original.indices) {
            val error = abs(original[i] - restored[i])
            assertTrue(error <= scale + 0.01f, "Error $error too high at index $i")
        }
    }

    @Test
    fun testEmptyQuantization() {
        val (scale, bytes) = Quantization.quantize(emptyList())
        val restored = Quantization.dequantize(bytes, scale)
        assertTrue(restored.isEmpty())
    }
}
