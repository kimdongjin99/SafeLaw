package com.example.safelaw

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.safelaw.data.ObjectBox
import com.example.safelaw.data.LawCase
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.objectbox.Box
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.util.Log

@RunWith(AndroidJUnit4::class)
class DbBuilderTest {

    @Test
    fun makeVectorDb() {
        // 1. 에뮬레이터(androidTest) 환경에 맞게 순수한 새 디비 공장(store)을 강제로 생성합니다.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        ObjectBox.store = com.example.safelaw.data.MyObjectBox.builder()
            .androidContext(appContext)
            .build()

        Log.d("SafeLaw_DB", "🚀 ObjectBox 스토어 초기화 완료! 데이터 주입을 시작합니다.")

        // 2. 맥북 로컬에 있는 criminal_law_data.json 파일 로드
        val jsonFilePath = "/Users/xkll1360/Downloads/criminal_law_data.json"
        val jsonFile = File(jsonFilePath)

        if (!jsonFile.exists()) {
            Log.e("SafeLaw_DB", "❌ 지정된 경로에 JSON 파일이 없습니다: $jsonFilePath")
            return
        }

        val jsonString = jsonFile.readText()

        // 3. Gson을 이용하여 JSON 데이터를 LawCase 리스트 객체로 파싱
        val gson = Gson()
        val listType = object : TypeToken<List<LawCase>>() {}.type
        val lawCaseList: List<LawCase> = gson.fromJson(jsonString, listType)

        Log.d("SafeLaw_DB", "📦 총 ${lawCaseList.size}개의 판례 데이터를 파싱했습니다. 디비에 주입합니다...")

        // 4. ObjectBox Box를 가져와서 리스트 한 방에 밀어넣기
        val lawCaseBox: Box<LawCase> = ObjectBox.store.boxFor(LawCase::class.java)

        // 대량 주입 시 속도 최적화를 위해 runInTx(트랜잭션) 처리
        ObjectBox.store.runInTx {
            lawCaseBox.removeAll() // 기존에 꼬여서 들어갔을 수 있는 데이터 초기화
            lawCaseBox.put(lawCaseList)
        }

        // 5. 주입 완료 검증 검사
        val finalCount = lawCaseBox.count()
        Log.d("SafeLaw_DB", "✅ 데이터베이스 구축 완벽 성공!!!")
        Log.d("SafeLaw_DB", "📊 현재 디비에 최종 저장된 판례 개수: $finalCount 개")

        // 디비 정상 종료
        ObjectBox.store.close()
    }
}