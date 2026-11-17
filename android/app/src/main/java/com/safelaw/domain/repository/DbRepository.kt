package com.safelaw.domain.repository

import com.safelaw.domain.GroundingData
import com.safelaw.domain.LegalSearchHit

interface DbRepository {
    suspend fun vectorSearch(
        question: String,
    ): List<LegalSearchHit>

    suspend fun findGroundingDataFromServer(
        caseIds: List<Long>,
    ): GroundingData
}