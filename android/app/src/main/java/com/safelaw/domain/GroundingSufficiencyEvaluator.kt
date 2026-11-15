package com.safelaw.domain

/*
 * 임시 폐기:
 * HNSW 검색 거리를 임계값과 비교해 근거 부족으로 판정하고 답변을 대치하는 정책은
 * 거리 분포와 적절한 임계값이 확정될 때까지 사용하지 않습니다.
 *
 * 이 파일의 로직은 원래도 호출부에 연결되지 않았으며, 재검토하기 쉽도록 삭제하지 않고
 * 주석으로 보존합니다. 판례 검색 자체에 필요한 질문 임베딩과 HNSW 검색은 계속 사용합니다.
 *
/** HNSW 검색 거리를 기준으로 SLM에 전달할 근거가 충분한지 판정합니다. */
class GroundingSufficiencyEvaluator {

    fun evaluate(
        searchHits: List<LegalSearchHit>,
        maximumDistance: Double,
        minimumRelevantCases: Int = 1,
    ): GroundingSufficiencyResult {
        require(maximumDistance >= 0.0) { "maximumDistance must not be negative" }
        require(minimumRelevantCases > 0) { "minimumRelevantCases must be positive" }

        if (searchHits.isEmpty()) {
            return GroundingSufficiencyResult(
                isSufficient = false,
                reason = GroundingInsufficiencyReason.NO_SEARCH_RESULT,
                relevantCaseCount = 0,
                nearestDistance = null,
            )
        }

        val relevantCaseCount = searchHits.count { it.distance <= maximumDistance }
        val nearestDistance = searchHits.minOf { it.distance }
        val isSufficient = relevantCaseCount >= minimumRelevantCases

        return GroundingSufficiencyResult(
            isSufficient = isSufficient,
            reason = if (isSufficient) null else GroundingInsufficiencyReason.RESULTS_TOO_FAR,
            relevantCaseCount = relevantCaseCount,
            nearestDistance = nearestDistance,
        )
    }
}

data class GroundingSufficiencyResult(
    val isSufficient: Boolean,
    val reason: GroundingInsufficiencyReason?,
    val relevantCaseCount: Int,
    val nearestDistance: Double?,
)

enum class GroundingInsufficiencyReason {
    NO_SEARCH_RESULT,
    RESULTS_TOO_FAR,
}
*/
