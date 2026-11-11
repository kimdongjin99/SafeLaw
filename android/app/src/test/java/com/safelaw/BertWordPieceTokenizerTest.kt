package com.safelaw

import com.safelaw.data.db.BertWordPieceTokenizer
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 정답 토큰 ID는 HuggingFace transformers의 KR-SBERT 토크나이저
 * (snunlp/KR-SBERT-V40K-klueNLI-augSTS, BertTokenizer fast/slow 둘 다 같음)로 만든 값입니다.
 */
class BertWordPieceTokenizerTest {

    private val tokenizer = File("src/main/assets/vocab.txt").useLines { lines ->
        BertWordPieceTokenizer.fromVocabLines(lines)
    }

    private val expectedIds = listOf(
        "친구가 돈을 안 갚아요" to longArrayOf(2, 15612, 9625, 3488, 2053, 9710, 3),
        "친구가 돈을 안 갚아요 ㅠㅠ" to longArrayOf(2, 15612, 9625, 3488, 2053, 9710, 20395, 3),
        "보이스피싱 피해를 당했어요. 어떻게 해야 하나요?" to longArrayOf(2, 35894, 11448, 2481, 13054, 18, 8642, 8744, 24912, 35, 3),
        "친구가 돈을 빌려가서 연락이 안 되는데 이거 처벌 가능한가요?" to
            longArrayOf(2, 15612, 9625, 14303, 9405, 24994, 3488, 11918, 9458, 9682, 10284, 11375, 35, 3),
        "Hello, World! I can't believe it's 2024." to
            longArrayOf(2, 44, 25932, 5394, 16, 59, 10215, 26760, 5, 45, 71, 9834, 11, 88, 29845, 5302, 19715, 5522, 5188, 33718, 11, 87, 11171, 5238, 18, 3),
        "Café naïve résumé" to longArrayOf(2, 39, 26273, 6550, 1, 86, 6550, 5395, 18509, 6550, 3),
        "형법 제347조(사기) 위반" to longArrayOf(2, 35757, 10824, 26862, 5333, 12, 9886, 13, 10286, 3),
        "中文字符 테스트" to longArrayOf(2, 600, 1222, 946, 1575, 15174, 3),
        "tab\tnew\nline   spaces" to longArrayOf(2, 88, 21033, 82, 17223, 80, 27075, 87, 5172, 21584, 10553, 3),
        "" to longArrayOf(2, 3),
    )

    @Test
    fun encode_matchesHuggingFaceTokenizer() {
        for ((text, expected) in expectedIds) {
            assertArrayEquals("토큰 ID 불일치: \"$text\"", expected, tokenizer.encode(text).inputIds)
        }
    }

    @Test
    fun encode_truncatesToMaxLength() {
        val text = "판례 ".repeat(10) + "law ".repeat(300)

        val encoding = tokenizer.encode(text)

        assertEquals(128, encoding.inputIds.size)
        assertEquals(3L, encoding.inputIds.last())
        assertEquals(5347L, encoding.inputIds[encoding.inputIds.size - 2])
    }

    @Test
    fun encode_buildsMaskAndTypeIds() {
        val encoding = tokenizer.encode("친구가 돈을 안 갚아요")

        assertEquals(encoding.inputIds.size, encoding.attentionMask.size)
        assertEquals(1L, encoding.attentionMask.distinct().single())
        assertEquals(0L, encoding.tokenTypeIds.distinct().single())
    }
}
