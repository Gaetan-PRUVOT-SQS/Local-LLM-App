package com.gaetan.localllmapp.llm

import android.content.Context
import android.util.Log
import com.gaetan.localllmapp.data.ModelVariant
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.SamplerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

class LlmEngine(
    private val context: Context,
) : Closeable {
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var conversation: Conversation? = null

    var status: EngineStatus = EngineStatus()
        private set

    suspend fun initialize(
        modelFile: File,
        modelVariant: ModelVariant,
        preferredBackend: BackendChoice = BackendChoice.GPU,
    ): EngineStatus = mutex.withLock {
        closeInternal()

        status = EngineStatus(isInitializing = true, backend = preferredBackend)
        withContext(Dispatchers.IO) {
            @OptIn(ExperimentalApi::class)
            ExperimentalFlags.enableSpeculativeDecoding = true

            val cacheDir = context.cacheDir.path
            val modelPath = modelFile.absolutePath
            val nativeLibraryDir = context.applicationInfo.nativeLibraryDir

            val attempts = buildAttemptOrder(
                preferred = preferredBackend,
                modelVariant = modelVariant,
            )
            var lastError: Throwable? = null

            for (attempt in attempts) {
                try {
                    val config = buildConfig(
                        modelPath = modelPath,
                        cacheDir = cacheDir,
                        nativeLibraryDir = nativeLibraryDir,
                        attempt = attempt,
                    )
                    val newEngine = Engine(config)
                    newEngine.initialize()
                    engine = newEngine
                    conversation = newEngine.createConversation(
                        ConversationConfig(
                            systemInstruction = com.google.ai.edge.litertlm.Contents.of(
                                "Tu es Gemma, un assistant IA utile et concis. Réponds en français et vouvoie toujours l'utilisateur, sauf demande contraire.",
                            ),
                            samplerConfig = SamplerConfig(
                                topK = 40,
                                topP = 0.95,
                                temperature = 0.8,
                            ),
                        ),
                    )
                    status = EngineStatus(
                        isReady = true,
                        isInitializing = false,
                        mode = attempt.mode,
                        backend = attempt.backend,
                    )
                    return@withContext status
                } catch (t: Throwable) {
                    lastError = t
                    Log.w(TAG, "Engine init failed for $attempt", t)
                }
            }

            status = EngineStatus(
                isReady = false,
                isInitializing = false,
                error = lastError?.message ?: "Impossible d'initialiser le moteur LiteRT-LM.",
            )
            status
        }
    }

    suspend fun getConversation(): Conversation? = mutex.withLock { conversation }

    override fun close() {
        kotlinx.coroutines.runBlocking {
            mutex.withLock { closeInternal() }
        }
    }

    private fun closeInternal() {
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
        status = EngineStatus()
    }

    private data class InitAttempt(
        val backend: BackendChoice,
        val mode: EngineMode,
    )

    private fun buildAttemptOrder(
        preferred: BackendChoice,
        modelVariant: ModelVariant,
    ): List<InitAttempt> {
        val backends = buildList {
            if (modelVariant.supportsNpu && preferred == BackendChoice.NPU) add(BackendChoice.NPU)
            when (preferred) {
                BackendChoice.NPU -> {
                    if (!contains(BackendChoice.NPU)) add(BackendChoice.NPU)
                    add(BackendChoice.GPU)
                    add(BackendChoice.CPU)
                }
                BackendChoice.GPU -> {
                    add(BackendChoice.GPU)
                    add(BackendChoice.CPU)
                }
                BackendChoice.CPU -> add(BackendChoice.CPU)
            }
        }.distinct()

        val attempts = mutableListOf<InitAttempt>()
        for (backend in backends) {
            attempts += InitAttempt(backend, EngineMode.MULTIMODAL)
        }
        for (backend in backends) {
            attempts += InitAttempt(backend, EngineMode.TEXT_ONLY)
        }
        return attempts.distinct()
    }

    private fun buildConfig(
        modelPath: String,
        cacheDir: String,
        nativeLibraryDir: String,
        attempt: InitAttempt,
    ): EngineConfig {
        val backend = resolveBackend(attempt.backend, nativeLibraryDir)

        return when (attempt.mode) {
            EngineMode.MULTIMODAL -> when (attempt.backend) {
                BackendChoice.NPU -> EngineConfig(
                    modelPath = modelPath,
                    backend = backend,
                    visionBackend = Backend.GPU(),
                    audioBackend = Backend.CPU(),
                    cacheDir = cacheDir,
                )
                else -> EngineConfig(
                    modelPath = modelPath,
                    backend = backend,
                    visionBackend = backend,
                    audioBackend = Backend.CPU(),
                    cacheDir = cacheDir,
                )
            }
            EngineMode.TEXT_ONLY -> EngineConfig(
                modelPath = modelPath,
                backend = if (attempt.backend == BackendChoice.NPU) {
                    Backend.NPU(nativeLibraryDir = nativeLibraryDir)
                } else {
                    Backend.CPU()
                },
                cacheDir = cacheDir,
            )
        }
    }

    private fun resolveBackend(
        choice: BackendChoice,
        nativeLibraryDir: String,
    ): Backend {
        return when (choice) {
            BackendChoice.NPU -> Backend.NPU(nativeLibraryDir = nativeLibraryDir)
            BackendChoice.GPU -> Backend.GPU()
            BackendChoice.CPU -> Backend.CPU()
        }
    }

    companion object {
        private const val TAG = "LlmEngine"
    }
}