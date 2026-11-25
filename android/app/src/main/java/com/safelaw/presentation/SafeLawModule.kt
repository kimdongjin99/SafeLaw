package com.safelaw.presentation

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReadableArray
import com.safelaw.domain.ChatTurn
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.module.annotations.ReactModule
import com.safelaw.domain.AskLegalQuestionUseCase
import com.safelaw.domain.state.GenerationStateManager
import com.safelaw.domain.state.GenerationStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

@ReactModule(name = SafeLawModule.NAME)
class SafeLawModule (
    reactContext: ReactApplicationContext,
    private val askLegalQuestionUseCase : AskLegalQuestionUseCase,
    private val generationStateManager: GenerationStateManager,
) : ReactContextBaseJavaModule(reactContext){

    private val askingJob = SupervisorJob()

    private val askingScope = CoroutineScope(
        askingJob + Dispatchers.Default,
    )

    override fun initialize() {
        super.initialize()
        askingScope.launch {
            generationStateManager.currentStage.collect { stage ->
                if (stage != null) {
                    emitGenerationStage(stage)
                }
            }
        }
    }

    private fun emitGenerationStage(stage: GenerationStage) {
        if (!reactApplicationContext.hasActiveReactInstance()) {
            return
        }

        val payload = Arguments.createMap().apply {
            putString("stage", stage.name)
        }
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(GENERATION_STAGE_EVENT, payload)
    }

    @ReactMethod
    fun ask(
        question : String,
        promise: Promise,
    ) {
        askingScope.launch {
            try {
                val answer = askLegalQuestionUseCase.ask(question)
                promise.resolve(answer)
            } catch (error: Throwable) {
                promise.reject("E_INFERENCE", error.message, error)
            }
        }
    }

    /**
     * 같은 대화방의 최근 대화와 함께 묻는다.
     * history: [{ question: string, answer: string }, ...] (오래된 것 -> 최근 순)
     */
    @ReactMethod
    fun askWithHistory(
        question: String,
        history: ReadableArray,
        promise: Promise,
    ) {
        val turns = (0 until history.size()).mapNotNull { index ->
            val item = history.getMap(index) ?: return@mapNotNull null
            val q = item.getString("question").orEmpty().trim()
            val a = item.getString("answer").orEmpty().trim()
            if (q.isEmpty() || a.isEmpty()) null else ChatTurn(q, a)
        }
        askingScope.launch {
            try {
                val answer = askLegalQuestionUseCase.ask(question, history = turns)
                promise.resolve(answer)
            } catch (error: Throwable) {
                promise.reject("E_INFERENCE", error.message, error)
            }
        }
    }

    @ReactMethod
    fun getGroundingData(
        question: String,
        promise: Promise,
    ) {
        askingScope.launch {
            try {
                val groundingData = askLegalQuestionUseCase.getGroundingData(question)
                promise.resolve(groundingData)
            } catch (error: Throwable) {
                promise.reject("E_GROUNDING", error.message, error)
            }
        }
    }

    @ReactMethod
    fun askStreaming(
        question: String,
        requestId: String,
        promise: Promise,
    ) {
        askingScope.launch {
            try {
                val answer = askLegalQuestionUseCase.ask(question) { token ->
                    emitToken(requestId, token)
                }
                promise.resolve(answer)
            } catch (error: Throwable) {
                promise.reject("E_INFERENCE", error.message, error)
            }
        }
    }

    private fun emitToken(requestId: String, token: String) {
        if (!reactApplicationContext.hasActiveReactInstance()) {
            return
        }

        val payload = Arguments.createMap().apply {
            putString("requestId", requestId)
            putString("token", token)
        }
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(TOKEN_EVENT, payload)
    }

    /** 설정 > 로컬 데이터 관리: 기기에 기억해 둔 검색 결과 캐시를 지웁니다. */
    @ReactMethod
    fun clearSearchCache(promise: Promise) {
        try {
            (reactApplicationContext.applicationContext as com.safelaw.MainApplication).clearSearchCache()
            promise.resolve(true)
        } catch (error: Throwable) {
            promise.reject("E_CLEAR_CACHE", error.message, error)
        }
    }

    @ReactMethod
    fun addListener(eventName: String) = Unit

    @ReactMethod
    fun removeListeners(count: Int) = Unit

    override fun invalidate() {
        askingScope.cancel()
        super.invalidate()
    }

    override fun getName(): String = NAME

    companion object {
        const val NAME = "SafeLawModule"
        const val TOKEN_EVENT = "SafeLawToken"
        const val GENERATION_STAGE_EVENT = "SafeLawGenerationStage"
    }
}
