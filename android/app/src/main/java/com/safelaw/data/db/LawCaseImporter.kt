package com.safelaw.data.db

import android.content.Context
import android.util.JsonReader
import android.util.Log
import com.safelaw.data.db.data.LawCase
import io.objectbox.Box
import io.objectbox.BoxStore
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** assets 폴더의 판례 임베딩 JSON을 ObjectBox에 주입합니다. */
class LawCaseImporter(
    context: Context,
    private val store: BoxStore,
) {

    private val appContext = context.applicationContext
    private val lawCaseBox: Box<LawCase> = store.boxFor(LawCase::class.java)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 📥 [인덱싱 단계] 주입이 끝까지 완료된 적이 없으면 대용량 JSON을 스트리밍으로 읽어 고속 주입합니다.
     * 중간에 앱이 꺼져 일부만 들어간 경우에도 다시 주입합니다. (같은 ID는 덮어쓰기)
     * @return 주입 후 DB에 저장된 판례 개수
     */
    suspend fun importIfEmpty(onProgress: (String) -> Unit = {}): Long = withContext(Dispatchers.IO) {
        val currentCount = lawCaseBox.count()
        Log.d(TAG, " 디비 로드 완료! 현재 보관 중인 판례 개수: $currentCount 개")

        if (currentCount > 0L && prefs.getBoolean(KEY_IMPORT_COMPLETED, false)) {
            return@withContext currentCount
        }

        if (currentCount > 0L) {
            Log.d(TAG, " 이전 주입이 중간에 끊겼습니다. 처음부터 다시 주입합니다.")
        }
        Log.d(TAG, " JSON 데이터 주입 시작... (수 분 걸릴 수 있습니다)")
        val startTime = System.currentTimeMillis()
        var readCount = 0

        val inputStream = appContext.assets.open(JSON_FILE_NAME)
        JsonReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            val bufferList = mutableListOf<LawCase>()
            reader.beginArray()

            while (reader.hasNext()) {
                bufferList.add(readLawCase(reader))
                readCount++

                // 1,000개씩 묶어서 트랜잭션 고속 주입
                if (bufferList.size >= BATCH_SIZE) {
                    ensureActive()
                    store.runInTx { lawCaseBox.put(bufferList) }
                    bufferList.clear()
                }

                if (readCount % PROGRESS_LOG_INTERVAL == 0) {
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                    val message = " 주입 진행 중... ${readCount}개 완료 (${elapsedSec}초 경과)"
                    Log.d(TAG, message)
                    onProgress(message)
                }
            }
            reader.endArray()

            if (bufferList.isNotEmpty()) {
                ensureActive()
                store.runInTx { lawCaseBox.put(bufferList) }
            }
        }

        prefs.edit().putBoolean(KEY_IMPORT_COMPLETED, true).apply()

        val importedCount = lawCaseBox.count()
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
        Log.d(TAG, " JSON 데이터 주입 완료! 총 ${importedCount}개 (${elapsedSec}초 소요)")
        importedCount
    }

    private fun readLawCase(reader: JsonReader): LawCase {
        var id: Long = 0
        var embeddingArray: FloatArray? = null

        reader.beginObject()
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

        return LawCase().apply {
            this.id = id
            this.embedding = embeddingArray
        }
    }

    companion object {
        private const val TAG = "SafeLaw_DB"
        private const val JSON_FILE_NAME = "criminal_law_data.json"
        private const val BATCH_SIZE = 1000
        private const val PROGRESS_LOG_INTERVAL = 10_000
        private const val PREFS_NAME = "safelaw_db"
        private const val KEY_IMPORT_COMPLETED = "law_case_import_completed"
    }
}
