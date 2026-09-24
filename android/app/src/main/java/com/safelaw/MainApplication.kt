package com.safelaw

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost
import com.safelaw.data.slm.LlamaPackage

//db연동 실험 시 아래 주석 제거하고 실행해주세요
class MainApplication : Application(), ReactApplication {

  //private lateinit var dbPrototypeRunner: DbPrototypeRunner
  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      packageList =
        PackageList(this).packages.apply {
          // Packages that cannot be autolinked yet can be added manually here, for example:
          // add(MyReactNativePackage())
          add(LlamaPackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()
    loadReactNative(this)

    //dbPrototypeRunner = DbPrototypeRunner(this)
    //dbPrototypeRunner.run()
  }
}
