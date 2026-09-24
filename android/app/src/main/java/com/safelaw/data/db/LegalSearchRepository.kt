package com.safelaw.data.db

import com.safelaw.domain.DbRepository

class LegalSearchRepository: DbRepository {
    override suspend fun vectorSearch(question: String): List<Long> {
        TODO("Not yet implemented")
    }

    override suspend fun findGroundingDataFromServer(caseIds: List<Long>): String {
        TODO("Not yet implemented")
    }

}