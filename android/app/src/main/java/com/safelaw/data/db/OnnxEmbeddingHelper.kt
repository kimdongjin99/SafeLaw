package com.safelaw.data.db

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer
import kotlin.math.sqrt

/**
 * KR-SBERT(snunlp/KR-SBERT-V40K-klueNLI-augSTS) ONNX 모델로 문장을 768차원 벡터로 바꿉니다.
 * 판례 임베딩 파일과 같은 모델·같은 방식(평균 풀링 + 정규화)이어야 검색이 맞습니다.
 * 모델·vocab·판례 임베딩은 Colab_code/krsbert_embedding.ipynb로 만듭니다.
 */
class OnnxEmbeddingHelper(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession
    private val tokenizer: BertWordPieceTokenizer

    init {
        val modelBytes = context.assets.open(MODEL_FILE_NAME).use { it.readBytes() }
        session = env.createSession(modelBytes)

        tokenizer = context.assets.open(VOCAB_FILE_NAME).bufferedReader().useLines { lines ->
            BertWordPieceTokenizer.fromVocabLines(lines)
        }
    }

    /**
     * 유저의 텍스트 질문을 받아 768차원 FloatArray 벡터로 변환하는 함수
     */
    fun embed(text: String): FloatArray {
        val encoding = tokenizer.encode(text)
        val shape = longArrayOf(1, encoding.inputIds.size.toLong())

        OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.inputIds), shape).use { inputIds ->
            OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.attentionMask), shape).use { attentionMask ->
                OnnxTensor.createTensor(env, LongBuffer.wrap(encoding.tokenTypeIds), shape).use { tokenTypeIds ->
                    val inputs = mapOf(
                        "input_ids" to inputIds,
                        "attention_mask" to attentionMask,
                        "token_type_ids" to tokenTypeIds,
                    )
                    session.run(inputs).use { output ->
                        // last_hidden_state: [1, 토큰 수, 768]
                        @Suppress("UNCHECKED_CAST")
                        val tokenEmbeddings = (output[0].value as Array<Array<FloatArray>>)[0]
                        return meanPoolAndNormalize(tokenEmbeddings, encoding.attentionMask)
                    }
                }
            }
        }
    }

    fun close() {
        session.close()
    }

    companion object {
        private const val MODEL_FILE_NAME = "kr_sbert_qint8.onnx"
        private const val VOCAB_FILE_NAME = "vocab.txt"

        /** 토큰별 벡터를 평균 내고(패딩 제외) 길이가 1이 되도록 정규화합니다. */
        fun meanPoolAndNormalize(tokenEmbeddings: Array<FloatArray>, attentionMask: LongArray): FloatArray {
            val dimension = tokenEmbeddings[0].size
            val pooled = FloatArray(dimension)
            var tokenCount = 0f

            tokenEmbeddings.forEachIndexed { index, tokenEmbedding ->
                if (attentionMask[index] == 1L) {
                    for (d in 0 until dimension) {
                        pooled[d] += tokenEmbedding[d]
                    }
                    tokenCount++
                }
            }

            for (d in 0 until dimension) {
                pooled[d] /= maxOf(tokenCount, 1e-9f)
            }

            val norm = sqrt(pooled.sumOf { (it * it).toDouble() }).toFloat()
            return FloatArray(dimension) { pooled[it] / maxOf(norm, 1e-12f) }
        }
    }
}
