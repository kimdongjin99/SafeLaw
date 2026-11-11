package com.safelaw.data.db

import java.text.Normalizer
import java.util.Locale

/**
 * HuggingFace BertTokenizer와 같은 방식으로 문장을 토큰 ID로 바꿉니다.
 * 순서: 텍스트 정리 → (소문자 + 악센트 제거) → 문장부호 분리 → WordPiece
 *
 * KR-SBERT(snunlp/KR-SBERT-V40K-klueNLI-augSTS)는 do_lower_case=false라서
 * 소문자·악센트 제거를 하지 않습니다. 한글 음절이 자모로 쪼개지지 않고 그대로 사전에서 찾아집니다.
 */
class BertWordPieceTokenizer(
    private val vocab: Map<String, Int>,
    private val maxLength: Int = DEFAULT_MAX_LENGTH,
    // uncased 모델(all-MiniLM 등)만 true. true면 한글이 자모로 분해됩니다.
    private val lowerCase: Boolean = false,
) {

    class Encoding(
        val inputIds: LongArray,
        val attentionMask: LongArray,
        val tokenTypeIds: LongArray,
    )

    private val clsId = vocab.getValue(CLS_TOKEN)
    private val sepId = vocab.getValue(SEP_TOKEN)
    private val unkId = vocab.getValue(UNK_TOKEN)

    /** [CLS] 문장 토큰 [SEP] 형태로 인코딩합니다. 길면 maxLength에 맞춰 뒤를 자릅니다. */
    fun encode(text: String): Encoding {
        val wordPieceIds = basicTokenize(text)
            .flatMap { wordPieceTokenize(it) }
            .take(maxLength - 2)

        val inputIds = (listOf(clsId) + wordPieceIds + sepId)
            .map { it.toLong() }
            .toLongArray()

        return Encoding(
            inputIds = inputIds,
            attentionMask = LongArray(inputIds.size) { 1L },
            tokenTypeIds = LongArray(inputIds.size) { 0L },
        )
    }

    private fun basicTokenize(text: String): List<String> {
        val cleaned = StringBuilder()
        text.codePoints().forEach { cp ->
            when {
                cp == 0 || cp == 0xFFFD || isControl(cp) -> Unit
                isWhitespace(cp) -> cleaned.append(' ')
                // 한자는 글자마다 따로 떼어냅니다. (한글은 해당 안 됨)
                isChineseChar(cp) -> cleaned.append(' ').appendCodePoint(cp).append(' ')
                else -> cleaned.appendCodePoint(cp)
            }
        }

        return cleaned.split(' ')
            .filter { it.isNotEmpty() }
            .flatMap { word ->
                val normalized = if (lowerCase) stripAccents(word.lowercase(Locale.ROOT)) else word
                splitOnPunctuation(normalized)
            }
    }

    // NFD로 분해한 뒤 결합용 기호(악센트)를 제거합니다. 한글 음절은 자모로 분해됩니다.
    private fun stripAccents(text: String): String {
        val decomposed = Normalizer.normalize(text, Normalizer.Form.NFD)
        val result = StringBuilder()
        decomposed.codePoints().forEach { cp ->
            if (Character.getType(cp) != Character.NON_SPACING_MARK.toInt()) {
                result.appendCodePoint(cp)
            }
        }
        return result.toString()
    }

    private fun splitOnPunctuation(text: String): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        text.codePoints().forEach { cp ->
            if (isPunctuation(cp)) {
                if (current.isNotEmpty()) {
                    tokens.add(current.toString())
                    current.clear()
                }
                tokens.add(String(Character.toChars(cp)))
            } else {
                current.appendCodePoint(cp)
            }
        }
        if (current.isNotEmpty()) {
            tokens.add(current.toString())
        }
        return tokens
    }

    // 사전에서 가장 긴 조각부터 맞춰 나갑니다. 이어지는 조각은 "##"을 붙여 찾습니다.
    private fun wordPieceTokenize(word: String): List<Int> {
        val chars = word.codePoints().toArray()
        if (chars.size > MAX_CHARS_PER_WORD) {
            return listOf(unkId)
        }

        val ids = mutableListOf<Int>()
        var start = 0
        while (start < chars.size) {
            var end = chars.size
            var matchedId: Int? = null
            while (start < end) {
                val piece = String(chars, start, end - start)
                val candidate = if (start > 0) "##$piece" else piece
                val id = vocab[candidate]
                if (id != null) {
                    matchedId = id
                    break
                }
                end--
            }
            if (matchedId == null) {
                return listOf(unkId)
            }
            ids.add(matchedId)
            start = end
        }
        return ids
    }

    private fun isWhitespace(cp: Int): Boolean =
        cp == ' '.code || cp == '\t'.code || cp == '\n'.code || cp == '\r'.code ||
            Character.getType(cp) == Character.SPACE_SEPARATOR.toInt()

    private fun isControl(cp: Int): Boolean {
        if (cp == '\t'.code || cp == '\n'.code || cp == '\r'.code) {
            return false
        }
        val type = Character.getType(cp)
        return type == Character.CONTROL.toInt() || type == Character.FORMAT.toInt()
    }

    private fun isPunctuation(cp: Int): Boolean {
        if (cp in 33..47 || cp in 58..64 || cp in 91..96 || cp in 123..126) {
            return true
        }
        return when (Character.getType(cp).toByte()) {
            Character.CONNECTOR_PUNCTUATION,
            Character.DASH_PUNCTUATION,
            Character.START_PUNCTUATION,
            Character.END_PUNCTUATION,
            Character.INITIAL_QUOTE_PUNCTUATION,
            Character.FINAL_QUOTE_PUNCTUATION,
            Character.OTHER_PUNCTUATION -> true
            else -> false
        }
    }

    private fun isChineseChar(cp: Int): Boolean =
        cp in 0x4E00..0x9FFF || cp in 0x3400..0x4DBF || cp in 0x20000..0x2A6DF ||
            cp in 0x2A700..0x2B73F || cp in 0x2B740..0x2B81F || cp in 0x2B820..0x2CEAF ||
            cp in 0xF900..0xFAFF || cp in 0x2F800..0x2FA1F

    companion object {
        // KR-SBERT의 max_seq_length (sentence_bert_config.json)
        const val DEFAULT_MAX_LENGTH = 128
        private const val MAX_CHARS_PER_WORD = 100
        private const val CLS_TOKEN = "[CLS]"
        private const val SEP_TOKEN = "[SEP]"
        private const val UNK_TOKEN = "[UNK]"

        /** vocab.txt 한 줄 = 토큰 하나, 줄 번호 = 토큰 ID */
        fun fromVocabLines(
            lines: Sequence<String>,
            maxLength: Int = DEFAULT_MAX_LENGTH,
            lowerCase: Boolean = false,
        ): BertWordPieceTokenizer {
            val vocab = HashMap<String, Int>()
            lines.forEachIndexed { index, token -> vocab[token] = index }
            return BertWordPieceTokenizer(vocab, maxLength, lowerCase)
        }
    }
}
