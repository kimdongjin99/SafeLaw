package com.safelaw.domain

/**
 * 같은 대화방의 이전 질문·답변 한 쌍.
 * 이어지는 질문("그럼 형량은?")의 맥락을 알 수 있도록 프롬프트와 판례 검색에 함께 넣는다.
 */
data class ChatTurn(
    val question: String,
    val answer: String,
)
