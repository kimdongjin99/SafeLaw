package com.safelaw.domain.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GenerationStateManager {

    private val _currentStage =
        MutableStateFlow<GenerationStage?>(null)

    val currentStage: StateFlow<GenerationStage?> =
        _currentStage.asStateFlow()

    fun changeStage(stage: GenerationStage) {
        _currentStage.value = stage
    }
}