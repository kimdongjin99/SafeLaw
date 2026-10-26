package com.safelaw.domain

interface SlmRepository {

    suspend fun generateAnswer(
        question: String,
        groundingData: String,
    ): String
}