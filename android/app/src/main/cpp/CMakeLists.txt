# llama.cpp와 SafeLaw JNI 브리지를 Android 공유 라이브러리로 빌드
cmake_minimum_required(VERSION 3.22.1)

project(appmodules LANGUAGES C CXX)

set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Preserve React Native's New Architecture app module and autolinked codegen
# libraries when adding the separate llama.cpp JNI target.
include(${REACT_ANDROID_DIR}/cmake-utils/ReactNative-application.cmake)

set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_COMMON OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TESTS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_TOOLS OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_SERVER OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_APP OFF CACHE BOOL "" FORCE)
set(LLAMA_BUILD_UI OFF CACHE BOOL "" FORCE)
set(GGML_OPENMP OFF CACHE BOOL "" FORCE)
set(GGML_NATIVE OFF CACHE BOOL "" FORCE)
set(GGML_CPU_KLEIDIAI OFF CACHE BOOL "" FORCE)

add_subdirectory(third-party/llama.cpp build-llama EXCLUDE_FROM_ALL)

add_library(llama_jni SHARED llama-jni/llama_jni.cpp)

target_include_directories(
        llama_jni
        PRIVATE
        third-party/llama.cpp/include
        third-party/llama.cpp/ggml/include
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
        llama_jni
        llama
        ${android-lib}
        ${log-lib}
)
