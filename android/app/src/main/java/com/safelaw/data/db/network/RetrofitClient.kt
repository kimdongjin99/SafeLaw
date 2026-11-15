package com.safelaw.data.db.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 배포된 SafeLaw Spring Boot 서버 주소
    private const val BASE_URL = "http://52.78.89.29:8080/"

    val apiService: SafeLawApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SafeLawApiService::class.java)
    }
}
