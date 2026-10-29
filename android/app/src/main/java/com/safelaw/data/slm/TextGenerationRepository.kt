package com.safelaw.data.slm

import android.content.Context
import com.safelaw.domain.SlmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class TextGenerationRepository(
    context: Context,
    private val bridge: LlamaBridge = LlamaBridge(),
) : SlmRepository {

    private val modelPlacement = ModelPlacement(context)
    private val inferenceProfile = InferenceProfile.forCurrentDevice()
    private val modelPreparationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preparedModel = modelPreparationScope.async {
        modelPlacement.getLocalDevelopmentModel()
    }
    private var isModelLoaded = false

    override suspend fun generateAnswer(
        question: String,
        groundingData: String,
    ): String = withContext(Dispatchers.IO) {
        ensureModelLoaded()

        val prompt = """
            다음 법률 근거만 사용하여 질문에 답변하세요.

            법률 근거:
            $groundingData

            질문:
            $question
        """.trimIndent()

        bridge.generate(
            prompt = prompt,
            maxTokens = inferenceProfile.maxTokens,
            contextSize = inferenceProfile.contextSize,
            batchSize = inferenceProfile.batchSize,
            microBatchSize = inferenceProfile.microBatchSize,
            generationThreads = inferenceProfile.generationThreads,
            promptThreads = inferenceProfile.promptThreads,
        )
    }

    private suspend fun ensureModelLoaded() {
        if (isModelLoaded) {
            return
        }

        val modelFile = preparedModel.await()

        check(bridge.loadModel(modelFile.absolutePath)) {
            "GGUF 모델을 불러오지 못했습니다: ${modelFile.absolutePath}"
        }

        isModelLoaded = true
    }

    fun release() {
        if (isModelLoaded) {
            bridge.releaseModel()
            isModelLoaded = false
        }
    }
}
