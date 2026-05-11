package com.example.safelaw.data

import android.content.Context
import io.objectbox.BoxStore

object ObjectBox {
    lateinit var store: BoxStore
        private set

    fun init(context: Context) {
        // MyObjectBox가 data 패키지 안에 생성되었다면 아래 경로가 맞습니다.
        store = com.example.safelaw.data.MyObjectBox.builder()
            .androidContext(context.applicationContext)
            .build()
    }
}