package com.safelaw.data.db.network

data class LawCaseRequest(val ids: List<Long>)

/**
 * 서버 LawTextDto와 같은 필드입니다.
 * 사건 번호·법원은 비어 있는 판례가 많아(각각 약 3만 9천 건, 5만 5천 건) null을 허용합니다.
 */
data class LawCaseResponse(
    val id: Long,
    val caseName: String?,
    val caseNum: String?,
    val courtName: String?,
    val content: String?,
)
