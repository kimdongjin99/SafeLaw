package com.example.safelaw

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.objectbox.BoxStore
import com.example.safelaw.data.MyObjectBox
import com.example.safelaw.data.LawCase
// 📍 새로 추가될 네트워크 패키지들 (동진 님 패키지명에 맞게 조정)
import com.example.safelaw.network.RetrofitClient
import com.example.safelaw.network.LawCaseRequest

class MainActivity : AppCompatActivity() {

    private lateinit var store: BoxStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // 1. 기기에 저장된 9만개 디비 창고 열기
            store = MyObjectBox.builder()
                .androidContext(applicationContext)
                .build()

            val box = store.boxFor(LawCase::class.java)

            // 📊 상태 점검 로그
            Log.d("SafeLaw_DB", "🚀 디비 로드 완료! 현재 보관 중인 판례 개수: ${box.count()} 개")

            // 2. 📍 [3번 기능 테스트] 하드코딩 없이 디비에서 진짜 첫 번째 판례 ID 꺼내오기
            val firstCase = box.all.firstOrNull()
            if (firstCase != null) {
                // 진짜 살아있는 ID(예: 30180)를 인자로 넣어서 서버로 쏩니다!
                sendIdToServer(firstCase.id)
            } else {
                Log.e("SafeLaw_Network", "❌ 디비가 비어있어 통신 테스트를 진행할 수 없습니다.")
            }

        } catch (e: Exception) {
            Log.e("SafeLaw_DB", "디비 여는 중 에러 발생: ${e.message}")
        }
    }

    // 📡 3번: 서버로 ID 리스트를 전송하고 원문을 받아오는 전담 통신 함수
    private fun sendIdToServer(targetId: Long) {
        // 네트워크 통신은 메인 스레드를 방해하면 안 되므로 백그라운드(IO)에서 실행합니다.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("SafeLaw_Network", "📡 스프링 부트 서버로 판례 ID 전송 시작... ID: [$targetId]")
                val requestData = LawCaseRequest(ids = listOf(targetId))

                // Retrofit을 이용해 스프링 부트 API 호출 슛!
                val response = RetrofitClient.apiService.getLawCaseTexts(requestData)

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("SafeLaw_Network", "✅ 서버 응답 수신 성공! 받아온 데이터 개수: ${body?.size}개")
                    body?.forEach {
                        Log.d("SafeLaw_Network", "📄 [서버 원문] ID: ${it.id} / 제목: ${it.title}\n내용: ${it.content}")
                    }
                } else {
                    Log.e("SafeLaw_Network", "❌ 서버 에러 응답 발생 코드: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SafeLaw_Network", "❌ 서버 연결 실패 (서버가 꺼져있거나 주소가 다릅니다): ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        // 앱이 닫힐 때 안전하게 닫아주기
        if (::store.isInitialized && !store.isClosed) {
            store.close()
        }
        super.onDestroy()
    }
}