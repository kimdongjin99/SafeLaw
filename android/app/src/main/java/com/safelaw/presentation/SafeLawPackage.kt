package com.safelaw.presentation

import com.facebook.react.BaseReactPackage
import com.facebook.react.bridge.NativeModule
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.module.model.ReactModuleInfo
import com.facebook.react.module.model.ReactModuleInfoProvider
import com.safelaw.domain.AskLegalQuestionUseCase
import com.safelaw.domain.state.GenerationStateManager

class SafeLawPackage(
    private val askLegalQuestionUseCase: AskLegalQuestionUseCase,
    private val generationStateManager: GenerationStateManager,
) : BaseReactPackage() {

    override fun getModule(
        name: String,
        reactContext: ReactApplicationContext
    ): NativeModule? = when (name) {
        SafeLawModule.NAME ->
            SafeLawModule(
                reactContext,
                askLegalQuestionUseCase,
                generationStateManager,
        )
        else -> null
    }

    override fun getReactModuleInfoProvider(): ReactModuleInfoProvider =
        ReactModuleInfoProvider{
            mapOf(
                SafeLawModule.NAME to ReactModuleInfo(
                    SafeLawModule.NAME,
                    SafeLawModule::class.java.name,
                    false,
                    false,
                    false,
                    false,
                )
            )
        }
}
