package com.example.safelaw.repository

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import io.objectbox.Box
import com.example.safelaw.data.LawCase
import com.example.safelaw.data.LawCase_
import java.io.File
import java.nio.LongBuffer

class LegalSearchManager(private val context: Context, private val lawCaseBox: Box<LawCase>) {

    private val ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val ortSession: OrtSession

    init {
        val modelFileName = "model.onnx"
        val modelFile = File(context.cacheDir, modelFileName)

        if (!modelFile.exists()) {
            context.assets.open(modelFileName).use { input ->
                modelFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        ortSession = ortEnv.createSession(modelFile.absolutePath)
    }

    fun search(query: String, limit: Int = 5): List<LawCase> {
        // [핵심] 이제 랜덤이 아니라 진짜 AI 모델을 돌려 벡터를 뽑습니다.
        val queryVector = getEmbedding(query)

        return lawCaseBox.query()
            .nearestNeighbors(LawCase_.featureVector, queryVector, limit)
            .build()
            .find()
    }

    fun getEmbedding(text: String): FloatArray {
        // 1. 토큰 생성 (글자 -> 숫자)
        val tokens = text.map { it.code.toLong() }.toLongArray()
        val shape = longArrayOf(1, tokens.size.toLong())

        // 2. 모델이 요구하는 3가지 필수 입력값 생성
        val tokenTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokens), shape)

        // [추가] token_type_ids: 모두 0으로 채운 데이터 (문장 구분을 위해 필요)
        val tokenTypeIds = LongArray(tokens.size) { 0L }
        val tokenTypeTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(tokenTypeIds), shape)

        // [추가] attention_mask: 모두 1로 채운 데이터 (어디를 집중해서 볼지 알려줌)
        val attentionMask = LongArray(tokens.size) { 1L }
        val maskTensor = OnnxTensor.createTensor(ortEnv, LongBuffer.wrap(attentionMask), shape)

        // 3. 모델 실행 (3개 데이터를 세트로 던집니다)
        val inputs = mapOf(
            "input_ids" to tokenTensor,
            "token_type_ids" to tokenTypeTensor,
            "attention_mask" to maskTensor
        )

        val output = ortSession.run(inputs)

        // 4. 결과값 추출
        val resultValue = output[0].value as Array<Array<FloatArray>>
        return resultValue[0][0]
    }

    fun close() {
        ortSession.close()
        ortEnv.close()
    }
}