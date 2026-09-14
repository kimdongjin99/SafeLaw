// React Native의 JavaScript 호출을 llama.cpp JNI 브리지의 비동기 작업으로 전달
package com.safelaw.slm

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

@ReactModule(name = LlamaModule.NAME)
class LlamaModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private val bridge = LlamaBridge()
    private val modelPlacement = ModelPlacement(reactContext)
    private val executor = Executors.newSingleThreadExecutor()
    private val invalidated = AtomicBoolean(false)

    override fun getName(): String = NAME

    /** 에뮬레이터의 앱 전용 외부 폴더에 미리 복사한 GGUF 모델을 로드합니다. */
    @ReactMethod
    fun loadLocalModel(promise: Promise) {
        execute(promise, ERROR_LOAD_MODEL) {
            val modelFile = modelPlacement.getLocalDevelopmentModel()
            if (bridge.loadModel(modelFile.absolutePath)) {
                promise.resolve(modelFile.absolutePath)
            } else {
                promise.reject(ERROR_LOAD_MODEL, "GGUF 모델을 불러오지 못했습니다: ${modelFile.absolutePath}")
            }
        }
    }

    @ReactMethod
    fun loadModel(modelPath: String, promise: Promise) {
        if (modelPath.isBlank()) {
            promise.reject(ERROR_INVALID_ARGUMENT, "모델 경로가 비어 있습니다.")
            return
        }

        execute(promise, ERROR_LOAD_MODEL) {
            if (bridge.loadModel(modelPath)) {
                promise.resolve(true)
            } else {
                promise.reject(ERROR_LOAD_MODEL, "GGUF 모델을 불러오지 못했습니다: $modelPath")
            }
        }
    }

    @ReactMethod
    fun generate(prompt: String, maxTokens: Double, promise: Promise) {
        if (prompt.isBlank()) {
            promise.reject(ERROR_INVALID_ARGUMENT, "프롬프트가 비어 있습니다.")
            return
        }

        val tokenCount = maxTokens.toInt()
        if (tokenCount <= 0) {
            promise.reject(ERROR_INVALID_ARGUMENT, "maxTokens는 1 이상이어야 합니다.")
            return
        }

        execute(promise, ERROR_GENERATE) {
            promise.resolve(bridge.generate(prompt, tokenCount))
        }
    }

    @ReactMethod
    fun releaseModel(promise: Promise) {
        execute(promise, ERROR_RELEASE_MODEL) {
            bridge.releaseModel()
            promise.resolve(null)
        }
    }

    override fun invalidate() {
        if (invalidated.compareAndSet(false, true)) {
            try {
                executor.execute { bridge.releaseModel() }
            } finally {
                executor.shutdown()
            }
        }
        super.invalidate()
    }

    private fun execute(promise: Promise, errorCode: String, task: () -> Unit) {
        if (invalidated.get()) {
            promise.reject(ERROR_MODULE_INVALIDATED, "LlamaModule이 이미 종료되었습니다.")
            return
        }

        try {
            executor.execute {
                try {
                    task()
                } catch (error: Throwable) {
                    promise.reject(errorCode, error.message ?: "네이티브 추론 작업에 실패했습니다.", error)
                }
            }
        } catch (error: RejectedExecutionException) {
            promise.reject(ERROR_MODULE_INVALIDATED, "LlamaModule이 이미 종료되었습니다.", error)
        }
    }

    companion object {
        const val NAME = "LlamaModule"

        private const val ERROR_INVALID_ARGUMENT = "E_INVALID_ARGUMENT"
        private const val ERROR_LOAD_MODEL = "E_LOAD_MODEL"
        private const val ERROR_GENERATE = "E_GENERATE"
        private const val ERROR_RELEASE_MODEL = "E_RELEASE_MODEL"
        private const val ERROR_MODULE_INVALIDATED = "E_MODULE_INVALIDATED"
    }
}
