package com.safelaw.domain

interface DbRepository {
    suspend fun vectorSearch(
        question: String,
    ): List<LegalSearchHit>

    suspend fun findGroundingDataFromServer(
        caseIds: List<Long>,
    ): GroundingData
}
