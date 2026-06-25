package com.gaetan.gemmchat.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

data class ExtractProgress(
    val bytesCopied: Long,
    val totalBytes: Long,
    val percent: Float,
)

sealed class InstallResult {
    data object Success : InstallResult()
    data class Error(val message: String) : InstallResult()
}

/**
 * Télécharge le modèle `.litertlm` depuis Hugging Face (public, non-gated) vers le
 * stockage privé, avec **reprise** (HTTP Range) si un téléchargement partiel existe.
 * Réutilise [ExtractProgress] / [InstallResult] de l'installeur.
 */
class ModelDownloader(
    private val modelRepository: ModelRepository,
) {
    suspend fun ensureDownloaded(
        variant: ModelVariant = modelRepository.selectedVariant,
        onProgress: (ExtractProgress) -> Unit = {},
    ): InstallResult = withContext(Dispatchers.IO) {
        val total = variant.expectedSizeBytes
        if (modelRepository.isModelReady(variant)) {
            onProgress(ExtractProgress(total, total, 1f))
            return@withContext InstallResult.Success
        }

        val target = modelRepository.modelFile(variant)
        val partial = modelRepository.partialFile(variant)

        var existing = if (partial.exists()) partial.length() else 0L
        if (existing >= total) { // partiel corrompu / trop grand → on repart
            partial.delete()
            existing = 0L
        }

        try {
            val conn = openFollowingRedirects(variant.downloadUrl, rangeFrom = existing)
            val code = conn.responseCode
            // Si on a demandé une reprise mais le serveur renvoie 200, il ignore le
            // Range → on repart de zéro.
            val resumed = existing > 0 && code == HttpURLConnection.HTTP_PARTIAL
            if (existing > 0 && code == HttpURLConnection.HTTP_OK) existing = 0L
            if (code != HttpURLConnection.HTTP_OK && code != HttpURLConnection.HTTP_PARTIAL) {
                conn.disconnect()
                return@withContext InstallResult.Error("Téléchargement refusé (HTTP $code).")
            }

            var copied = if (resumed) existing else 0L
            onProgress(ExtractProgress(copied, total, copied.toFloat() / total))

            conn.inputStream.use { input ->
                FileOutputStream(partial, /* append = */ resumed).use { output ->
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        coroutineContext.ensureActive() // annulable
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        copied += read
                        onProgress(ExtractProgress(copied, total, (copied.toFloat() / total).coerceIn(0f, 1f)))
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            val size = partial.length()
            if (size != total) {
                // téléchargement incomplet : on garde le partiel pour permettre une reprise
                return@withContext InstallResult.Error(
                    "Téléchargement incomplet ($size/$total octets). Réessaie pour reprendre.",
                )
            }

            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            InstallResult.Success
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Throwable) {
            Log.w(TAG, "Échec du téléchargement du modèle", e)
            InstallResult.Error("Téléchargement impossible. Vérifie ta connexion puis réessaie.")
        }
    }

    /**
     * Suit les redirections manuellement (HF resolve → CDN) en **conservant** l'en-tête
     * Range, que HttpURLConnection perdrait lors d'un follow automatique.
     */
    private fun openFollowingRedirects(urlString: String, rangeFrom: Long): HttpURLConnection {
        var current = urlString
        var hops = 0
        while (true) {
            val conn = (URL(current).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 60_000
                instanceFollowRedirects = false
                setRequestProperty("User-Agent", "GemmaChat-Android")
                if (rangeFrom > 0) setRequestProperty("Range", "bytes=$rangeFrom-")
            }
            val code = conn.responseCode
            if (code in 300..399) {
                val location = conn.getHeaderField("Location")
                conn.disconnect()
                if (location.isNullOrBlank() || ++hops > 5) error("Trop de redirections")
                current = location
                continue
            }
            return conn
        }
    }

    companion object {
        private const val TAG = "ModelDownloader"
    }
}
