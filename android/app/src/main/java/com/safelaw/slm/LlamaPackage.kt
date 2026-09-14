// LlamaModule을 React Native 런타임에 네이티브 모듈로 등록
package com.safelaw.slm

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider

class LlamaPackage : BaseReactPackage() {
    override fun getModule(
        name: String,
        reactContext: ReactApplicationContext,
    ): NativeModule? = when (name) {
        LlamaModule.NAME -> LlamaModule(reactContext)
        else -> null
    }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider =
        ReactModuleInfoProvider {
            mapOf(
                LlamaModule.NAME to ReactModuleInfo(
                    LlamaModule.NAME,
                    LlamaModule::class.java.name,
                    false,
                    false,
                    false,
                    false,
                ),
            )
        }
}
