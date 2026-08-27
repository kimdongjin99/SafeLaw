package com.example.safelaw.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitClient {
    // 📍 에뮬레이터에서 맥북 로컬 스프링 부트(8080)로 접속하는 약속된 주소
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val apiService: SafeLawApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(SafeLawApiService::class.java)
    }
}