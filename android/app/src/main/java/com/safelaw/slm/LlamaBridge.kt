// Kotlin 코드와 llama.cpp의 네이티브 JNI 함수를 직접 연결
package com.safelaw.slm

/** Kotlin 코드와 llama.cpp 네이티브 추론 엔진을 연결하는 JNI 브리지입니다. */
class LlamaBridge {
    companion object {
        init {
            System.loadLibrary("llama_jni")
        }
    }

    /** 지정한 경로의 GGUF 모델을 네이티브 메모리에 로드합니다. */
    external fun loadModel(modelPath: String): Boolean

    /** 로드된 모델에 프롬프트를 전달하고 생성된 텍스트를 반환합니다. */
    external fun generate(prompt: String, maxTokens: Int): String

    /** 로드된 모델과 네이티브 리소스를 해제합니다. */
    external fun releaseModel()
}
