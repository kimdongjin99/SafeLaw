package com.safelaw

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.safelaw.data.db.LegalSearchRepository
import com.safelaw.data.slm.TextGenerationRepository
import com.safelaw.domain.AskLegalQuestionUseCase
import com.safelaw.presentation.SafeLawPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainApplication : Application(), ReactApplication {

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

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          add(
            SafeLawPackage(
              askLegalQuestionUseCase = askLegalQuestionUseCase,
            ),
          )
        },
    )
  }

  override fun onCreate() {
    super.onCreate()

    //앱 실행 시작과 동시에 비동기로 모델 로딩 시작
    CoroutineScope(Dispatchers.IO).launch {
      //llamaRepository.loadModel()
    }

    loadReactNative(this)

  }
}
