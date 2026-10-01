package com.safelaw.domain

import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.annotations.ReactModule
import com.safelaw.data.slm.LlamaModule
import com.safelaw.data.slm.TextGenerationRepository

@ReactModule(name = LlamaModule.NAME)
class SafeLawChatDomain(
    reactContext: ReactApplicationContext,
    private val db: DbRepository,
    private val slm: SlmRepository,
): ReactContextBaseJavaModule(reactContext) {

    suspend fun ask(question: String): String {
        // DB에서 벡터 검색 후 서버에서 grounding 데이터 조회
        //val searchResult = db.vectorSearch(question)
        //val groundingData = db.findGroundingDataFromServer(searchResult)

        // 질문과 grounding 데이터를 기반으로 모델 추론
        return slm.generateAnswer(
            question = question,
            groundingData = "",
        )
    }
}