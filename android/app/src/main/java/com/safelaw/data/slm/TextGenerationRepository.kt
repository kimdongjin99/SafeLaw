package com.safelaw.data.slm

import com.safelaw.domain.SlmRepository

class TextGenerationRepository : SlmRepository{
    override suspend fun generateAnswer(
        question: String,
        groundingData: String
    ): String {
        TODO("Not yet implemented")
    }


}