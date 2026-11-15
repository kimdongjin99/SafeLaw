package com.safelaw.domain

/** HNSW로 검색된 판례 ID와 질문 벡터로부터의 거리를 함께 보관합니다. */
data class LegalSearchHit(
    val caseId: Long,
    val distance: Double,
)
