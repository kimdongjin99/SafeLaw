package com.safelaw.data.slm

import android.util.Log
import com.safelaw.domain.GroundingData
import com.safelaw.domain.PrecedentReference

/** 모델 답변을 보수적으로 정리하고, 관련 조항과 근거 판례를 최종 문자열로 조립합니다. */
class LegalAnswerPostProcessor(
    private val validator: LegalAnswerValidator = LegalAnswerValidator(),
) {

    fun process(
        generatedAnswer: String,
        groundingData: GroundingData,
    ): String {
        val normalizedAnswer = removeRepeatedSentences(normalize(generatedAnswer))
        if (groundingData.precedents.isBlank()) {
            return buildFinalAnswer(
                answer = INSUFFICIENT_GROUNDING_MESSAGE,
                articles = emptyList(),
                precedents = groundingData.precedentReferences,
            )
        }

        val correctionResult = validator.correctArticleNumbers(
            answer = normalizedAnswer,
            groundingData = groundingData.precedents,
        )
        logArticleCorrections(correctionResult.corrections)

        val correctedAnswer = correctionResult.correctedAnswer
        val numberValidation = validator.validateArticleNumbers(
            answer = correctedAnswer,
            groundingData = groundingData.precedents,
        )
        val contentValidation = validator.validateArticleContents(
            answer = correctedAnswer,
            groundingData = groundingData.precedents,
        )

        logValidationIssues(numberValidation.issues + contentValidation.issues)

        val answerWithWarnings = if (numberValidation.issues.isNotEmpty()) {
            "$correctedAnswer\n\n$UNCERTAIN_ARTICLE_NUMBER_WARNING"
        } else {
            correctedAnswer
        }

        // 조문 검증은 아직 오탐 가능성이 있으므로 답변이나 근거 조항을 삭제·차단하지 않습니다.
        // 검증 결과는 위의 경고 로그로만 남깁니다.
        val validatedArticles = numberValidation.checkedCitations
            .distinctBy { citation -> citation.canonicalKey() }

        return buildFinalAnswer(
            answer = answerWithWarnings,
            articles = validatedArticles,
            precedents = groundingData.precedentReferences,
        )
    }

    private fun normalize(answer: String): String =
        answer
            .replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()

    /** 완전히 같은 문장이 반복된 경우 두 번째 문장부터 제거합니다. */
    private fun removeRepeatedSentences(answer: String): String {
        val seen = mutableSetOf<String>()
        val filtered = SENTENCE_PATTERN.findAll(answer)
            .map { it.value.trim() }
            .filter { it.isNotEmpty() }
            .filter { sentence ->
                val key = sentence.lowercase().replace(Regex("\\s+"), " ")
                seen.add(key)
            }
            .toList()

        return normalize(filtered.joinToString(" "))
            .ifBlank { INSUFFICIENT_GROUNDING_MESSAGE }
    }

    private fun logValidationIssues(issues: List<ArticleValidationIssue>) {
        issues.forEach { issue ->
            Log.w(
                VALIDATION_TAG,
                buildString {
                    append(issue.type)
                    append(": ").append(issue.reason)
                    append(" citation=\"").append(issue.citation.rawText).append('"')
                    issue.contentOverlap?.let { append(" overlap=").append("%.3f".format(it)) }
                    append(" answerSentence=\"").append(issue.citation.sentence).append('"')
                    issue.groundingSentence?.let {
                        append(" groundingSentence=\"").append(it).append('"')
                    }
                },
            )
        }
    }

    private fun logArticleCorrections(corrections: List<ArticleCorrection>) {
        corrections.forEach { correction ->
            Log.i(
                VALIDATION_TAG,
                "ARTICLE_NUMBER_CORRECTED: " +
                    "${correction.originalCitation.rawText} -> " +
                    "${correction.correctedCitation.rawText} " +
                    "overlap=${"%.3f".format(correction.contentOverlap)}",
            )
        }
    }

    private fun buildFinalAnswer(
        answer: String,
        articles: List<ArticleCitation>,
        precedents: List<PrecedentReference>,
    ): String = buildString {
        appendLine("1. 답변")
        appendLine()
        appendLine(answer)
        appendLine()
        appendLine("2. 근거 조항")
        appendLine()
        if (articles.isEmpty()) {
            appendLine("- 답변에서 확인된 관련 조항이 없습니다.")
        } else {
            articles.forEach { citation ->
                append("- ").appendLine(citation.displayText())
            }
        }
        appendLine()
        appendLine("3. 근거 출처")
        appendLine()
        if (precedents.isEmpty()) {
            appendLine("- 제공된 근거 판례가 없습니다.")
        } else {
            precedents.forEachIndexed { index, precedent ->
                append(index + 1).append(") ")
                    .appendLine(precedent.caseName ?: "사건명 없음")
                precedent.caseNumber?.let {
                    append("   - 사건번호: ").appendLine(it)
                }
                precedent.courtName?.let {
                    append("   - 법원: ").appendLine(it)
                }
                append("   - 판례 ID: ").appendLine(precedent.id)
            }
        }
    }.trim()

    private fun ArticleCitation.canonicalKey(): String = listOf(
        lawName.orEmpty().replace(" ", ""),
        articleNumber.toString(),
        subArticleNumber?.toString().orEmpty(),
        paragraphNumber?.toString().orEmpty(),
        itemNumber?.toString().orEmpty(),
    ).joinToString(":")

    private fun ArticleCitation.displayText(): String = buildString {
        lawName?.takeIf { it.isNotBlank() }?.let { append(it).append(" ") }
        append("제").append(articleNumber).append("조")
        subArticleNumber?.let { append("의").append(it) }
        paragraphNumber?.let { append(" 제").append(it).append("항") }
        itemNumber?.let { append(" 제").append(it).append("호") }
    }

    companion object {
        private const val VALIDATION_TAG = "SafeLaw_Validation"
        private const val INSUFFICIENT_GROUNDING_MESSAGE =
            "제공된 판례 근거만으로는 신뢰할 수 있는 답변을 구성하기 어렵습니다."
        private const val UNCERTAIN_ARTICLE_NUMBER_WARNING =
            "※ 답변에 포함된 조문 번호 중 제공된 판례 근거에서 확인되지 않은 항목이 있어, 조문 번호가 확실하지 않을 수 있습니다."
        private val SENTENCE_PATTERN = Regex("[^.!?。！？\\r\\n]+[.!?。！？]?")
    }
}
