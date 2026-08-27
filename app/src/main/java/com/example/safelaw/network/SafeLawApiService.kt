package com.example.safelaw.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SafeLawApiService {
    @POST("api/lawcases/text")
    suspend fun getLawCaseTexts(@Body request: LawCaseRequest): Response<List<LawCaseResponse>>
}