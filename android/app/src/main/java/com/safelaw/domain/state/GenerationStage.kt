package com.safelaw.domain.state;

enum class GenerationStage(
    val message: String,
) {
    EMBEDDING_QUESTION("질문을 분석하고 있습니다."),
    SEARCHING_PRECEDENTS("관련 판례를 검색하고 있습니다."),
    GENERATING_ANSWER("답변을 생성하고 있습니다."),
    POST_PROCESSING("답변을 정리하고 있습니다."),
    COMPLETED("답변 생성이 완료되었습니다."),
    FAILED("답변 생성 중 문제가 발생했습니다."),
}