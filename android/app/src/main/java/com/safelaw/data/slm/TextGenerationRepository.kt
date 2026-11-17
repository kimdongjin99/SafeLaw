package com.safelaw.data.slm

import android.content.Context
import com.safelaw.domain.GroundingData
import com.safelaw.domain.repository.SlmRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

class TextGenerationRepository(
    context: Context,
    private val bridge: LlamaBridge = LlamaBridge(),
    private val postProcessor: LegalAnswerPostProcessor = LegalAnswerPostProcessor(),
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
        groundingData: GroundingData,
        onToken: (String) -> Unit,
    ): String = withContext(Dispatchers.IO) {

        // 모델 추론 시행
        val generatedModelAnswer = modelInference(
            question = question,
            groundingData = groundingData,
            onToken = onToken,
        )

        // 후처리
        postProcess(
            generatedAnswer = generatedModelAnswer,
            groundingData = groundingData,
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

    private suspend fun modelInference(
        question: String,
        groundingData: GroundingData,
        onToken: (String) -> Unit,
    ): String {

        // 모델 적제 확인
        ensureModelLoaded()

        val prompt = """
            다음 법률 근거만 사용하여 질문에 답변하세요.

            법률 근거:
            ${groundingData.precedents}

            질문:
            $question
        """.trimIndent()

        return bridge.generate(
            prompt = prompt,
            maxTokens = inferenceProfile.maxTokens,
            contextSize = inferenceProfile.contextSize,
            batchSize = inferenceProfile.batchSize,
            microBatchSize = inferenceProfile.microBatchSize,
            generationThreads = inferenceProfile.generationThreads,
            promptThreads = inferenceProfile.promptThreads,
            tokenCallback = LlamaBridge.TokenCallback { tokenBytes ->
                onToken(tokenBytes.toString(Charsets.UTF_8))
            },
        )
    }

    private fun postProcess(
        generatedAnswer: String,
        groundingData: GroundingData,
    ): String = postProcessor.process(
        generatedAnswer = generatedAnswer,
        groundingData = groundingData,
    )

    fun release() {
        if (isModelLoaded) {
            bridge.releaseModel()
            isModelLoaded = false
        }
    }
}
