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
                try { reader?.close() } catch (_: Exception) {}
            }
        }
    }

    /**
     * 🔍 [검색 및 섀도우 쿼리 단계] 진짜 판례 ID와 가짜(더미) ID들을 섞어서 서버 전송 준비
     */
    private fun executeSearchAndServerSync(box: Box<LawCase>) {
        val firstCase = box.query().build().findFirst()

        if (firstCase != null) {
            val targetId = firstCase.id

            // LawCase_ 의존성 없이 순수 리스트로 ID 추출
            val allIds = box.all.map { it.id }
            val dummyIds = allIds
                .filter { it != targetId }
                .shuffled()
                .take(3) // 가짜 ID 3개 선정

            // 진짜 ID와 가짜 ID를 합친 후 무작위로 섞음
            val mixedIds = (listOf(targetId) + dummyIds).shuffled()

            Log.d("SafeLaw_Network", "🎯 [섀도우 쿼리 적용] 진짜 ID: [$targetId] / 전송할 ID 리스트: $mixedIds")
            sendIdsToServer(mixedIds, targetId)

        } else {
            Log.e("SafeLaw_Network", "❌ 디비에 데이터가 존재하지 않습니다.")
        }
    }

    /**
     * 📡 스프링 부트 서버로 섞인 ID 리스트 전송 및 진짜 원문만 남기고 가짜는 버리기
     */
    private fun sendIdsToServer(requestIds: List<Long>, targetId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d("SafeLaw_Network", "📡 스프링 부트 서버로 블라인드 패킷 전송 시작... 요청 ID 목록: $requestIds")
                val requestData = LawCaseRequest(ids = requestIds)

                val response = RetrofitClient.apiService.getLawCaseTexts(requestData)

                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("SafeLaw_Network", "✅ 서버 응답 수신 성공! 받아온 데이터 총 개수: ${body?.size}개 (더미 포함)")

                    // 1. 진짜 판례만 쏙 골라내기
                    val trueCase = body?.find { it.id == targetId }

                    // 2. 가짜(더미)로 딸려온 판례들은 화면에 노출하지 않고 폐기
                    val dummyCases = body?.filter { it.id != targetId } ?: emptyList()
                    Log.d("SafeLaw_Network", "🗑️ [보안 처리] 함께 수신된 가짜 판례 ${dummyCases.size}개는 메모리에서 안전하게 폐기합니다.")

                    if (trueCase != null) {
                        Log.d("SafeLaw_Network", "📄 [최종 유효 원문] ID: ${trueCase.id} / 제목: ${trueCase.title}\n내용: ${trueCase.content}")
                    } else {
                        Log.e("SafeLaw_Network", "❌ 응답 데이터 중에서 진짜 타겟 ID를 찾지 못했습니다.")
                    }
                } else {
                    Log.e("SafeLaw_Network", "❌ 서버 에러 응답 발생 코드: ${response.code()}")
                }
            } catch (e: Exception) {
                Log.e("SafeLaw_Network", "❌ 서버 연결 실패: ${e.message}")
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