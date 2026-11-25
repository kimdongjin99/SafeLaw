package com.safelaw.domain.repository

import com.safelaw.domain.ChatTurn
import com.safelaw.domain.GroundingData

interface SlmRepository {

    suspend fun generateAnswer(
        question: String,
        groundingData: GroundingData,
        onToken: (String) -> Unit,
        // 같은 대화방의 최근 대화 (오래된 것 -> 최근 순). 비어 있으면 이번 질문만으로 답한다.
        history: List<ChatTurn> = emptyList(),
    ): String
}