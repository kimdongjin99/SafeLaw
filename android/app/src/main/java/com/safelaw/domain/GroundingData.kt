package com.safelaw.domain

/** SLM 프롬프트용 판례 원문과 최종 출력용 판례 메타데이터를 함께 보관합니다. */
data class GroundingData(
    val precedents: String,
    val precedentReferences: List<PrecedentReference>,
) {
    companion object {
        val EMPTY = GroundingData(
            precedents = "",
            precedentReferences = emptyList(),
        )
    }
}

data class PrecedentReference(
    val id: Long,
    val caseName: String?,
    val caseNumber: String?,
    val courtName: String?,
)
