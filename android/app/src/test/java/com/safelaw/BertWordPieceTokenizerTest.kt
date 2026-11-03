package com.safelaw

import com.safelaw.data.db.BertWordPieceTokenizer
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 정답 토큰 ID는 HuggingFace tokenizers(BertWordPieceTokenizer, lowercase=True)에
 * 같은 vocab.txt를 넣어 만든 값입니다.
 */
class BertWordPieceTokenizerTest {

    private val tokenizer = File("src/main/assets/vocab.txt").useLines { lines ->
        BertWordPieceTokenizer.fromVocabLines(lines)
    }

    private val expectedIds = listOf(
        "친구가 돈을 안 갚아요" to longArrayOf(101L, 1465L, 30019L, 30021L, 29991L, 30014L, 29991L, 30006L, 1457L, 30011L, 30021L, 29999L, 30017L, 30022L, 1463L, 30006L, 30021L, 100L, 102L),
        "친구가 돈을 안 갚아요 ㅠㅠ" to longArrayOf(101L, 1465L, 30019L, 30021L, 29991L, 30014L, 29991L, 30006L, 1457L, 30011L, 30021L, 29999L, 30017L, 30022L, 1463L, 30006L, 30021L, 100L, 100L, 102L),
        "보이스피싱 피해를 당했어요. 어떻게 해야 하나요?" to longArrayOf(101L, 1460L, 30011L, 29999L, 30019L, 29997L, 30017L, 30004L, 30019L, 29997L, 30019L, 30025L, 1468L, 30019L, 30005L, 30007L, 29994L, 30017L, 30022L, 100L, 1012L, 100L, 100L, 1469L, 30006L, 29992L, 30006L, 29999L, 30013L, 1029L, 102L),
        "Hello, World! I can't believe it's 2024." to longArrayOf(101L, 7592L, 1010L, 2088L, 999L, 1045L, 2064L, 1005L, 1056L, 2903L, 2009L, 1005L, 1055L, 16798L, 2549L, 1012L, 102L),
        "Café naïve résumé" to longArrayOf(101L, 7668L, 15743L, 13746L, 102L),
        "형법 제347조(사기) 위반" to longArrayOf(101L, 1469L, 30010L, 30025L, 29996L, 30008L, 30024L, 1464L, 30009L, 22022L, 2581L, 30000L, 30011L, 1006L, 1461L, 30006L, 29991L, 30019L, 1007L, 100L, 102L),
        "中文字符 테스트" to longArrayOf(101L, 1746L, 1861L, 100L, 100L, 1467L, 30009L, 29997L, 30017L, 30003L, 30017L, 102L),
        "tab\tnew\nline   spaces" to longArrayOf(101L, 21628L, 2047L, 2240L, 7258L, 102L),
        "" to longArrayOf(101L, 102L),
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

        assertEquals(256, encoding.inputIds.size)
        assertEquals(102L, encoding.inputIds.last())
        assertEquals(2375L, encoding.inputIds[encoding.inputIds.size - 2])
    }

    @Test
    fun encode_buildsMaskAndTypeIds() {
        val encoding = tokenizer.encode("친구가 돈을 안 갚아요")

        assertEquals(encoding.inputIds.size, encoding.attentionMask.size)
        assertEquals(1L, encoding.attentionMask.distinct().single())
        assertEquals(0L, encoding.tokenTypeIds.distinct().single())
    }
}
