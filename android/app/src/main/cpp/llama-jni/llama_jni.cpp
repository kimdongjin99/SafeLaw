// Kotlin에서 GGUF 모델 로딩, 텍스트 생성, 메모리 해제를 호출할 수 있도록 JNI로 연결
#include <jni.h>
#include <android/log.h>
#include <algorithm>
#include <mutex>
#include <string>
#include <thread>
#include <vector>

#include "llama.h"

namespace { //이 .cpp에서만 사용할 변수, 메서드 정의
    constexpr const char *LOG_TAG = "SafeLawLlama";
    constexpr uint32_t CONTEXT_SIZE = 2048;
    constexpr uint32_t BATCH_SIZE = 512;


    std::mutex g_mutex;
    //여러 스레드가 공유 데이터에 동시에 접근하지 못하도록 잠그는 장치
    llama_model *g_model = nullptr;
    bool g_backend_initialized = false;

    //llama.cpp의 오류 로그를 android tomcat에서 확인할 수 있도록 해주는 메서드
    void llama_android_log(
            enum ggml_log_level level,
            const char *text,
            void *
    ) {
        int priority = ANDROID_LOG_INFO;
        switch (level) {
            case GGML_LOG_LEVEL_ERROR:
                priority = ANDROID_LOG_ERROR;
                break;
            case GGML_LOG_LEVEL_WARN:
                priority = ANDROID_LOG_WARN;
                break;
            case GGML_LOG_LEVEL_DEBUG:
                priority = ANDROID_LOG_DEBUG;
                break;
            default:
                break;
        }
        __android_log_print(priority, LOG_TAG, "%s", text);
    }

    //개발자가 C++에서 오류 로그를 남기려고 만든 함수
    void log_error(const std::string &message) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "%s", message.c_str());
    }

    //모델 메모리 해제
    void release_model_locked() {
        if (g_model != nullptr) {
            llama_model_free(g_model);
            g_model = nullptr;
        }
    }

    //모델 프롬프트 생성 메서드, 유저 메시지를 받아 프롬프트를 만든다.
    std::string format_prompt(const std::string &user_prompt) {
        const char *system_prompt =
                "당신은 한국 법률 정보를 제공하는 도우미 SafeLaw 입니다. "
                "반드시 한국어로 답변하세요. "
                "확실하지 않은 내용은 확실하지 않다고 밝혀 주세요. "
                "법령이나 판례를 임의로 만들어내지 마세요.";

        const char *chat_template = llama_model_chat_template(g_model, nullptr);
        if (chat_template == nullptr) {
            return std::string(system_prompt) + "\n\nUser: " + user_prompt + "\nAssistant:";
        }

        const llama_chat_message messages[] = {
                {"system", system_prompt},
                {"user",   user_prompt.c_str()},
        };

        std::vector<char> buffer(user_prompt.size() * 2 + 1024);
        int32_t length = llama_chat_apply_template(
                chat_template, messages, 2, true, buffer.data(),
                static_cast<int32_t>(buffer.size())
        );

        if (length > static_cast<int32_t>(buffer.size())) {
            buffer.resize(length);
            length = llama_chat_apply_template(
                    chat_template, messages, 2, true, buffer.data(),
                    static_cast<int32_t>(buffer.size())
            );
        }

        if (length < 0) {
            return std::string(system_prompt) + "\n\nUser: " + user_prompt + "\nAssistant:";
        }

        return std::string(buffer.data(), length);
    }

    std::string generate_text(const std::string &prompt, int max_tokens) {
        if (g_model == nullptr) {
            return "Model is not loaded.";
        }

        const llama_vocab *vocab = llama_model_get_vocab(g_model);
        const std::string formatted_prompt = format_prompt(prompt);
        const int token_count = -llama_tokenize(
                vocab, formatted_prompt.c_str(),
                static_cast<int32_t>(formatted_prompt.size()),
                nullptr, 0, true, true
        );

        if (token_count <= 0) {
            return "Failed to tokenize the prompt.";
        }

        const int safe_max_tokens = std::clamp(max_tokens, 1, 512);
        if (token_count + safe_max_tokens >= static_cast<int>(CONTEXT_SIZE)) {
            return "Prompt is too long for the current context size.";
        }

        std::vector<llama_token> tokens(token_count);
        if (llama_tokenize(
                vocab, formatted_prompt.c_str(),
                static_cast<int32_t>(formatted_prompt.size()),
                tokens.data(), static_cast<int32_t>(tokens.size()),
                true, true
        ) < 0) {
            return "Failed to tokenize the prompt.";
        }

        llama_context_params context_params = llama_context_default_params();
        context_params.n_ctx = CONTEXT_SIZE;
        context_params.n_batch = BATCH_SIZE;
        context_params.n_ubatch = BATCH_SIZE;

        const unsigned int hardware_threads = std::thread::hardware_concurrency();
        context_params.n_threads = std::clamp(
                static_cast<int>(hardware_threads == 0 ? 4 : hardware_threads) - 2,
                2, 6
        );
        context_params.n_threads_batch = context_params.n_threads;

        llama_context *context = llama_init_from_model(g_model, context_params);
        if (context == nullptr) {
            return "Failed to create the model context.";
        }

        llama_sampler *sampler =
                llama_sampler_chain_init(llama_sampler_chain_default_params());
        llama_sampler_chain_add(sampler, llama_sampler_init_min_p(0.05f, 1));
        llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.3f));
        llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));

        std::string response;
        llama_batch batch =
                llama_batch_get_one(tokens.data(), static_cast<int32_t>(tokens.size()));

        for (int generated = 0; generated < safe_max_tokens; ++generated) {
            if (llama_decode(context, batch) != 0) {
                log_error("llama_decode failed");
                break;
            }

            const llama_token token = llama_sampler_sample(sampler, context, -1);
            if (llama_vocab_is_eog(vocab, token)) {
                break;
            }

            int piece_size = llama_token_to_piece(vocab, token, nullptr, 0, 0, true);
            if (piece_size < 0) {
                std::vector<char> piece(static_cast<size_t>(-piece_size));
                piece_size = llama_token_to_piece(
                        vocab, token, piece.data(),
                        static_cast<int32_t>(piece.size()), 0, true
                );
                if (piece_size > 0) {
                    response.append(piece.data(), piece_size);
                }
            }

            llama_token next_token = token;
            batch = llama_batch_get_one(&next_token, 1);
        }

        llama_sampler_free(sampler);
        llama_free(context);
        return response;
    }

} // namespace

