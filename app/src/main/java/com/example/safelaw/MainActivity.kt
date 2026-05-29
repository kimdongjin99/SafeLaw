package com.example.safelaw

import android.os.Bundle
import android.util.JsonReader
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStreamReader
import kotlin.concurrent.thread
import io.objectbox.Box
import io.objectbox.BoxStore
import com.example.safelaw.data.MyObjectBox // 📍 자동 생성된 MyObjectBox 임포트 체크!
import com.example.safelaw.data.LawCase

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 🚀 [치트키 백그라운드 스레드] 스트리밍 방식으로 메모리 방어하며 디비 주입 가동!
        thread {
            var store: BoxStore? = null
            var reader: JsonReader? = null
            try {
                Log.d("SafeLaw_DB", "⚙️ [시작] 가상 기기 내부에서 스트리밍 디비 주입을 시작합니다...")

                // 📍 [치트키 패치] data.mdb 복사 로직을 우회하여, 생짜로 기기 내부에 빈 디비 창고를 직접 빌드합니다.
                val context = applicationContext
                store = MyObjectBox.builder()
                    .androidContext(context)
                    .build()

                val box: Box<LawCase> = store.boxFor(LawCase::class.java)

                // 기존에 혹시 들어가 있을지 모를 찌꺼기 데이터 청소 (깔끔하게 새로 시작)
                box.removeAll()

                // 2. assets 폴더에서 대용량 json 파이프라인(Stream) 열기
                val assetManager = resources.assets
                val inputStream = assetManager.open("criminal_law_data.json")
                reader = JsonReader(InputStreamReader(inputStream, "UTF-8"))

                val startTime = System.currentTimeMillis()

                // 대량 주입 시 메모리 방어와 속도 향상을 위해 1000개씩 묶어서 처리할 버퍼 리스트
                val bufferList = mutableListOf<LawCase>()
                var totalCount = 0

                reader.beginArray() // 거대한 JSON 배열 시작 상자 열기 [

                while (reader.hasNext()) {
                    reader.beginObject() // 판례 한 개 상자 열기 {

                    var id: Long = 0
                    var embeddingArray: FloatArray? = null

                    // { } 내부의 키-값 쌍들을 하나씩 조사
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "id" -> id = reader.nextLong()
                            "embedding" -> {
                                val floatList = mutableListOf<Float>()
                                reader.beginArray() // embedding 내부의 배열 열기 [
                                while (reader.hasNext()) {
                                    floatList.add(reader.nextDouble().toFloat())
                                }
                                reader.endArray() // ]
                                embeddingArray = floatList.toFloatArray()
                            }
                            else -> reader.skipValue() // 혹시 다른 필드가 들어있다면 안전하게 패스
                        }
                    }
                    reader.endObject() // } 판례 한 개 상자 닫기

                    // 3. 추출한 id와 embedding으로 LawCase 객체 생성
                    val lawCase = LawCase().apply {
                        this.id = id
                        this.embedding = embeddingArray
                    }
                    bufferList.add(lawCase)
                    totalCount++

                    // 4. 1,000개가 쌓일 때마다 트랜잭션을 걸어서 디비에 고속 주입
                    if (bufferList.size >= 1000) {
                        store.runInTx {
                            box.put(bufferList)
                        }
                        bufferList.clear()
                        Log.d("SafeLaw_DB", "⏳ 현재 디비 주입 중... 완료된 개수: $totalCount 개")
                    }
                }

                reader.endArray() // 거대한 JSON 배열 끝 ]

                // 5. 1000개로 나누고 남은 찌꺼기 데이터 최종 주입
                if (bufferList.isNotEmpty()) {
                    store.runInTx {
                        box.put(bufferList)
                    }
                }

                val endTime = System.currentTimeMillis()

                Log.d("SafeLaw_DB", "✅ 데이터베이스 구축 완벽 성공!!! (소요시간: ${(endTime - startTime)/1000.0}초)")
                Log.d("SafeLaw_DB", "📊 현재 디비에 최종 저장된 판례 개수: ${box.count()} 개")

            } catch (e: Exception) {
                Log.e("SafeLaw_DB", "❌ 주입 중 치명적 에러 발생: ${e.message}")
                e.printStackTrace()
            } finally {
                try { reader?.close() } catch (ex: Exception) {}
                // 📍 주입이 끝나면 파일로 완전히 쓰기(Flush)되도록 스토어를 닫아줍니다.
                try { store?.close() } catch (ex: Exception) {}
                Log.d("SafeLaw_DB", "🔒 디비 파일 작성이 완료되어 스토어를 안전하게 닫았습니다.")
            }
        }
    }
}