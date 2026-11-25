package com.safelaw.data.slm

import android.content.Context
import com.safelaw.domain.ChatTurn
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
        history: List<ChatTurn>,
    ): String = withContext(Dispatchers.IO) {

        // 모델 추론 시행 (최근 대화를 넣되, 문맥 한도를 넘으면 오래된 대화부터 빼고 다시 시도)
        var turns = history.takeLast(MAX_HISTORY_TURNS)
        var generatedModelAnswer: String
        while (true) {
            generatedModelAnswer = modelInference(
                question = question,
                groundingData = groundingData,
                onToken = onToken,
                history = turns,
            )
            if (generatedModelAnswer != PROMPT_TOO_LONG || turns.isEmpty()) break
            turns = turns.drop(1)
        }

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
        history: List<ChatTurn>,
    ): String {

        // 모델 적제 확인
        ensureModelLoaded()

        // 이전 대화는 맥락 파악용으로만 짧게 넣는다 (답변은 앞부분만 잘라 압축)
        val historySection = if (history.isEmpty()) {
            ""
        } else {
            val lines = history.joinToString("\n") { turn ->
                "사용자: ${turn.question.take(MAX_HISTORY_QUESTION_CHARS)}\n" +
                    "AI: ${turn.answer.take(MAX_HISTORY_ANSWER_CHARS)}"
            }
            "이전 대화 (현재 질문의 맥락을 이해하는 데만 참고하세요):\n$lines\n\n"
        }

        val prompt = "다음 법률 근거만 사용하여 질문에 답변하세요.\n\n" +
            historySection +
            "법률 근거:\n${groundingData.precedents}\n\n" +
            "질문:\n$question"

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

    companion object {
        // 프롬프트에 넣을 최근 대화 수와 길이 (문맥 한도 1,024~2,048토큰 안에 판례 5개와 함께 들어가도록)
        private const val MAX_HISTORY_TURNS = 3
        private const val MAX_HISTORY_QUESTION_CHARS = 150
        private const val MAX_HISTORY_ANSWER_CHARS = 200
        // llama_jni.cpp가 프롬프트가 문맥 한도를 넘을 때 돌려주는 문구
        private const val PROMPT_TOO_LONG = "Prompt is too long for the current context size."
    }

    fun release() {
        if (isModelLoaded) {
            bridge.releaseModel()
            isModelLoaded = false
        }
    }
}
