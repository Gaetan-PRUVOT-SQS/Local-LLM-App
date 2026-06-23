package com.gaetan.localllmapp.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.FileOutputStream
import java.io.IOException

data class ExtractProgress(
    val bytesCopied: Long,
    val totalBytes: Long,
    val percent: Float,
)

sealed class InstallResult {
    data object Success : InstallResult()
    data class Error(val message: String) : InstallResult()
}

class BundledModelInstaller(
    private val context: Context,
    private val modelRepository: ModelRepository,
) {
    suspend fun ensureInstalled(
        variant: ModelVariant = modelRepository.selectedVariant,
        onProgress: (ExtractProgress) -> Unit = {},
    ): InstallResult = withContext(Dispatchers.IO) {
        if (modelRepository.isModelReady(variant)) {
            onProgress(
                ExtractProgress(
                    bytesCopied = variant.expectedSizeBytes,
                    totalBytes = variant.expectedSizeBytes,
                    percent = 1f,
                ),
            )
            return@withContext InstallResult.Success
        }

        val manifest = loadManifest()
            ?: return@withContext InstallResult.Error(
                "Manifest des chunks absent dans l'APK. Recompilez avec ./scripts/build_bundled_apk.sh",
            )

        if (manifest.fileName != variant.fileName) {
            return@withContext InstallResult.Error(
                "Chunk embarqué incompatible: ${manifest.fileName} (attendu: ${variant.fileName})",
            )
        }

        val totalBytes = manifest.expectedSizeBytes
        val target = modelRepository.modelFile(variant)
        val partial = modelRepository.partialFile(variant)
        if (partial.exists()) partial.delete()

        try {
            FileOutputStream(partial).use { output ->
                var copied = 0L
                onProgress(ExtractProgress(0, totalBytes, 0f))

                for (chunkName in manifest.chunks) {
                    context.assets.open(chunkName).use { input ->
                        val buffer = ByteArray(1024 * 1024)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            output.write(buffer, 0, read)
                            copied += read
                            onProgress(
                                ExtractProgress(
                                    bytesCopied = copied,
                                    totalBytes = totalBytes,
                                    percent = copied.toFloat() / totalBytes,
                                ),
                            )
                        }
                    }
                }
            }

            val extractedSize = partial.length()
            if (extractedSize != variant.expectedSizeBytes) {
                partial.delete()
                return@withContext InstallResult.Error(
                    "Taille incorrecte après assemblage: $extractedSize octets (attendu: ${variant.expectedSizeBytes}).",
                )
            }

            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }

            InstallResult.Success
        } catch (e: IOException) {
            partial.delete()
            InstallResult.Error("Assemblage du modèle impossible: ${e.message ?: "erreur inconnue"}")
        }
    }

    private fun loadManifest(): ChunkManifest? = try {
        context.assets.open(MANIFEST_ASSET).use { stream ->
            val json = JSONObject(stream.bufferedReader().readText())
            ChunkManifest(
                fileName = json.getString("fileName"),
                expectedSizeBytes = json.getLong("expectedSizeBytes"),
                chunks = buildList {
                    val array = json.getJSONArray("chunks")
                    for (i in 0 until array.length()) {
                        add(array.getString(i))
                    }
                },
            )
        }
    } catch (_: IOException) {
        null
    }

    private data class ChunkManifest(
        val fileName: String,
        val expectedSizeBytes: Long,
        val chunks: List<String>,
    )

    companion object {
        private const val MANIFEST_ASSET = "manifest.json"
    }
}