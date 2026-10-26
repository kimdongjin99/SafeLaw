package com.safelaw.data.slm

import android.content.Context
import com.safelaw.domain.SlmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TextGenerationRepository(
    context: Context,
    private val bridge: LlamaBridge = LlamaBridge(),
) : SlmRepository {

    private val modelPlacement = ModelPlacement(context)
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
            maxTokens = 256,
        )
    }

    private fun ensureModelLoaded() {
        if (isModelLoaded) {
            return
        }

        val modelFile = modelPlacement.getLocalDevelopmentModel()

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