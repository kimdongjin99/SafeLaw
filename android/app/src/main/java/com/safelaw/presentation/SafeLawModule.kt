package com.safelaw.presentation

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.Arguments
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
