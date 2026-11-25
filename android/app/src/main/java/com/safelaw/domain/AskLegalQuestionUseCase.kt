package com.safelaw.domain

import com.safelaw.domain.repository.DbRepository
import com.safelaw.domain.repository.SlmRepository

class AskLegalQuestionUseCase(
    private val db: DbRepository,
    private val slm: SlmRepository,
){

    suspend fun getGroundingData(question: String): String {
        val searchHits = db.vectorSearch(question)
        return db.findGroundingDataFromServer(searchHits.map { it.caseId }).precedents
    }

    suspend fun ask(
        question: String,
        history: List<ChatTurn> = emptyList(),
        onToken: (String) -> Unit = {},
    ): String {
        // "그럼 형량은?"처럼 앞 질문에 기대는 질문도 맞는 판례를 찾도록, 직전 질문을 붙여서 검색한다.
        val searchQuery = history.lastOrNull()
            ?.let { "${it.question.take(SEARCH_CONTEXT_CHARS)} $question" }
            ?: question

        // DB에서 벡터 검색 후 서버에서 grounding 데이터 조회
        val searchHits = db.vectorSearch(searchQuery)
        val groundingData = db.findGroundingDataFromServer(searchHits.map { it.caseId })


        // 질문과 grounding 데이터, 최근 대화를 기반으로 모델 추론
        return slm.generateAnswer(
            question = question,
            groundingData = groundingData,
            onToken = onToken,
            history = history,
        )
    }

    companion object {
        // 검색어에 붙일 직전 질문 길이 (너무 길면 지금 질문의 뜻이 묻힘)
        private const val SEARCH_CONTEXT_CHARS = 100
    }
}
