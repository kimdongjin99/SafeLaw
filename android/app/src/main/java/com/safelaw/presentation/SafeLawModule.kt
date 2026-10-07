package com.safelaw.presentation

import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule
import com.safelaw.domain.AskLegalQuestionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

@ReactModule(name = SafeLawModule.NAME)
class SafeLawModule (
    reactContext: ReactApplicationContext,
    private val askLegalQuestionUseCase : AskLegalQuestionUseCase
) : ReactContextBaseJavaModule(reactContext){

    private val askingJob = SupervisorJob()

    private val askingScope = CoroutineScope(
        askingJob + Dispatchers.Default,
    )

    @ReactMethod
    fun ask(
        question : String,
        promise: Promise,
    ) {
        askingScope.launch {
            val answer = askLegalQuestionUseCase.ask(question)
            promise.resolve(answer)
        }
    }

    override fun invalidate() {
        askingScope.cancel()
        super.invalidate()
    }

    override fun getName(): String = NAME

    companion object {
        const val NAME = "SafeLawModule"
    }
}