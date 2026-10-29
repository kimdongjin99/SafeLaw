// 역할: 개발용 로컬 GGUF 경로와 운영용 Hugging Face 모델 저장 위치를 관리합니다.
package com.safelaw.data.slm

import android.content.Context
import android.util.Log
import java.io.File

internal class ModelPlacement(context: Context) {
    private val appContext = context.applicationContext

    /** APK asset에 포함된 시험용 GGUF를 앱 내부 저장소로 복사하고 파일 경로를 반환합니다. */
    fun getLocalDevelopmentModel(): File {
        val modelDir = File(appContext.noBackupFilesDir, "models")
        check(modelDir.isDirectory || modelDir.mkdirs()) {
            "모델 저장 폴더를 만들 수 없습니다: ${modelDir.absolutePath}"
        }
        val modelFile = File(modelDir, MODEL_FILE_NAME)
        val temporaryFile = File(modelDir, "$MODEL_FILE_NAME.part")
        val assetLength = appContext.assets.openFd(MODEL_ASSET_PATH).use { it.length }

        if (modelFile.isFile && modelFile.canRead() && modelFile.length() == assetLength) {
            Log.i(LOG_TAG, "Using copied model: ${modelFile.absolutePath}")
            return modelFile
        }

        Log.i(
            LOG_TAG,
            "Copying bundled model from assets ($assetLength bytes) to ${modelFile.absolutePath}",
        )

        if (temporaryFile.exists()) {
            check(temporaryFile.delete()) {
                "이전 임시 모델 파일을 삭제할 수 없습니다: ${temporaryFile.absolutePath}"
            }
        }

        appContext.assets.open(MODEL_ASSET_PATH).buffered(MODEL_COPY_BUFFER_SIZE).use { input ->
            temporaryFile.outputStream().buffered(MODEL_COPY_BUFFER_SIZE).use { output ->
                input.copyTo(output, MODEL_COPY_BUFFER_SIZE)
            }
        }

        check(temporaryFile.length() == assetLength) {
            temporaryFile.delete()
            "APK에서 복사한 모델 크기가 올바르지 않습니다."
        }

        if (modelFile.exists()) {
            check(modelFile.delete()) {
                "기존 모델 파일을 교체할 수 없습니다: ${modelFile.absolutePath}"
            }
        }
        check(temporaryFile.renameTo(modelFile)) {
            "복사한 모델을 최종 위치로 이동하지 못했습니다: ${modelFile.absolutePath}"
        }

        Log.i(LOG_TAG, "Bundled model copy completed: ${modelFile.absolutePath}")

        check(modelFile.isFile && modelFile.canRead()) {
            "GGUF 모델을 찾거나 읽을 수 없습니다: ${modelFile.absolutePath}"
        }
        return modelFile
    }

    /*
     * 운영 전환 시 사용할 Hugging Face 다운로드 방식입니다.
     * 네트워크 작업이므로 LlamaModule의 백그라운드 실행 큐 또는 WorkManager에서 호출해야 합니다.
     * 실제 배포 전에는 MODEL_SHA256을 Hugging Face에 업로드한 파일의 SHA-256 값으로 교체합니다.
     *
    fun downloadFromHuggingFace(): File {
        check(MODEL_SHA256.length == 64) {
            "MODEL_SHA256에 실제 GGUF 파일의 SHA-256 값을 설정해야 합니다."
        }

        val modelDir = File(appContext.noBackupFilesDir, "models").apply { mkdirs() }
        val destination = File(modelDir, MODEL_FILE_NAME)
        val temporary = File(modelDir, "$MODEL_FILE_NAME.part")

        if (destination.isFile && sha256(destination).equals(MODEL_SHA256, ignoreCase = true)) {
            return destination
        }

        val connection = java.net.URL(HUGGING_FACE_MODEL_URL).openConnection() as
            java.net.HttpURLConnection
        connection.instanceFollowRedirects = true
        connection.connectTimeout = 30_000
        connection.readTimeout = 30_000

        try {
            check(connection.responseCode in 200..299) {
                "Hugging Face 다운로드 실패: HTTP ${connection.responseCode}"
            }

            connection.inputStream.use { input ->
                temporary.outputStream().buffered().use { output ->
                    input.copyTo(output)
                }
            }

            check(sha256(temporary).equals(MODEL_SHA256, ignoreCase = true)) {
                temporary.delete()
                "다운로드한 모델의 SHA-256 값이 일치하지 않습니다."
            }

            if (destination.exists()) destination.delete()
            check(temporary.renameTo(destination)) {
                "검증한 모델 파일을 최종 위치로 이동하지 못했습니다."
            }
            return destination
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().buffered().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte) }
    }
    */

    companion object {
        private const val LOG_TAG = "SafeLawModel"
        private const val MODEL_ASSET_PATH = "models/safe-law-exaone-v3-q4_k_m.gguf"
        private const val MODEL_COPY_BUFFER_SIZE = 8 * 1024 * 1024
        const val MODEL_FILE_NAME = "safe-law-exaone-v3-q4_k_m.gguf"

        // 운영용 Hugging Face 다운로드 코드를 활성화할 때 사용합니다.
        private const val HUGGING_FACE_MODEL_URL =
            "https://huggingface.co/jjjof/safe_law_exaone_gguf_v3/resolve/main/" +
                "$MODEL_FILE_NAME?download=true"
        private const val MODEL_SHA256 =
            "354120e805a66e706e0b6a1bc40391e416b66038e241328dc6119691f3709dc4"
    }
}
