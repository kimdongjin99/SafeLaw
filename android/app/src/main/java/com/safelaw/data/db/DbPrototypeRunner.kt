package com.safelaw.data.db

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.safelaw.data.db.data.MyObjectBox
import io.objectbox.BoxStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** DB 파이프라인(주입 → 벡터 검색 → 서버 원문 조회)을 테스트 화면에서 입력한 질문으로 실행해 봅니다. */
class DbPrototypeRunner(
    context: Context,
    private val listener: Listener,
) {

    /** 모든 콜백은 메인 스레드에서 호출됩니다. */
    interface Listener {
        fun onStatus(status: String)
        fun onReady(caseCount: Long)
        fun onLog(message: String)
        fun onSearchFinished()
    }

    private val appContext = context.applicationContext
    private val runnerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mainHandler = Handler(Looper.getMainLooper())

    private lateinit var store: BoxStore
    private lateinit var repository: LegalSearchRepository

    /** DB를 열고, 필요하면 JSON을 주입한 뒤 검색 준비가 되면 onReady를 호출합니다. */
    fun start() {
        try {
            store = MyObjectBox.builder()
                .androidContext(appContext)
                .build()
            repository = LegalSearchRepository(
                context = appContext,
                store = store,
                onEvent = { message -> postLog(message) },
            )
        } catch (e: Exception) {
            Log.e(TAG, " 초기화 중 에러 발생: ${e.message}")
            e.printStackTrace()
            post { listener.onStatus("초기화 실패: ${e.message}") }
            return
        }

        runnerScope.launch {
            try {
                post { listener.onStatus("DB 준비 중... (첫 실행은 JSON 주입에 수 분 걸려요)") }
                // 검색 함수가 알아서 주입하지만, 진행 상황을 화면에 보여주려고 미리 준비시킵니다.
                val caseCount = repository.prepare { progress ->
                    post { listener.onStatus("DB 준비 중...$progress") }
                }
                post { listener.onReady(caseCount) }

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, " JSON 주입 에러: ${e.message}")
                e.printStackTrace()
                post { listener.onStatus("DB 준비 실패: ${e.message}") }
            }
        }
    }

    /** 질문 하나로 검색 → 서버 원문 조회를 실행하고, 과정을 onLog로 알려줍니다. */
    fun ask(prompt: String) {
        runnerScope.launch {
            val startTime = System.currentTimeMillis()
            postLog("━━━━━━━━━━━━━━━━━━━━\n❓ 질문: \"$prompt\"")
            try {
                val caseIds = repository.vectorSearch(prompt)
                if (caseIds.isEmpty()) {
                    postLog(" 유사한 판례를 찾지 못했습니다.")
                    return@launch
                }

                val groundingData = repository.findGroundingDataFromServer(caseIds)
                Log.d(TAG, " 진짜 판례 수신 성공 -> \"$prompt\"\n$groundingData")
                postLog(" 📄 진짜 판례 원문:\n${groundingData.take(RESULT_PREVIEW_LENGTH)}" +
                    if (groundingData.length > RESULT_PREVIEW_LENGTH) "\n... (이하 생략, 전체는 Logcat)" else "")

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, " DB 파이프라인 실행 중 에러: ${e.message}")
                e.printStackTrace()
                postLog(" ❌ 에러: ${e.message}")
            } finally {
                postLog(" ⏱ ${System.currentTimeMillis() - startTime}ms")
                post { listener.onSearchFinished() }
            }
        }
    }

    fun close() {
        runnerScope.cancel()
        if (::repository.isInitialized) {
            repository.close()
        } else if (::store.isInitialized && !store.isClosed) {
            store.close()
        }
    }

    private fun postLog(message: String) = post { listener.onLog(message) }

    private fun post(action: () -> Unit) {
        mainHandler.post(action)
    }

    companion object {
        private const val TAG = "SafeLaw_DB"
        private const val RESULT_PREVIEW_LENGTH = 1500
    }
}
