package com.safelaw.data.db

import android.content.Context
import android.util.Log
import com.safelaw.data.db.data.LawCase
import com.safelaw.data.db.data.LawCase_
import com.safelaw.data.db.data.MyObjectBox
import com.safelaw.data.db.network.LawCaseRequest
import com.safelaw.data.db.network.RetrofitClient
import com.safelaw.data.db.network.SafeLawApiService
import com.safelaw.domain.DbRepository
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LegalSearchRepository(
    context: Context,
    // 넘기지 않으면 첫 검색 때 직접 엽니다.
    store: BoxStore? = null,
    embeddingHelper: OnnxEmbeddingHelper? = null,
    private val apiService: SafeLawApiService = RetrofitClient.apiService,
    // 검색 과정(캐시 사용 여부, 서버 전송 ID 등)을 밖에서 보고 싶을 때 넘깁니다. (테스트 화면용)
    private val onEvent: (String) -> Unit = {},
) : DbRepository {

    private val appContext = context.applicationContext

    // DB와 ONNX 모델은 무거워서 앱 시작 때가 아니라 처음 검색할 때 엽니다.
    private val storeLazy = lazy {
        store ?: MyObjectBox.builder()
            .androidContext(appContext)
            .build()
    }
    private val embeddingHelperLazy = lazy { embeddingHelper ?: OnnxEmbeddingHelper(appContext) }

    private val boxStore: BoxStore by storeLazy
    private val embeddingHelper: OnnxEmbeddingHelper by embeddingHelperLazy
    private val lawCaseBox: Box<LawCase> by lazy { boxStore.boxFor(LawCase::class.java) }
    private val importer by lazy { LawCaseImporter(appContext, boxStore) }

    // 판례 주입이 끝났는지 기억해서, 두 번째 호출부터는 바로 넘어갑니다.
    private val prepareMutex = Mutex()
    @Volatile
    private var isDataReady = false

    //  최근 질문 50개의 검색 결과와 서버 원문을 기억
    private val searchResultCache = SearchResultCache()

    /**
     * 📥 검색 전에 판례 데이터가 DB에 들어 있는지 확인하고, 없으면 assets JSON을 주입합니다.
     * 검색 함수들이 알아서 호출하므로 따로 부르지 않아도 되지만,
     * 앱 시작 시 미리 불러두면 첫 검색이 오래 걸리지 않습니다.
     * @return DB에 저장된 판례 개수
     */
    suspend fun prepare(onProgress: (String) -> Unit = {}): Long = prepareMutex.withLock {
        if (isDataReady) {
            return@withLock lawCaseBox.count()
        }
        importer.importIfEmpty(onProgress).also { isDataReady = true }
    }

    /**
     * 🔍 질문을 임베딩한 뒤 벡터 검색으로 가장 유사한 판례 ID를 유사도 순으로 반환합니다.
     * 이전 질문과 충분히 비슷하면 임베딩·검색 없이 이전 결과를 재사용합니다.
     */
    override suspend fun vectorSearch(question: String): List<Long> = withContext(Dispatchers.Default) {
        prepare()

        searchResultCache.findSimilarQuestion(question)?.let { hit ->
            report(
                CACHE_TAG,
                " [캐시 O] 비슷한 이전 질문 재사용 (자카드 ${"%.2f".format(hit.similarity)}) " +
                    "\"$question\" ≈ \"${hit.cachedQuestion}\" -> 판례 ID: ${hit.caseIds}"
            )
            return@withContext hit.caseIds
        }

        report(CACHE_TAG, " [캐시 X] ONNX 모델로 새로 임베딩 계산 중 -> \"$question\"")
        val queryEmbedding = embeddingHelper.embed(question)

        // HNSW는 근사 검색이라 5개만 찾으면 진짜 상위 판례를 놓칩니다.
        // 후보를 넉넉히 찾은 뒤 가까운 순으로 SEARCH_LIMIT개만 씁니다. (결과는 거리 오름차순)
        val caseIds = lawCaseBox.query(LawCase_.embedding.nearestNeighbors(queryEmbedding, CANDIDATE_COUNT))
            .build()
            .use { query -> query.findIdsWithScores().take(SEARCH_LIMIT).map { it.id } }

        report(SEARCH_TAG, " [임베딩 검색 성공] 질문: \"$question\" -> 유사한 판례 ID: $caseIds")
        if (caseIds.isNotEmpty()) {
            searchResultCache.putSearchResult(question, caseIds)
        }
        caseIds
    }

    /**
     * 📡 [섀도우 쿼리] 진짜 판례 ID에 가짜(더미) ID를 섞어 서버에 요청하고,
     * 응답에서 진짜 판례 원문만 남겨 검색 순서대로 이어 붙여 반환합니다.
     * 같은 판례 ID 목록으로 받아둔 원문이 캐시에 있으면 서버에 요청하지 않습니다.
     */
    override suspend fun findGroundingDataFromServer(caseIds: List<Long>): String = withContext(Dispatchers.IO) {
        if (caseIds.isEmpty()) {
            return@withContext ""
        }
        prepare()

        searchResultCache.findGroundingData(caseIds)?.let { cached ->
            report(CACHE_TAG, " [캐시 O] 판례 원문 재사용, 서버 요청 생략 -> 판례 ID: $caseIds")
            return@withContext cached
        }

        val targetIds = caseIds.toSet()
        val dummyIds = pickDummyIds(exclude = targetIds)
        val mixedIds = (caseIds + dummyIds).shuffled()
        report(
            NETWORK_TAG,
            " [섀도우 쿼리] 진짜 ${caseIds.size}개 + 가짜 ${dummyIds.size}개 섞어서 전송 -> $mixedIds\n" +
                "   (진짜: $caseIds / 가짜: $dummyIds)"
        )

        // 서버가 꺼져 있거나 에러가 나도 앱이 죽지 않도록, 판례 없이("") 답변을 이어가게 합니다.
        val response = try {
            apiService.getLawCaseTexts(LawCaseRequest(ids = mixedIds))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            report(NETWORK_TAG, " ❌ 서버 연결 실패, 판례 없이 진행합니다: ${e.message}")
            return@withContext ""
        }
        if (!response.isSuccessful) {
            report(NETWORK_TAG, " ❌ 서버 에러 응답 (코드 ${response.code()}), 판례 없이 진행합니다.")
            return@withContext ""
        }

        val body = response.body().orEmpty()
        val trueCasesById = body.filter { it.id in targetIds }.associateBy { it.id }
        report(NETWORK_TAG, " 서버 응답 ${body.size}개 수신 -> 진짜 ${trueCasesById.size}개 사용, 가짜 ${body.size - trueCasesById.size}개 폐기 완료")

        val groundingData = caseIds
            .mapNotNull { trueCasesById[it] }
            .joinToString(separator = "\n\n") { "[판례 ${it.id}] ${it.title}\n${it.content}" }

        if (groundingData.isNotEmpty()) {
            searchResultCache.putGroundingData(caseIds, groundingData)
        }
        groundingData
    }

    // 임베딩까지 읽지 않도록 ID만 조회한 뒤 더미를 고릅니다.
    private fun pickDummyIds(exclude: Set<Long>): List<Long> {
        val allIds = lawCaseBox.query().build().use { it.findIds() }
        return allIds
            .filter { it !in exclude }
            .shuffled()
            .take(DUMMY_COUNT)
    }

    private fun report(tag: String, message: String) {
        Log.d(tag, message)
        onEvent(message)
    }

    fun close() {
        // 한 번도 검색하지 않았다면 열린 적이 없으니 닫을 것도 없습니다.
        if (embeddingHelperLazy.isInitialized()) {
            embeddingHelper.close()
        }
        if (storeLazy.isInitialized() && !boxStore.isClosed) {
            boxStore.close()
        }
    }

    companion object {
        private const val SEARCH_TAG = "SafeLaw_Search"
        private const val NETWORK_TAG = "SafeLaw_Network"
        private const val CACHE_TAG = "SafeLaw_Cache"
        private const val SEARCH_LIMIT = 5
        private const val CANDIDATE_COUNT = 100
        private const val DUMMY_COUNT = 10
    }
}
