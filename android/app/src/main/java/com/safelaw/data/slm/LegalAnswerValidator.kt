package com.safelaw.data.slm

import kotlin.math.max

/**
 * SLM 답변에 포함된 법률 조문을 제공된 근거 문자열과 대조합니다.
 *
 * 이 검증기는 법령 원문을 조회하지 않습니다. 따라서 검증 결과는 오직 groundingData에
 * 포함된 조문과 문장을 기준으로 하며, 실제 법률의 정확성을 최종 보증하지 않습니다.
 */
class LegalAnswerValidator(
    private val minimumContentOverlap: Double = DEFAULT_MINIMUM_CONTENT_OVERLAP,
    private val minimumCorrectionOverlap: Double = DEFAULT_MINIMUM_CORRECTION_OVERLAP,
) {

    init {
        require(minimumContentOverlap in 0.0..1.0) {
            "minimumContentOverlap must be between 0.0 and 1.0"
        }
        require(minimumCorrectionOverlap in 0.0..1.0) {
            "minimumCorrectionOverlap must be between 0.0 and 1.0"
        }
    }

    /**
     * 근거에 없는 조문을 인용한 문장이 판례의 특정 문장과 충분히 유사할 때 조문 문자열만 보정합니다.
     * 후보가 모호하거나 법률명이 충돌하면 원문을 유지합니다.
     */
    fun correctArticleNumbers(
        answer: String,
        groundingData: String,
    ): ArticleCorrectionResult {
        val numberValidation = validateArticleNumbers(answer, groundingData)
        val groundingCitations = extractArticleCitations(groundingData)
        var correctedAnswer = answer
        val corrections = mutableListOf<ArticleCorrection>()

        numberValidation.issues
            .filter { it.type == ArticleValidationIssueType.ARTICLE_NUMBER_NOT_FOUND }
            .distinctBy { it.citation.sentence to it.citation.rawText }
            .forEach { issue ->
                val answerCitation = issue.citation
                val candidates = groundingCitations
                    .asSequence()
                    .filter { candidate -> answerCitation.hasCompatibleLawName(candidate) }
                    .mapNotNull { candidate ->
                        correctionContentOverlap(
                            answerSentence = answerCitation.sentence,
                            groundingSentence = candidate.sentence,
                        )?.let { overlap -> candidate to overlap }
                    }
                    .sortedByDescending { (_, overlap) -> overlap }
                    .toList()

                val best = candidates.firstOrNull() ?: return@forEach
                val secondBestOverlap = candidates.getOrNull(1)?.second
                val isUnambiguous = secondBestOverlap == null ||
                    best.second - secondBestOverlap >= MINIMUM_CORRECTION_MARGIN
                if (best.second < minimumCorrectionOverlap || !isUnambiguous) {
                    return@forEach
                }

                val correctedSentence = answerCitation.sentence.replace(
                    oldValue = answerCitation.rawText,
                    newValue = best.first.rawText,
                )
                correctedAnswer = correctedAnswer.replace(
                    oldValue = answerCitation.sentence,
                    newValue = correctedSentence,
                )
                corrections += ArticleCorrection(
                    originalCitation = answerCitation,
                    correctedCitation = best.first,
                    contentOverlap = best.second,
                )
            }

        return ArticleCorrectionResult(
            correctedAnswer = correctedAnswer,
            corrections = corrections,
        )
    }

    /** 답변이 인용한 각 조문 번호가 groundingData에도 존재하는지 검사합니다. */
    fun validateArticleNumbers(
        answer: String,
        groundingData: String,
    ): ArticleValidationResult {
        val answerCitations = extractArticleCitations(answer)
        val groundingCitations = extractArticleCitations(groundingData)

        val issues = answerCitations.mapNotNull { answerCitation ->
            val existsInGrounding = groundingCitations.any { groundingCitation ->
                answerCitation.referencesSameArticleAs(groundingCitation)
            }

            if (existsInGrounding) {
                null
            } else {
                ArticleValidationIssue(
                    type = ArticleValidationIssueType.ARTICLE_NUMBER_NOT_FOUND,
                    citation = answerCitation,
                    reason = "답변에 인용된 조문 번호를 제공된 근거에서 찾을 수 없습니다.",
                )
            }
        }

        return ArticleValidationResult(
            isValid = issues.isEmpty(),
            checkedCitations = answerCitations,
            issues = issues,
        )
    }

    /**
     * 같은 조문을 포함한 답변 문장과 근거 문장의 핵심 단어 중첩률을 비교합니다.
     *
     * 조문 번호가 근거에 없는 경우는 [validateArticleNumbers]의 책임이므로 이 메서드에서는
     * 건너뜁니다. 두 검증은 번호 검증 후 내용 검증 순서로 호출하는 것을 전제로 합니다.
     */
    fun validateArticleContents(
        answer: String,
        groundingData: String,
    ): ArticleValidationResult {
        val answerCitations = extractArticleCitations(answer)
        val groundingCitations = extractArticleCitations(groundingData)
        val checkedCitations = mutableListOf<ArticleCitation>()
        val issues = mutableListOf<ArticleValidationIssue>()

        answerCitations.forEach { answerCitation ->
            val candidates = groundingCitations.filter { groundingCitation ->
                answerCitation.referencesSameArticleAs(groundingCitation)
            }
            if (candidates.isEmpty()) {
                return@forEach
            }

            checkedCitations += answerCitation
            val bestCandidate = candidates.maxByOrNull { groundingCitation ->
                contentOverlap(answerCitation.sentence, groundingCitation.sentence)
            } ?: return@forEach
            val overlap = contentOverlap(answerCitation.sentence, bestCandidate.sentence)

            if (overlap < minimumContentOverlap) {
                issues += ArticleValidationIssue(
                    type = ArticleValidationIssueType.ARTICLE_CONTENT_MISMATCH,
                    citation = answerCitation,
                    reason = "조문 설명과 근거 문장의 핵심 내용이 충분히 일치하지 않습니다.",
                    groundingSentence = bestCandidate.sentence,
                    contentOverlap = overlap,
                )
            }
        }

        return ArticleValidationResult(
            isValid = issues.isEmpty(),
            checkedCitations = checkedCitations,
            issues = issues,
        )
    }

    internal fun extractArticleCitations(text: String): List<ArticleCitation> =
        splitIntoSentences(text).flatMap { sentence ->
            ARTICLE_PATTERN.findAll(sentence).map { match ->
                ArticleCitation(
                    rawText = match.value,
                    lawName = match.groups[1]?.value?.trim(),
                    articleNumber = match.groupValues[2].toInt(),
                    subArticleNumber = match.groups[3]?.value?.toIntOrNull(),
                    paragraphNumber = match.groups[4]?.value?.toIntOrNull(),
                    itemNumber = match.groups[5]?.value?.toIntOrNull(),
                    sentence = sentence.trim(),
                )
            }.toList()
        }

    private fun contentOverlap(answerSentence: String, groundingSentence: String): Double {
        val answerKeywords = extractKeywords(answerSentence)
        if (answerKeywords.isEmpty()) {
            return 1.0
        }

        val groundingKeywords = extractKeywords(groundingSentence)
        val commonKeywordCount = answerKeywords.intersect(groundingKeywords).size
        return commonKeywordCount.toDouble() / max(answerKeywords.size, 1)
    }

    private fun correctionContentOverlap(
        answerSentence: String,
        groundingSentence: String,
    ): Double? {
        val answerKeywords = extractKeywords(answerSentence)
        if (answerKeywords.size < MINIMUM_CORRECTION_KEYWORD_COUNT) {
            return null
        }

        val groundingKeywords = extractKeywords(groundingSentence)
        val commonKeywordCount = answerKeywords.intersect(groundingKeywords).size
        return commonKeywordCount.toDouble() / answerKeywords.size
    }

    private fun extractKeywords(sentence: String): Set<String> =
        ARTICLE_PATTERN.replace(sentence, " ")
            .lowercase()
            .split(NON_WORD_PATTERN)
            .asSequence()
            .map { it.trim() }
            .filter { it.length >= MINIMUM_KEYWORD_LENGTH }
            .filterNot { it in STOP_WORDS }
            .toSet()

    private fun splitIntoSentences(text: String): List<String> =
        text.split(SENTENCE_BOUNDARY_PATTERN)
            .map { it.trim() }
            .filter { it.isNotEmpty() }

    private fun ArticleCitation.referencesSameArticleAs(other: ArticleCitation): Boolean {
        if (articleNumber != other.articleNumber || subArticleNumber != other.subArticleNumber) {
            return false
        }
        if (paragraphNumber != null && other.paragraphNumber != null &&
            paragraphNumber != other.paragraphNumber
        ) {
            return false
        }
        if (itemNumber != null && other.itemNumber != null && itemNumber != other.itemNumber) {
            return false
        }

        val thisLaw = lawName?.replace(" ", "")
        val otherLaw = other.lawName?.replace(" ", "")
        return thisLaw == null || otherLaw == null || thisLaw == otherLaw
    }

    private fun ArticleCitation.hasCompatibleLawName(other: ArticleCitation): Boolean {
        val thisLaw = lawName?.replace(" ", "")
        val otherLaw = other.lawName?.replace(" ", "")
        return thisLaw == null || otherLaw == null || thisLaw == otherLaw
    }

    companion object {
        private const val DEFAULT_MINIMUM_CONTENT_OVERLAP = 0.35
        private const val DEFAULT_MINIMUM_CORRECTION_OVERLAP = 0.75
        private const val MINIMUM_CORRECTION_MARGIN = 0.10
        private const val MINIMUM_CORRECTION_KEYWORD_COUNT = 2
        private const val MINIMUM_KEYWORD_LENGTH = 2

        private val ARTICLE_PATTERN = Regex(
            pattern = "(?:([가-힣A-Za-z·ㆍ]{2,30}법)\\s*)?" +
                "제\\s*(\\d+)\\s*조" +
                "(?:\\s*의\\s*(\\d+))?" +
                "(?:\\s*제\\s*(\\d+)\\s*항)?" +
                "(?:\\s*제\\s*(\\d+)\\s*호)?",
        )
        private val SENTENCE_BOUNDARY_PATTERN = Regex("(?<=[.!?。！？])\\s+|[\\r\\n]+")
        private val NON_WORD_PATTERN = Regex("[^가-힣a-z0-9]+")
        private val STOP_WORDS = setOf(
            "따라", "대한", "경우", "해당", "이는", "있다", "있는", "하며", "한다",
            "됩니다", "입니다", "그리고", "또는", "에서", "으로", "에게", "까지", "관한",
        )
    }
}

data class ArticleCitation(
    val rawText: String,
    val lawName: String?,
    val articleNumber: Int,
    val subArticleNumber: Int?,
    val paragraphNumber: Int?,
    val itemNumber: Int?,
    val sentence: String,
)

data class ArticleValidationResult(
    val isValid: Boolean,
    val checkedCitations: List<ArticleCitation>,
    val issues: List<ArticleValidationIssue>,
)

data class ArticleCorrectionResult(
    val correctedAnswer: String,
    val corrections: List<ArticleCorrection>,
)

data class ArticleCorrection(
    val originalCitation: ArticleCitation,
    val correctedCitation: ArticleCitation,
    val contentOverlap: Double,
)

data class ArticleValidationIssue(
    val type: ArticleValidationIssueType,
    val citation: ArticleCitation,
    val reason: String,
    val groundingSentence: String? = null,
    val contentOverlap: Double? = null,
)

enum class ArticleValidationIssueType {
    ARTICLE_NUMBER_NOT_FOUND,
    ARTICLE_CONTENT_MISMATCH,
}
