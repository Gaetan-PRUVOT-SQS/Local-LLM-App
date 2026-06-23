package com.gaetan.localllmapp.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.coroutineContext

/** Progression d'un téléchargement de modèle (écran 03). */
data class DownloadProgress(
    val bytesCopied: Long,
    val totalBytes: Long,
    val percent: Float,
    val speedBytesPerSec: Long,
    val etaSeconds: Long,
)

sealed class DownloadResult {
    data object Success : DownloadResult()
    /** Réseau requis absent (ex. Wi-Fi only mais pas de Wi-Fi). */
    data class WaitingForNetwork(val message: String) : DownloadResult()
    data class Error(val message: String) : DownloadResult()
}

/**
 * Télécharge le `.litertlm` depuis [ModelVariant.downloadUrl] vers le stockage interne,
 * de façon **résumable** (HTTP Range, équivalent `curl -C -`). La mise en pause se fait en
 * annulant la coroutine appelante : le fichier `.partial` est conservé et la reprise repart
 * de son offset.
 */
class ModelDownloader(
    private val context: Context,
    private val modelRepository: ModelRepository,
) {
    suspend fun download(
        variant: ModelVariant,
        wifiOnly: Boolean,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadResult = withContext(Dispatchers.IO) {
        if (modelRepository.isModelReady(variant)) {
            onProgress(DownloadProgress(variant.expectedSizeBytes, variant.expectedSizeBytes, 1f, 0, 0))
            return@withContext DownloadResult.Success
        }
        if (!variant.available) {
            return@withContext DownloadResult.Error("Modèle « ${variant.label} » pas encore disponible.")
        }
        if (!hasUsableNetwork(wifiOnly)) {
            return@withContext DownloadResult.WaitingForNetwork(
                if (wifiOnly) "En attente du Wi‑Fi…" else "Aucune connexion réseau.",
            )
        }

        val target = modelRepository.modelFile(variant)
        val partial = modelRepository.partialFile(variant)
        val existing = if (partial.exists()) partial.length() else 0L

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(variant.downloadUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 30_000
                readTimeout = 30_000
                instanceFollowRedirects = true
                if (existing > 0L) setRequestProperty("Range", "bytes=$existing-")
            }
            connection.connect()

            val code = connection.responseCode
            if (code !in 200..299) {
                return@withContext DownloadResult.Error(
                    "Téléchargement refusé (HTTP $code). Le modèle est peut-être protégé par licence.",
                )
            }
            val resuming = code == HttpURLConnection.HTTP_PARTIAL && existing > 0L
            if (!resuming && existing > 0L) partial.delete()

            val reportedLength = connection.contentLengthLong.takeIf { it > 0 } ?: -1L
            val totalBytes = when {
                resuming && reportedLength > 0 -> existing + reportedLength
                reportedLength > 0 -> reportedLength
                else -> variant.expectedSizeBytes
            }

            var copied = if (resuming) existing else 0L
            val startMs = System.currentTimeMillis()
            val startBytes = copied
            var lastEmitMs = 0L

            connection.inputStream.use { input ->
                FileOutputStream(partial, resuming).use { output ->
                    val buffer = ByteArray(1 shl 20) // 1 Mo
                    while (true) {
                        coroutineContext.ensureActive() // pause = annulation → .partial conservé
                        val read = input.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        copied += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmitMs >= 120 || copied >= totalBytes) {
                            lastEmitMs = now
                            val elapsedSec = ((now - startMs) / 1000.0).coerceAtLeast(0.001)
                            val speed = ((copied - startBytes) / elapsedSec).toLong()
                            val remaining = (totalBytes - copied).coerceAtLeast(0)
                            val eta = if (speed > 0) remaining / speed else 0L
                            onProgress(
                                DownloadProgress(
                                    bytesCopied = copied,
                                    totalBytes = totalBytes,
                                    percent = if (totalBytes > 0) copied.toFloat() / totalBytes else 0f,
                                    speedBytesPerSec = speed,
                                    etaSeconds = eta,
                                ),
                            )
                        }
                    }
                    output.flush()
                }
            }

            val finalSize = partial.length()
            if (finalSize != variant.expectedSizeBytes) {
                // Tolérance : certains miroirs n'ont pas exactement la taille attendue.
                if (finalSize < variant.expectedSizeBytes / 2) {
                    partial.delete()
                    return@withContext DownloadResult.Error(
                        "Fichier incomplet ($finalSize octets). Réessayez.",
                    )
                }
            }
            verifyAndFinalize(variant, partial, target)
                ?: DownloadResult.Success
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel // pause : on garde le .partial pour reprendre
        } catch (e: IOException) {
            DownloadResult.Error("Réseau interrompu: ${e.message ?: "erreur"} — la reprise est possible.")
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Vérifie l'intégrité (SHA-256) puis renomme atomiquement `.partial` → cible.
     * Retourne un [DownloadResult.Error] en cas d'échec, ou null si tout est OK.
     */
    private fun verifyAndFinalize(variant: ModelVariant, partial: java.io.File, target: java.io.File): DownloadResult.Error? {
        if (variant.sha256.isNotBlank()) {
            val actual = sha256Of(partial)
            if (!actual.equals(variant.sha256, ignoreCase = true)) {
                partial.delete()
                return DownloadResult.Error(
                    "Échec de vérification d'intégrité (empreinte SHA-256 invalide). Le fichier est corrompu ou altéré.",
                )
            }
        }
        if (target.exists()) target.delete()
        if (!partial.renameTo(target)) {
            partial.copyTo(target, overwrite = true)
            partial.delete()
        }
        return null
    }

    private fun sha256Of(file: java.io.File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(1 shl 20)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /**
     * Importe un modèle **déjà présent sur le téléphone** (sélectionné via le sélecteur de
     * fichiers) en le copiant dans le stockage interne de l'app. Aucun réseau requis.
     */
    suspend fun importLocal(
        variant: ModelVariant,
        uri: Uri,
        onProgress: (DownloadProgress) -> Unit = {},
    ): DownloadResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val total = querySize(uri) ?: variant.expectedSizeBytes
        val target = modelRepository.modelFile(variant)
        val partial = modelRepository.partialFile(variant)
        if (partial.exists()) partial.delete()

        try {
            val input = resolver.openInputStream(uri)
                ?: return@withContext DownloadResult.Error("Fichier illisible.")
            input.use { stream ->
                FileOutputStream(partial).use { output ->
                    val buffer = ByteArray(1 shl 20)
                    var copied = 0L
                    val startMs = System.currentTimeMillis()
                    var lastEmitMs = 0L
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = stream.read(buffer)
                        if (read == -1) break
                        output.write(buffer, 0, read)
                        copied += read
                        val now = System.currentTimeMillis()
                        if (now - lastEmitMs >= 120 || copied >= total) {
                            lastEmitMs = now
                            val elapsed = ((now - startMs) / 1000.0).coerceAtLeast(0.001)
                            val speed = (copied / elapsed).toLong()
                            val eta = if (speed > 0) (total - copied).coerceAtLeast(0) / speed else 0L
                            onProgress(
                                DownloadProgress(copied, total, if (total > 0) copied.toFloat() / total else 0f, speed, eta),
                            )
                        }
                    }
                    output.flush()
                }
            }
            val size = partial.length()
            if (size < ModelRepository.MIN_MODEL_BYTES) {
                partial.delete()
                return@withContext DownloadResult.Error(
                    "Fichier trop petit (${ModelVariant.formatBytes(size)}). Choisissez un .litertlm valide.",
                )
            }
            if (target.exists()) target.delete()
            if (!partial.renameTo(target)) {
                partial.copyTo(target, overwrite = true)
                partial.delete()
            }
            DownloadResult.Success
        } catch (cancel: kotlinx.coroutines.CancellationException) {
            throw cancel
        } catch (e: Exception) {
            partial.delete()
            DownloadResult.Error("Import impossible: ${e.message ?: "erreur"}.")
        }
    }

    private fun querySize(uri: Uri): Long? {
        return try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { c ->
                val idx = c.getColumnIndex(OpenableColumns.SIZE)
                if (c.moveToFirst() && idx >= 0 && !c.isNull(idx)) c.getLong(idx) else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun hasUsableNetwork(wifiOnly: Boolean): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return false
        return if (wifiOnly) caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) else true
    }
}
