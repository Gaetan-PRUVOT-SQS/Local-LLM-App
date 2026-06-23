package com.gaetan.gemmchat.llm

enum class BackendChoice {
    NPU,
    GPU,
    CPU,
}

enum class EngineMode {
    MULTIMODAL,
    TEXT_ONLY,
}

data class EngineStatus(
    val isReady: Boolean = false,
    val isInitializing: Boolean = false,
    val mode: EngineMode = EngineMode.MULTIMODAL,
    val backend: BackendChoice = BackendChoice.GPU,
    val error: String? = null,
)