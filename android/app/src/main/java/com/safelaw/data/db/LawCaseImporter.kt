package com.safelaw.data.db

import android.content.Context
import android.util.Log
import com.safelaw.data.db.data.LawCase
import io.objectbox.Box
import io.objectbox.BoxStore
import java.io.DataInputStream
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * assets 폴더의 판례 임베딩 바이너리 파일을 ObjectBox에 주입합니다.
 *
 * 파일 형식 (Colab_code/krsbert_embedding.ipynb가 만듦, 모두 little-endian)
 *  - 헤더 16바이트: "SLAW" | 형식 버전(int32) | 판례 수(int32) | 차원(int32)
 *  - 판례마다: id(int64) + 임베딩(float32 × 차원)
 * JSON보다 파일이 훨씬 작고, 숫자를 글자에서 다시 계산하지 않아 주입이 빠릅니다.
 */
class LawCaseImporter(
    context: Context,
    private val store: BoxStore,
) {

    private val appContext = context.applicationContext
    private val lawCaseBox: Box<LawCase> = store.boxFor(LawCase::class.java)
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * 📥 [인덱싱 단계] 지금 데이터셋 버전으로 주입이 끝난 적이 없으면 DB를 비우고 처음부터 주입합니다.
     * 중간에 앱이 꺼진 경우, 예전 모델(384차원) 데이터가 남아 있는 경우 모두 여기서 새로 채워집니다.
     * @return 주입 후 DB에 저장된 판례 개수
     */
    suspend fun importIfEmpty(onProgress: (String) -> Unit = {}): Long = withContext(Dispatchers.IO) {
        val currentCount = lawCaseBox.count()
        Log.d(TAG, " 디비 로드 완료! 현재 보관 중인 판례 개수: $currentCount 개")

        if (currentCount > 0L && prefs.getInt(KEY_IMPORTED_VERSION, 0) == DATASET_VERSION) {
            return@withContext currentCount
        }

        if (currentCount > 0L) {
            Log.d(TAG, " 이전 데이터가 끊겼거나 예전 버전입니다. 비우고 다시 주입합니다.")
            lawCaseBox.removeAll()
        }
        Log.d(TAG, " 판례 임베딩 주입 시작... (수 분 걸릴 수 있습니다)")
        val startTime = System.currentTimeMillis()

        appContext.assets.open(DATA_FILE_NAME).use { input ->
            readAll(input) { batch, readCount ->
                ensureActive()
                store.runInTx { lawCaseBox.put(batch) }

                if (readCount % PROGRESS_LOG_INTERVAL == 0) {
                    val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
                    val message = " 주입 진행 중... ${readCount}개 완료 (${elapsedSec}초 경과)"
                    Log.d(TAG, message)
                    onProgress(message)
                }
            }
        }

        prefs.edit().putInt(KEY_IMPORTED_VERSION, DATASET_VERSION).apply()

        val importedCount = lawCaseBox.count()
        val elapsedSec = (System.currentTimeMillis() - startTime) / 1000
        Log.d(TAG, " 판례 임베딩 주입 완료! 총 ${importedCount}개 (${elapsedSec}초 소요)")
        importedCount
    }

    companion object {
        private const val TAG = "SafeLaw_DB"
        private const val DATA_FILE_NAME = "law_cases_krsbert.bin"
        private const val BATCH_SIZE = 1000
        private const val PROGRESS_LOG_INTERVAL = 10_000
        private const val PREFS_NAME = "safelaw_db"
        private const val KEY_IMPORTED_VERSION = "law_case_imported_version"

        // 판례 데이터나 임베딩 모델을 바꾸면 1 올립니다. 기존 DB를 지우고 다시 주입하게 됩니다.
        // 1 = all-MiniLM 384차원 JSON(9만 건), 2 = KR-SBERT 768차원 바이너리
        const val DATASET_VERSION = 2

        const val EMBEDDING_DIMENSION = 768
        private const val MAGIC = 0x57414C53 // "SLAW" (little-endian)
        private const val FORMAT_VERSION = 1
        private const val HEADER_BYTES = 16

        /**
         * 바이너리를 BATCH_SIZE개씩 읽어 onBatch에 넘깁니다. (안드로이드 없이 단위 테스트할 수 있게 분리)
         * @return 읽은 판례 수
         */
        fun readAll(input: InputStream, onBatch: (List<LawCase>, Int) -> Unit): Int {
            val data = DataInputStream(input.buffered(1 shl 16))

            val header = ByteArray(HEADER_BYTES).also { data.readFully(it) }
            val headerBuffer = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            require(headerBuffer.int == MAGIC) { "판례 임베딩 파일 형식이 아닙니다." }
            require(headerBuffer.int == FORMAT_VERSION) { "지원하지 않는 파일 형식 버전입니다." }
            val totalCount = headerBuffer.int
            val dimension = headerBuffer.int
            require(dimension == EMBEDDING_DIMENSION) {
                "임베딩 차원이 맞지 않습니다. 파일: $dimension, 앱: $EMBEDDING_DIMENSION (모델을 확인하세요)"
            }

            val recordBytes = Long.SIZE_BYTES + dimension * Float.SIZE_BYTES
            val chunk = ByteArray(recordBytes * BATCH_SIZE)
            var readCount = 0

            while (readCount < totalCount) {
                val n = minOf(BATCH_SIZE, totalCount - readCount)
                data.readFully(chunk, 0, n * recordBytes)
                val buffer = ByteBuffer.wrap(chunk, 0, n * recordBytes).order(ByteOrder.LITTLE_ENDIAN)

                val batch = List(n) {
                    LawCase().apply {
                        id = buffer.long
                        embedding = FloatArray(dimension).also { buffer.asFloatBuffer().get(it) }
                        buffer.position(buffer.position() + dimension * Float.SIZE_BYTES)
                    }
                }
                readCount += n
                onBatch(batch, readCount)
            }
            return readCount
        }
    }
}
