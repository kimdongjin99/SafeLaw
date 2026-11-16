package com.safelaw.domain

class AskLegalQuestionUseCase(
    private val db: DbRepository,
    private val slm: SlmRepository,
){

    suspend fun getGroundingData(question: String): String {
        // 시연용: 로컬 ONNX 임베딩 및 ObjectBox/HNSW 검색을 실행하지 않습니다.
        // val searchHits = db.vectorSearch(question)
        // val caseIds = searchHits.map { it.caseId }
        return db.findGroundingDataFromServer(DemoQuestionScenario.CASE_IDS).precedents
    }

    suspend fun ask(
        question: String,
        onToken: (String) -> Unit = {},
    ): String {
        // 시연용: 로컬 DB 관련 구현은 메모리 사용을 피하기 위해 잠시 비활성화합니다.
        // val searchHits = db.vectorSearch(question)
        // val groundingData = db.findGroundingDataFromServer(searchHits.map { it.caseId })
        val groundingData = db.findGroundingDataFromServer(DemoQuestionScenario.CASE_IDS)


        // 질문과 grounding 데이터를 기반으로 모델 추론
        return slm.generateAnswer(
            question = question,
            groundingData = groundingData,
            onToken = onToken,
        )
    }
}
