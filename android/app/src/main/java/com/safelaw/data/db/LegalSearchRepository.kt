package com.safelaw.data.db

import android.content.Context
import android.util.Log
import com.safelaw.data.db.data.LawCase
import com.safelaw.data.db.data.LawCase_
import com.safelaw.data.db.data.MyObjectBox
import com.safelaw.data.db.network.LawCaseRequest
import com.safelaw.data.db.network.LawCaseResponse
import com.safelaw.data.db.network.RetrofitClient
import com.safelaw.data.db.network.SafeLawApiService
import com.safelaw.domain.repository.DbRepository
import com.safelaw.domain.GroundingData
import com.safelaw.domain.LegalSearchHit
import com.safelaw.domain.PrecedentReference
import io.objectbox.Box
import io.objectbox.BoxStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import java.util.concurrent.Executors
import kotlin.random.asKotlinRandom

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

    // ObjectBox DB는 검색·더미 선택에 쓸 때만 열고, 서버에서 판례를 받은 뒤 닫습니다.
    // 열려 있으면 HNSW 벡터 캐시 등으로 수백 MB를 쓰는데, 답변 생성(EXAONE) 중에는 DB가 필요 없기 때문입니다.
    // 닫아도 판례 데이터는 DB 파일에 그대로 있어 다시 넣지 않습니다.
    // 밖에서 넘겨받은 DB(테스트용)는 닫지 않고 계속 씁니다.
    private val injectedStore = store
    private var openedStore: BoxStore? = null
    private val storeLock = Any()
    private val dbMutex = Mutex()

    // ObjectBox는 DB를 읽은 스레드마다 읽기 정보를 따로 들고 있어서, 다른 스레드에서 DB를 닫으면 멈출 수 있습니다.
    // 그래서 DB를 열고, 읽고, 닫는 일은 전부 이 한 스레드에서만 합니다.
    private val dbDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "SafeLaw-DB")
    }.asCoroutineDispatcher()

    // 가짜 ID를 서버가 예측하지 못하도록 보안용 난수를 씁니다.
    private val dummyRandom = SecureRandom().asKotlinRandom()

    // KR-SBERT 모델은 검색할 때만 열고 검색이 끝나면 바로 닫습니다.
    // 검색 다음 단계인 EXAONE이 1.5GB 이상을 쓰므로, 같이 올라가 있으면 저사양 기기에서 메모리가 부족해집니다.
    // 밖에서 넘겨받은 모델(테스트용)은 닫지 않고 계속 씁니다.
    private val injectedEmbeddingHelper = embeddingHelper
    private val embeddingMutex = Mutex()

    private val boxStore: BoxStore
        get() = synchronized(storeLock) {
            injectedStore
                ?: openedStore?.takeUnless { it.isClosed }
                ?: MyObjectBox.builder()
                    .androidContext(appContext)
                    .build()
                    .also {
                        openedStore = it
                        Log.d(DB_TAG, " DB 열기")
                    }
        }
    private val lawCaseBox: Box<LawCase>
        get() = boxStore.boxFor(LawCase::class.java)

    private fun releaseStore() {
        if (injectedStore != null) return
        synchronized(storeLock) {
            openedStore?.let {
                it.closeThreadResources()
                it.close()
                Log.d(DB_TAG, " DB 닫기 (답변 생성 동안 메모리 확보)")
            }
            openedStore = null
        }
    }

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
        withContext(dbDispatcher) {
            if (isDataReady) {
                return@withContext lawCaseBox.count()
            }
            LawCaseImporter(appContext, boxStore, dbDispatcher).importIfEmpty(onProgress).also { isDataReady = true }
        }
    }

    /**
     * 🔍 질문을 임베딩한 뒤 판례 ID와 HNSW 거리를 가까운 순서로 반환합니다.
     * 이전 질문과 충분히 비슷하면 임베딩·검색 없이 이전 결과를 재사용합니다.
     */
    override suspend fun vectorSearch(question: String): List<LegalSearchHit> = withContext(Dispatchers.Default) {
        // 검색 도중 다른 질문의 판례 조회가 끝나 DB를 닫지 않도록 잠급니다.
        dbMutex.withLock {
            prepare()

            searchResultCache.findSimilarQuestion(question)?.let { hit ->
                report(
                    CACHE_TAG,
                    " [캐시 O] 비슷한 이전 질문 재사용 (자카드 ${"%.2f".format(hit.similarity)}) " +
                        "\"$question\" ≈ \"${hit.cachedQuestion}\" -> 검색 결과: ${hit.searchHits}"
                )
                return@withContext hit.searchHits
            }

            report(CACHE_TAG, " [캐시 X] ONNX 모델로 새로 임베딩 계산 중 -> \"$question\"")
            val queryEmbedding = embedWithTemporaryModel(question)

            // HNSW는 근사 검색이라 5개만 찾으면 진짜 상위 판례를 놓칩니다.
            // 후보를 넉넉히 찾은 뒤 가까운 순으로 SEARCH_LIMIT개만 씁니다. (결과는 거리 오름차순)
            val searchHits = withContext(dbDispatcher) {
                lawCaseBox.query(LawCase_.embedding.nearestNeighbors(queryEmbedding, CANDIDATE_COUNT))
                    .build()
                    .use { query ->
                        query.findIdsWithScores()
                            .take(SEARCH_LIMIT)
                            .map { result ->
                                LegalSearchHit(
                                    caseId = result.id,
                                    distance = result.score,
                                )
                            }
                    }
            }

            report(SEARCH_TAG, " [임베딩 검색 성공] 질문: \"$question\" -> 검색 결과: $searchHits")
            if (searchHits.isNotEmpty()) {
                searchResultCache.putSearchResult(question, searchHits)
            }
            searchHits
        }
    }

    // 모델을 열어 임베딩하고 바로 닫습니다. 동시에 두 질문이 와도 한 번에 하나만 열리도록 잠급니다.
    private suspend fun embedWithTemporaryModel(question: String): FloatArray = embeddingMutex.withLock {
        injectedEmbeddingHelper?.let { return@withLock it.embed(question) }

        val helper = OnnxEmbeddingHelper(appContext)
        try {
            helper.embed(question)
        } finally {
            helper.close()
        }
    }

    /**
     * 📡 [섀도우 쿼리] 진짜 판례 ID에 가짜(더미) ID를 섞어 서버에 요청하고,
     * 응답에서 진짜 판례 원문만 남겨 검색 순서대로 이어 붙여 반환합니다.
     * 같은 판례 ID 목록으로 받아둔 원문이 캐시에 있으면 서버에 요청하지 않습니다.
     */
    override suspend fun findGroundingDataFromServer(caseIds: List<Long>): GroundingData = withContext(Dispatchers.IO) {
        // 판례를 받고 나면 답변 생성 전까지 DB가 필요 없으므로, 어떻게 끝나든 DB를 닫습니다.
        dbMutex.withLock {
            try {
                findGroundingData(caseIds)
            } finally {
                withContext(NonCancellable + dbDispatcher) { releaseStore() }
            }
        }
    }

    private suspend fun findGroundingData(caseIds: List<Long>): GroundingData = withContext(Dispatchers.IO) {
        if (caseIds.isEmpty()) {
            return@withContext GroundingData.EMPTY
        }
        prepare()

        searchResultCache.findGroundingData(caseIds)?.let { cached ->
            report(CACHE_TAG, " [캐시 O] 판례 원문 재사용, 서버 요청 생략 -> 판례 ID: $caseIds")
            return@withContext cached
        }

        val targetIds = caseIds.toSet()
        val dummyIds = withContext(dbDispatcher) { pickDummyIds(exclude = targetIds) }
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
            return@withContext GroundingData.EMPTY
        }
        if (!response.isSuccessful) {
            report(NETWORK_TAG, " ❌ 서버 에러 응답 (코드 ${response.code()}), 판례 없이 진행합니다.")
            return@withContext GroundingData.EMPTY
        }

        val body = response.body().orEmpty()
        val trueCasesById = body.filter { it.id in targetIds }.associateBy { it.id }
        report(NETWORK_TAG, " 서버 응답 ${body.size}개 수신 -> 진짜 ${trueCasesById.size}개 사용, 가짜 ${body.size - trueCasesById.size}개 폐기 완료")

        val orderedCases = caseIds.mapNotNull { trueCasesById[it] }
        val groundingData = GroundingData(
            precedents = orderedCases.joinToString(separator = "\n\n") { it.toGroundingText() },
            precedentReferences = orderedCases.map { it.toPrecedentReference() },
        )

        if (groundingData.precedents.isNotEmpty()) {
            searchResultCache.putGroundingData(caseIds, groundingData)
        }
        groundingData
    }

    // 가짜 ID는 판례 ID 범위(1 ~ 판례 수)에서 무작위로 뽑고, DB에 있는 번호인지 하나씩 확인합니다.
    // 예전처럼 ID 12만 개를 전부 꺼내 섞으면 DB 파일 전체를 읽게 되어, 앱을 켜고 첫 질문 때 약 7초가 걸렸습니다.
    private fun pickDummyIds(exclude: Set<Long>): List<Long> {
        val box = lawCaseBox
        val maxId = box.count()
        if (maxId <= 0L) return emptyList()

        val picked = LinkedHashSet<Long>()
        var attempts = 0
        while (picked.size < DUMMY_COUNT && attempts < DUMMY_COUNT * MAX_ATTEMPTS_PER_DUMMY) {
            attempts++
            val id = dummyRandom.nextLong(1L, maxId + 1)
            if (id !in exclude && box.contains(id)) {
                picked += id
            }
        }
        return picked.toList()
    }

    // 예: [판례 87425] 컴퓨터등사용사기등 (2023고단390, 인천지방법원 부천지원)
    // 사건 번호·법원은 있는 것만 붙이고, 둘 다 없으면 괄호를 생략합니다.
    private fun LawCaseResponse.toGroundingText(): String {
        val details = listOf(caseNum, courtName)
            .mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) }
            .joinToString(", ")
        val header = buildString {
            append("[판례 ").append(id).append("] ")
            append(caseName?.trim().orEmpty().ifEmpty { "사건명 없음" })
            if (details.isNotEmpty()) append(" (").append(details).append(")")
        }
        return header + "\n" + content.orEmpty()
    }

    private fun LawCaseResponse.toPrecedentReference(): PrecedentReference =
        PrecedentReference(
            id = id,
            caseName = caseName?.trim()?.takeIf(String::isNotEmpty),
            caseNumber = caseNum?.trim()?.takeIf(String::isNotEmpty),
            courtName = courtName?.trim()?.takeIf(String::isNotEmpty),
        )

    private fun report(tag: String, message: String) {
        Log.d(tag, message)
        onEvent(message)
    }

    fun close() {
        // KR-SBERT 모델은 검색마다 열고 닫으므로 여기서는 넘겨받은 것만 닫습니다.
        injectedEmbeddingHelper?.close()
        injectedStore?.close()
        releaseStore()
    }

    companion object {
        private const val SEARCH_TAG = "SafeLaw_Search"
        private const val DB_TAG = "SafeLaw_DB"
        private const val NETWORK_TAG = "SafeLaw_Network"
        private const val CACHE_TAG = "SafeLaw_Cache"
        private const val SEARCH_LIMIT = 5
        private const val CANDIDATE_COUNT = 100
        private const val DUMMY_COUNT = 10
        // 빈 번호가 많아도 무한히 돌지 않도록 가짜 ID 하나당 최대 시도 횟수
        private const val MAX_ATTEMPTS_PER_DUMMY = 20
    }
}
