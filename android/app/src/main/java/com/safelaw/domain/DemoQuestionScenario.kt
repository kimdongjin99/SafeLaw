package com.safelaw.domain

/** 시연 영상에서만 사용하는 고정 질문과 서버에서 확인한 대여금 판례 ID입니다. */
object DemoQuestionScenario {
    const val QUESTION =
        "친구에게 돈을 빌려줬는데 8개월동안 못 받고 있어. 계약서는 따로 작성하지 않았는데 돈을 돌려받을 수 있을까?"

    // 122146: 금전소비대차 입증, 121675: 채무 증거, 121362: 소멸시효 기산점
    val CASE_IDS = listOf(122146L, 121675L, 121362L)

    // 로컬 DB를 열지 않고도 섀도우 쿼리 형태를 유지하기 위한 고정 더미 ID입니다.
    val DUMMY_CASE_IDS = listOf(90001L, 999999L, 12345L, 67890L)
}
