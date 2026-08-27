buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        // 이 줄이 반드시 있어야 MyObjectBox를 생성하는 플러그인이 로드됩니다.
        classpath("io.objectbox:objectbox-gradle-plugin:4.0.3")
    }
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("io.objectbox")
}

android {
    namespace = "com.example.safelaw"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.safelaw"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            pickFirsts += "**/libobjectbox-jni.so"
        }
    }

    // 9만 개 데이터 처리 시 빌드 속도 및 메모리 에러 방지
    aaptOptions {
        noCompress("onnx", "json", "csv")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    // ObjectBox 4.0.3 버전 통일
    val objectboxVersion = "4.0.3"
    implementation("io.objectbox:objectbox-android:$objectboxVersion")
    kapt("io.objectbox:objectbox-processor:$objectboxVersion")

    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.9.2")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.1")

    // androidTest 구역에서도 Gson과 ObjectBox, JUnit을 쓸 수 있게 허가하는 진짜 정품 코드
    androidTestImplementation("com.google.code.gson:gson:2.10.1")
    androidTestImplementation("io.objectbox:objectbox-android:3.8.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")

    // 로컬 test 구역에서도 JUnit을 완벽하게 인식하도록 보장하는 정품 코드
    testImplementation("junit:junit:4.13.2")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // 코루틴 (네트워크 통신용)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}