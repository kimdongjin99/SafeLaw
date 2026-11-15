package com.safelaw.domain

interface SlmRepository {

    suspend fun generateAnswer(
        question: String,
        groundingData: GroundingData,
        onToken: (String) -> Unit,
    ): String
}
