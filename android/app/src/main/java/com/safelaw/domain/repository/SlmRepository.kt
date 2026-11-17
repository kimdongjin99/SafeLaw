package com.safelaw.domain.repository

import com.safelaw.domain.GroundingData

interface SlmRepository {

    suspend fun generateAnswer(
        question: String,
        groundingData: GroundingData,
        onToken: (String) -> Unit,
    ): String
}