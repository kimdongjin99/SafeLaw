package com.safelaw

import com.safelaw.data.db.LawCaseImporter
import com.safelaw.data.db.data.LawCase
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/** Colab 노트북이 만드는 law_cases_krsbert.bin 형식을 앱이 그대로 읽는지 확인합니다. */
class LawCaseBinaryReaderTest {

    private val dim = LawCaseImporter.EMBEDDING_DIMENSION

    // 노트북과 같은 방식으로 파일을 만듭니다: "SLAW" | 1 | 개수 | 차원, 이후 (id, float32 × 차원)
    private fun buildFile(ids: List<Long>, dimension: Int = dim): ByteArray {
        val buffer = ByteBuffer.allocate(16 + ids.size * (8 + dimension * 4)).order(ByteOrder.LITTLE_ENDIAN)
        buffer.put("SLAW".toByteArray()).putInt(1).putInt(ids.size).putInt(dimension)
        ids.forEach { id ->
            buffer.putLong(id)
            repeat(dimension) { d -> buffer.putFloat(id * 0.001f + d) }
        }
        return buffer.array()
    }

    @Test
    fun readAll_readsIdsAndEmbeddingsAcrossBatches() {
        val ids = (1L..2500L).map { it * 7 } // 1000개 묶음 3번(1000, 1000, 500)
        val read = mutableListOf<LawCase>()
        val progress = mutableListOf<Int>()

        val count = LawCaseImporter.readAll(ByteArrayInputStream(buildFile(ids))) { batch, readCount ->
            read += batch
            progress += readCount
        }

        assertEquals(2500, count)
        assertEquals(listOf(1000, 2000, 2500), progress)
        assertEquals(ids, read.map { it.id })
        val last = read.last()
        assertEquals(dim, last.embedding!!.size)
        assertArrayEquals(
            floatArrayOf(last.id * 0.001f, last.id * 0.001f + 1, last.id * 0.001f + (dim - 1)),
            floatArrayOf(last.embedding!![0], last.embedding!![1], last.embedding!![dim - 1]),
            0f,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun readAll_rejectsWrongDimension() {
        LawCaseImporter.readAll(ByteArrayInputStream(buildFile(listOf(1L), dimension = 384))) { _, _ -> }
    }

    @Test(expected = IllegalArgumentException::class)
    fun readAll_rejectsOtherFiles() {
        val notOurFile = "[{\"id\": 1}]".toByteArray() + ByteArray(16)
        LawCaseImporter.readAll(ByteArrayInputStream(notOurFile)) { _, _ -> }
    }
}