/*
 * extern "C":
 * C는 C++과 달리 Function Overloading을 지원하지 않으므로, linker가 function을 이름으로만 구분한다.
 * 그런데 C언어로 작성된 source code를 include 할 경우, C언어는 다형성을 지원하지 않으므로 mangling된
 * function의 원형을 알 수가 없어 Link 과정에서 Error가 발생한다. extern "C"는 Name Mangling을 하지
 * 않도록 하여 client C 코드가 C와 호환되는, 함수의 선언만을 갖고 있는 Header File 사용을 통해 C++의
 * function에 link할 수 있도록 한다.
 */
extern "C"
JNIEXPORT jboolean
JNICALL Java_com_safelaw_data_slm_LlamaBridge_loadModel(
        JNIEnv *env, jobject, jstring model_path
) {
    //g_mutex 잠금: 다른 스레드가 모델을 생성하거나 해제하는 중에 로드하지 못하게 한다.
    std::lock_guard<std::mutex> lock(g_mutex);
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "loadModel entered");

    if (!g_backend_initialized) {
        llama_log_set(llama_android_log, nullptr);
        llama_backend_init();
        g_backend_initialized = true;
        __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "backend initialized");
    }

    const char *path = env->GetStringUTFChars(model_path, nullptr);
    if (path == nullptr) {
        return JNI_FALSE;
    }

    release_model_locked();

    llama_model_params params = llama_model_default_params();
    params.n_gpu_layers = 0;
    params.use_mmap = true;

    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "loading model: %s", path);
    g_model = llama_model_load_from_file(path, params);
    env->ReleaseStringUTFChars(model_path, path);

    if (g_model == nullptr) {
        log_error("Failed to load GGUF model");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

extern "C"
JNIEXPORT jstring JNICALL
Java_com_safelaw_data_slm_LlamaBridge_generate(
        JNIEnv *env, jobject, jstring prompt, jint max_tokens
) {
    const char *prompt_chars = env->GetStringUTFChars(prompt, nullptr);
    if (prompt_chars == nullptr) {
        return env->NewStringUTF("Failed to read the prompt.");
    }

    const std::string prompt_string(prompt_chars);
    env->ReleaseStringUTFChars(prompt, prompt_chars);

    std::lock_guard<std::mutex> lock(g_mutex);
    const std::string result = generate_text(prompt_string, max_tokens);
    return env->NewStringUTF(result.c_str());
}

extern "C"
JNIEXPORT void JNICALL
Java_com_safelaw_data_slm_LlamaBridge_releaseModel(
        JNIEnv *, jobject
) {
    std::lock_guard<std::mutex> lock(g_mutex);
    release_model_locked();
}
