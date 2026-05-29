package com.example.safelaw

import android.app.Application

class SafelawApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 앱이 시작될 때 단 한 번만 ObjectBox를 초기화합니다.
        //ObjectBox.init(this)
    }
}