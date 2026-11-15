package com.safelaw.data.db.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SafeLawApiService {
    // 서버 LawSearchController: POST /api/v1/laws/texts (번호 1~30개)
    @POST("api/v1/laws/texts")
    suspend fun getLawCaseTexts(@Body request: LawCaseRequest): Response<List<LawCaseResponse>>
}