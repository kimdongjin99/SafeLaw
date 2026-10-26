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
import io.objectbox.kotlin.query
import com.example.safelaw.data.MyObjectBox
import com.example.safelaw.data.LawCase
import com.example.safelaw.data.LawCase_
import com.example.safelaw.network.RetrofitClient
import com.example.safelaw.network.LawCaseRequest

class MainActivity : AppCompatActivity() {

    private lateinit var store: BoxStore
    private lateinit var myOnnxHelper: OnnxEmbeddingHelper

    //  최근 임베딩 결과 50개까지 기억
    private val embeddingCache = object : LinkedHashMap<String, FloatArray>(50, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FloatArray>?): Boolean {
            return size > 50
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        try {
            store = MyObjectBox.builder()
                .androidContext(applicationContext)
                .build()

            myOnnxHelper = OnnxEmbeddingHelper(this)

            val box: Box<LawCase> = store.boxFor(LawCase::class.java)
            val currentCount = box.count()
            Log.d("SafeLaw_DB", " 디비 로드 완료! 현재 보관 중인 판례 개수: $currentCount 개")

            if (currentCount == 0L) {
                importJsonDataToDb(box)
            } else {
                executeSearchAndServerSync(box, "친구가 돈을 안 갚아요")
            }

        } catch (e: Exception) {
            Log.e("SafeLaw_DB", " 초기화 중 에러 발생: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun importJsonDataToDb(box: Box<LawCase>) {
        kotlin.concurrent.thread {
            var reader: JsonReader? = null
            try {
                val assetManager = resources.assets
                val inputStream = assetManager.open("criminal_law_data.json")
                reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))

                val bufferList = mutableListOf<LawCase>()
                reader.beginArray()

                while (reader.hasNext()) {
                    reader.beginObject()
                    var id: Long = 0
                    var embeddingArray: FloatArray? = null

                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "id" -> id = reader.nextLong()
                            "embedding" -> {
                                val floatList = mutableListOf<Float>()
                                reader.beginArray()
                                while (reader.hasNext()) {
                                    floatList.add(reader.nextDouble().toFloat())
                                }
                                reader.endArray()
                                embeddingArray = floatList.toFloatArray()
                            }
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()

                    val lawCase = LawCase().apply {
                        this.id = id
                        this.embedding = embeddingArray
                    }
                    bufferList.add(lawCase)

                    if (bufferList.size >= 1000) {
                        store.runInTx { box.put(bufferList) }
                        bufferList.clear()
                    }
                }
                reader.endArray()

                if (bufferList.isNotEmpty()) {
                    store.runInTx { box.put(bufferList) }
                }

                runOnUiThread {
                    executeSearchAndServerSync(box, "친구가 돈을 안 갚아요")
                }

            } catch (e: Exception) {
                Log.e("SafeLaw_DB", " JSON 주입 에러: ${e.message}")
            } finally {
                try { reader?.close() } catch (_: Exception) {}
            }
        }
    }

    //  캐시를 확인하고 없으면 새로 계산한 뒤 저장하는 함수
    private fun getQueryEmbeddingWithCache(prompt: String): FloatArray {
        return embeddingCache.getOrPut(prompt) {
            Log.d("SafeLaw_Cache", " [캐시 X] ONNX 모델로 새로 임베딩 계산 중 -> \"$prompt\"")
            myOnnxHelper.embed(prompt)
        }.also {
            if (embeddingCache.containsKey(prompt)) {
                Log.d("SafeLaw_Cache", " [캐시 O] 메모리에서 임베딩 재사용 -> \"$prompt\"")
            }
        }
    }

    private fun executeSearchAndServerSync(box: Box<LawCase>, userPrompt: String) {
        try {
            //  직접 모델을 호출하는 대신 캐시 함수를 거쳐 임베딩 가져오기
            val queryEmbedding: FloatArray = getQueryEmbeddingWithCache(userPrompt)

            // 벡터 검색 수행
            val nearestCase = box.query(LawCase_.embedding.nearestNeighbors(queryEmbedding, 5))
                .build()
                .findFirst()

            if (nearestCase != null) {
                val targetId = nearestCase.id

                //  [핵심] 서버 통신과 관계없이, 폰 안에서 검색이 끝난 순간 무조건 먼저 로그를 찍습니다!
                Log.d("SafeLaw_Search", " [임베딩 검색 성공] 질문: \"$userPrompt\" -> 가장 유사한 판례 ID: $targetId")

                val allCases = box.all
                val allIds = mutableListOf<Long>()
                for (item in allCases) {
                    allIds.add(item.id)
                }

                val dummyIds = allIds
                    .filter { it != targetId }
                    .shuffled()
                    .take(10)

                val mixedIds = (listOf(targetId) + dummyIds).shuffled()

                // 서버 전송 시도
                sendIdsToServer(mixedIds, targetId)

            } else {
                Log.e("SafeLaw_Search", " 유사한 판례를 찾지 못했습니다.")
            }

        } catch (e: Exception) {
            Log.e("SafeLaw_Search", " 검색 중 에러: ${e.message}")
        }
    }

    private fun sendIdsToServer(requestIds: List<Long>, targetId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val requestData = LawCaseRequest(ids = requestIds)
                val response = RetrofitClient.apiService.getLawCaseTexts(requestData)

                if (response.isSuccessful) {
                    val body = response.body()
                    val trueCase = body?.find { it.id == targetId }
                    val dummyCases = body?.filter { it.id != targetId } ?: emptyList()

                    Log.d("SafeLaw_Network", " 가짜 판례 ${dummyCases.size}개 폐기 완료")
                    if (trueCase != null) {
                        Log.d("SafeLaw_Network", " 진짜 판례 수신 성공: ${trueCase.title}")
                    }
                }
            } catch (e: Exception) {
                Log.e("SafeLaw_Network", " 서버 통신 실패: ${e.message}")
            }
        }
    }

    override fun onDestroy() {
        if (::myOnnxHelper.isInitialized) {
            myOnnxHelper.close()
        }
        if (::store.isInitialized && !store.isClosed) {
            store.close()
        }
        super.onDestroy()
    }
}