package com.example.safelaw.data

import android.content.Context
import android.util.Log
import io.objectbox.BoxStore
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object ObjectBox {
    lateinit var store: BoxStore


    fun init(context: Context) {
        try {
            // 1. 앱 내부 저장소의 objectbox 실제 데이터 폴더 경로 지정
            val contextFolder = context.getDir("objectbox", Context.MODE_PRIVATE)
            val dbFile = File(contextFolder, "data.mdb")

            // ⭐ [핵심 치트키] 712MB짜리 진짜 DB가 제대로 안착할 수 있도록,
            // 파일 용량이 100MB 이하(가짜 0바이트 껍데기 파일 등)이면 무조건 강제로 삭제하고 새로 복사합니다.
            if (!dbFile.exists() || dbFile.length() < 100 * 1024 * 1024L) {
                Log.d("SafeLaw", "📦 꼬여있던 구형 DB 찌꺼기를 강제 삭제하고 진짜 대용량 DB(712MB) 복사를 시작합니다...")

                dbFile.delete() // 👈 기존 꼬인 파일 강제 소각!

                val inputStream: InputStream = context.assets.open("data.mdb")
                val outputStream = FileOutputStream(dbFile)

                val buffer = ByteArray(8192)
                var length: Int
                while (inputStream.read(buffer).also { length = it } > 0) {
                    outputStream.write(buffer, 0, length)
                }

                outputStream.flush()
                outputStream.close()
                inputStream.close()
                Log.d("SafeLaw", "📦 진짜 DB 파일 복사 완료!")
            }

            // 2. 진짜 파이썬 데이터가 완벽하게 심어진 상태에서 디비 엔진을 깨웁니다.
            store = com.example.safelaw.data.MyObjectBox.builder()
                .androidContext(context.applicationContext)
                .directory(contextFolder)
                .build()

            // 3. 데이터 수량 체크용 로그
            val box = store.boxFor(LawCase::class.java)
            Log.d("SafeLaw", "✅ DB 초기화 성공! 현재 저장된 판례 개수: ${box.count()}개")

        } catch (e: Exception) {
            Log.e("SafeLaw", "❌ DB 초기화 중 에러 발생: ${e.message}")
            e.printStackTrace()
        }
    }
}