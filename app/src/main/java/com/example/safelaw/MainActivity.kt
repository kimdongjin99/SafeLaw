package com.example.safelaw

import android.os.Bundle
import android.util.JsonReader
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStreamReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import io.objectbox.Box
import io.objectbox.BoxStore
import com.example.safelaw.data.MyObjectBox
import com.example.safelaw.data.LawCase
import com.example.safelaw.network.RetrofitClient
import com.example.safelaw.network.LawCaseRequest

class MainActivity : AppCompatActivity() {

    private lateinit var store: BoxStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            // 1. 기기 내부의 ObjectBox 창고 오픈
            store = MyObjectBox.builder()
                .androidContext(applicationContext)
                .build()

            val box: Box<LawCase> = store.boxFor(LawCase::class.java)
            val currentCount = box.count()
            Log.d("SafeLaw_DB", "🚀 디비 로드 완료! 현재 보관 중인 판례 개수: $currentCount 개")

            // 2. 만약 디비가 비어있다면 최초 1회 JSON 데이터를 스트리밍으로 고속 주입
            if (currentCount == 0L) {
                importJsonDataToDb(box)
            } else {
                // 이미 주입되어 있다면 곧바로 검색 및 서버 통신 테스트 진행
                executeSearchAndServerSync(box)
            }

        } catch (e: Exception) {
            Log.e("SafeLaw_DB", "❌ 초기화 중 에러 발생: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 📥 [인덱싱 단계] assets 폴더의 대용량 JSON을 읽어와 ObjectBox에 고속 주입하는 함수
     */
    private fun importJsonDataToDb(box: Box<LawCase>) {
        // 대용량 주입은 메인 스레드를 멈추지 않도록 백그라운드 스레드에서 실행
        kotlin.concurrent.thread {
            var reader: JsonReader? = null
            try {
                Log.d("SafeLaw_DB", "⚙️ [시작] 디비가 비어있어 JSON 스트리밍 주입을 시작합니다...")

                val assetManager = resources.assets
                val inputStream = assetManager.open("criminal_law_data.json")
                reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))

                val startTime = System.currentTimeMillis()
                val bufferList = mutableListOf<LawCase>()
                var totalCount = 0

                reader.beginArray() // [

                while (reader.hasNext()) {
                    reader.beginObject() // {

                    var id: Long = 0
                    var embeddingArray: FloatArray? = null

                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "id" -> id = reader.nextLong()
                            "embedding" -> {
                                val floatList = mutableListOf<Float>()
                                reader.beginArray() // [
                                while (reader.hasNext()) {
                                    floatList.add(reader.nextDouble().toFloat())
                                }
                                reader.endArray() // ]
                                embeddingArray = floatList.toFloatArray()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject() // }

                    val lawCase = LawCase().apply {
                        this.id = id
                        this.embedding = embeddingArray
                    }
                    bufferList.add(lawCase)
                    totalCount++

                    // 1,000개씩 묶어서 트랜잭션 고속 주입
                    if (bufferList.size >= 1000) {
                        store.runInTx {
                            box.put(bufferList)
                        }
                        bufferList.clear()
                    }
                }
                reader.endArray() // ]

                // 남은 잔여 데이터 처리
                if (bufferList.isNotEmpty()) {
                    store.runInTx {
                        box.put(bufferList)
                    }
                }

                val endTime = System.currentTimeMillis()
                Log.d("SafeLaw_DB", "✅ 데이터베이스 구축 완벽 성공! (소요시간: ${(endTime - startTime)/1000.0}초, 총 ${box.count()}개)")

                // 주입이 끝났으므로 서버 통신 단계로 진입
                runOnUiThread {
                    executeSearchAndServerSync(box)
                }

            } catch (e: Exception) {
                Log.e("SafeLaw_DB", "❌ JSON 주입 중 치명적 에러 발생: ${e.message}")
                e.printStackTrace()
            } finally {
                try { reader?.close() } catch (ex: Exception) {}
            }
        }
    }

    /**
     * 🔍 [검색 및 통신 단계] 온디바이스에서 법률 ID를 추출해 서버로 전송하는 함수
     */
    private fun executeSearchAndServerSync(box: Box<LawCase>) {
        val firstCase = box.all.firstOrNull()
        if (firstCase != null) {
            Log.d("SafeLaw_Network", "🎯 추출된 판례 ID: ${firstCase.id} -> 서버로 전송을 시도합니다.")
            sendIdToServer(firstCase.id)
        } else {
            Log.e("SafeLaw_Network", "❌ 디비에 데이터가 존재하지 않습니다.")
        }
    }

    // 📡 스프링 부트 서버로 판례 ID 전송 및 원문 수신
    private fun sendIdToServer(targetId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("SafeLaw_Network", "📡 스프링 부트 서버로 판례 ID 전송 시작... ID: [$targetId]")
                val requestData = LawCaseRequest(ids = listOf(targetId))

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
                Log.e("SafeLaw_Network", "❌ 서버 연결 실패 (서버 주소나 네트워크 상태를 확인하세요): ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        if (::store.isInitialized && !store.isClosed) {
            store.close()
        }
        super.onDestroy()
    }
}