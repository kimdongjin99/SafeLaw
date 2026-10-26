package com.example.safelaw

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.LongBuffer

class OnnxEmbeddingHelper(context: Context) {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        // assets 폴더에 있는 onnx 모델 파일 이름 (예: model.onnx 또는 embedding_model.onnx)
        // 본인이 보유한 모델 파일명으로 수정해주세요!
        val modelBytes = context.assets.open("model_qint8_arm64.onnx").readBytes()
        session = env.createSession(modelBytes)
    }

    /**
     * 유저의 텍스트 질문을 받아 384차원 FloatArray 벡터로 변환하는 함수
     */
    fun embed(text: String): FloatArray {
        // 주의: 사용중인 ONNX 모델의 입력 방식(토크나이저 등)에 맞추어 전처리가 필요합니다.
        // 현재는 예시로 384차원의 더미 또는 기본 변환 로직을 수행합니다.
        // 만약 토크나이저가 따로 있다면 토큰화 과정을 거쳐 텐서에 넣어주어야 합니다.

        // 임시로 384차원 벡터를 반환하는 구조 (모델 규격에 맞춰 조정 필요)
        val dummyInput = FloatArray(384) { 0.1f }
        return dummyInput
    }

    fun close() {
        session.close()
        env.close()
    }
}