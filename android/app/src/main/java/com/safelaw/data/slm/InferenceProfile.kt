package com.safelaw.data.slm

import android.os.Build
import android.util.Log
import java.util.Locale

/** 기기 성능에 맞춰 llama.cpp context, batch, thread 구성을 선택합니다. */
internal data class InferenceProfile(
    val name: String,
    val contextSize: Int,
    val batchSize: Int,
    val microBatchSize: Int,
    val generationThreads: Int,
    val promptThreads: Int,
    val maxTokens: Int,
) {
    companion object {
        private const val LOG_TAG = "SafeLawProfile"

        private val LOW = InferenceProfile(
            name = "low-s6-lite",
            contextSize = 1024,
            batchSize = 256,
            microBatchSize = 128,
            generationThreads = 4,
            promptThreads = 4,
            maxTokens = 128,
        )

        private val MEDIUM = InferenceProfile(
            name = "medium-s10-s6-lite-2024",
            contextSize = 1536,
            batchSize = 256,
            microBatchSize = 128,
            generationThreads = 4,
            promptThreads = 4,
            maxTokens = 128,
        )

        private val S7_FE_CELLULAR = InferenceProfile(
            name = "medium-s7-fe-cellular",
            contextSize = 1536,
            batchSize = 256,
            microBatchSize = 128,
            generationThreads = 4,
            promptThreads = 4,
            maxTokens = 160,
        )

        private val S7_FE_WIFI = InferenceProfile(
            name = "medium-s7-fe-wifi",
            contextSize = 1536,
            batchSize = 512,
            microBatchSize = 256,
            generationThreads = 4,
            promptThreads = 4,
            maxTokens = 160,
        )

        private val HIGH = InferenceProfile(
            name = "high-tab-s7-s9-fe-plus",
            contextSize = 2048,
            batchSize = 512,
            microBatchSize = 512,
            generationThreads = 4,
            promptThreads = 6,
            maxTokens = 192,
        )

        fun forCurrentDevice(): InferenceProfile = forModel(Build.MODEL).also { profile ->
            Log.i(
                LOG_TAG,
                "Selected ${profile.name} for model=${Build.MODEL}, " +
                    "ctx=${profile.contextSize}, batch=${profile.batchSize}, " +
                    "ubatch=${profile.microBatchSize}, threads=${profile.generationThreads}, " +
                    "promptThreads=${profile.promptThreads}, maxTokens=${profile.maxTokens}",
            )
        }

        internal fun forModel(model: String): InferenceProfile {
            val normalized = model.uppercase(Locale.US)
            return when {
                // Galaxy Tab S6 Lite 2020 / 2022
                normalized.containsAny("P610", "P615", "P617", "P613", "P619") -> LOW

                // Galaxy Tab S6 Lite 2024, Galaxy S10 5G (SM-G977N)
                normalized.containsAny("P620", "P625", "G977") -> MEDIUM

                // Galaxy Tab S7 FE Wi-Fi
                normalized.containsAny("T730", "T733") -> S7_FE_WIFI

                // Galaxy Tab S7 FE LTE / 5G
                normalized.containsAny("T735", "T736", "T737") -> S7_FE_CELLULAR

                // Galaxy Tab S7, Galaxy Tab S9 FE+
                normalized.containsAny(
                    "T870", "T875", "T876", "T878",
                    "X610", "X616",
                ) -> HIGH

                // 알 수 없는 기기는 메모리 사용량이 가장 낮은 안전 설정을 사용합니다.
                else -> LOW
            }
        }

        private fun String.containsAny(vararg modelCodes: String): Boolean =
            modelCodes.any(::contains)
    }
}
