package com.safelaw

import com.safelaw.data.db.OnnxEmbeddingHelper
import kotlin.math.sqrt
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class MeanPoolingTest {

    @Test
    fun meanPool_averagesOnlyMaskedTokens_andNormalizes() {
        val tokenEmbeddings = arrayOf(
            floatArrayOf(1f, 0f),
            floatArrayOf(3f, 4f),
            floatArrayOf(100f, 100f), // 패딩 토큰: 평균에서 빠져야 함
        )
        val attentionMask = longArrayOf(1L, 1L, 0L)

        val result = OnnxEmbeddingHelper.meanPoolAndNormalize(tokenEmbeddings, attentionMask)

        // 평균 (2, 2) → 정규화 (1/√2, 1/√2)
        val expected = (1 / sqrt(2.0)).toFloat()
        assertArrayEquals(floatArrayOf(expected, expected), result, 1e-6f)
    }

    @Test
    fun meanPool_resultHasUnitLength() {
        val tokenEmbeddings = Array(5) { i -> FloatArray(384) { d -> (i + 1) * 0.01f * (d % 7 - 3) } }

        val result = OnnxEmbeddingHelper.meanPoolAndNormalize(tokenEmbeddings, LongArray(5) { 1L })

        assertEquals(384, result.size)
        assertEquals(1.0, sqrt(result.sumOf { (it * it).toDouble() }), 1e-5)
    }
}
