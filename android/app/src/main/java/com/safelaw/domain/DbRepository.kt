package com.safelaw.domain

interface DbRepository {
    suspend fun vectorSearch(
        question: String,
    ): List<Long>

    suspend fun findGroundingDataFromServer(
        caseIds: List<Long>,
    ): String
}