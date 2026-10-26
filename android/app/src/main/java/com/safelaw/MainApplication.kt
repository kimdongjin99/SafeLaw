package com.safelaw

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeHost
import com.facebook.react.ReactPackage
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.load
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.facebook.react.defaults.DefaultReactNativeHost
import com.facebook.soloader.SoLoader
import com.safelaw.data.db.LegalSearchRepository
import com.safelaw.data.slm.TextGenerationRepository
import com.safelaw.domain.AskLegalQuestionUseCase
import com.safelaw.presentation.SafeLawPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainApplication : Application(), ReactApplication {

  // 온디바이스 법률 AI 관련 의존성 주입
  private val dbRepository by lazy {
    LegalSearchRepository()
  }

  private val slmRepository by lazy {
    TextGenerationRepository(applicationContext)
  }

  private val askLegalQuestionUseCase by lazy {
    AskLegalQuestionUseCase(
      db = dbRepository,
      slm = slmRepository,
    )
  }

  override val reactNativeHost: ReactNativeHost =
      object : DefaultReactNativeHost(this) {
        override fun getPackages(): List<ReactPackage> =
            PackageList(this).packages.apply {
              // SafeLaw 커스텀 패키지 및 유즈케이스 추가
              add(
                SafeLawPackage(
                  askLegalQuestionUseCase = askLegalQuestionUseCase,
                )
              )
            }

        override fun getJSMainModuleName(): String = "index"

        override fun getUseDeveloperSupport(): Boolean = BuildConfig.DEBUG

        override val isNewArchEnabled: Boolean = BuildConfig.IS_NEW_ARCHITECTURE_ENABLED
        override val isHermesEnabled: Boolean = BuildConfig.IS_HERMES_ENABLED
      }

  override val reactHost: ReactHost
    get() = getDefaultReactHost(applicationContext, reactNativeHost)

  override fun onCreate() {
    super.onCreate()
    SoLoader.init(this, false)
    if (BuildConfig.IS_NEW_ARCHITECTURE_ENABLED) {
      load()
    }

    // 앱 실행 시작과 동시에 비동기로 모델 로딩 시작
    CoroutineScope(Dispatchers.IO).launch {
      //llamaRepository.loadModel()
    }
  }
}