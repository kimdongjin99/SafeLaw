package com.safelaw.data.db

/**
 * 최근 질문들의 검색 결과(판례 ID)와 서버에서 받은 판례 원문을 기억합니다.
 * 새 질문이 이전 질문과 자카드 유사도 기준 이상으로 비슷하면 이전 결과를 재사용합니다.
 */
class SearchResultCache(
    private val maxSize: Int = DEFAULT_MAX_SIZE,
    private val similarityThreshold: Double = DEFAULT_SIMILARITY_THRESHOLD,
) {

    data class SimilarQuestionHit(
        val cachedQuestion: String,
        val similarity: Double,
        val caseIds: List<Long>,
    )

    private class CachedSearch(
        val bigrams: Set<String>,
        val caseIds: List<Long>,
    )

    // 질문 → 검색 결과(판례 ID)
    private val searchCache = lruMap<String, CachedSearch>()

    // 판례 ID 목록 → 서버에서 받은 판례 원문
    private val groundingCache = lruMap<List<Long>, String>()

    /** 캐시된 질문 중 가장 비슷한 질문을 찾고, 기준 이상이면 그 검색 결과를 반환합니다. */
    @Synchronized
    fun findSimilarQuestion(question: String): SimilarQuestionHit? {
        val bigrams = toBigrams(question)

        val best = searchCache.entries
            .map { (cachedQuestion, cached) -> Triple(cachedQuestion, cached, jaccard(bigrams, cached.bigrams)) }
            .maxByOrNull { it.third }
            ?: return null

        val (cachedQuestion, cached, similarity) = best
        if (similarity < similarityThreshold) {
            return null
        }

        // 최근에 쓴 항목으로 순서 갱신
        searchCache[cachedQuestion]
        return SimilarQuestionHit(cachedQuestion, similarity, cached.caseIds)
    }

    @Synchronized
    fun putSearchResult(question: String, caseIds: List<Long>) {
        searchCache[question] = CachedSearch(toBigrams(question), caseIds)
    }

    @Synchronized
    fun findGroundingData(caseIds: List<Long>): String? = groundingCache[caseIds]

    @Synchronized
    fun putGroundingData(caseIds: List<Long>, groundingData: String) {
        groundingCache[caseIds] = groundingData
    }

    private fun <K, V> lruMap(): LinkedHashMap<K, V> =
        object : LinkedHashMap<K, V>(maxSize, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>?): Boolean {
                return size > maxSize
            }
        }

    companion object {
        const val DEFAULT_MAX_SIZE = 50
        const val DEFAULT_SIMILARITY_THRESHOLD = 0.7

        /**
         * 한국어는 조사·어미 때문에 띄어쓰기 단위 비교가 부정확하므로,
         * 공백과 문장부호를 지운 뒤 글자 2개씩 묶은 집합(bigram)으로 비교합니다.
         */
        fun toBigrams(text: String): Set<String> {
            val normalized = text.lowercase().filter { it.isLetterOrDigit() }
            if (normalized.length < 2) {
                return if (normalized.isEmpty()) emptySet() else setOf(normalized)
            }
            return normalized.windowed(size = 2).toSet()
        }

        /** 자카드 유사도 = 교집합 크기 / 합집합 크기 (0.0 ~ 1.0) */
        fun jaccard(a: Set<String>, b: Set<String>): Double {
            if (a.isEmpty() || b.isEmpty()) {
                return 0.0
            }
            val intersection = a.count { it in b }
            val union = a.size + b.size - intersection
            return intersection.toDouble() / union
        }
    }
}
