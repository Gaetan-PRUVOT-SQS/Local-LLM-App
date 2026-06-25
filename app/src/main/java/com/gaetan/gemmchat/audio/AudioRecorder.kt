package com.gaetan.gemmchat.audio

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.io.File
import java.io.RandomAccessFile

/**
 * Enregistre en **WAV PCM 16 bits, 16 kHz, mono**.
 *
 * Important : LiteRT-LM décode l'audio via miniaudio, qui ne gère que WAV/FLAC/MP3
 * (PAS l'AAC/M4A). Un MediaRecorder en MPEG_4/AAC provoque
 * « Failed to initialize miniaudio decoder, error code: -10 ». On passe donc par
 * AudioRecord (PCM brut) et on écrit nous-mêmes l'en-tête WAV.
 */
class AudioRecorder(
    private val context: Context,
) {
    private var recorder: AudioRecord? = null
    private var thread: Thread? = null
    private var outputFile: File? = null

    @Volatile
    private var active = false

    val isRecording: Boolean
        get() = active

    fun start(): File {
        stopInternal(deleteFile = true)

        val dir = File(context.filesDir, "recordings").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val file = File(dir, "voice_${System.currentTimeMillis()}.wav")

        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBuf <= 0) error("Configuration micro non supportée")
        val bufSize = maxOf(minBuf, SAMPLE_RATE * 2) // ~1 s

        @Suppress("MissingPermission") // RECORD_AUDIO demandée côté UI
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufSize,
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            error("Micro indisponible")
        }

        recorder = audioRecord
        outputFile = file
        active = true
        audioRecord.startRecording()

        thread = Thread {
            runCatching {
                RandomAccessFile(file, "rw").use { raf ->
                    raf.setLength(0)
                    raf.write(ByteArray(WAV_HEADER_SIZE)) // placeholder, complété à la fin
                    val buffer = ByteArray(bufSize)
                    var dataBytes = 0L
                    while (active) {
                        val read = audioRecord.read(buffer, 0, buffer.size)
                        if (read > 0) {
                            raf.write(buffer, 0, read)
                            dataBytes += read
                        }
                    }
                    raf.seek(0)
                    raf.write(wavHeader(dataBytes.toInt()))
                }
            }.onFailure { Log.w(TAG, "Erreur d'écriture WAV", it) }
        }.also { it.start() }

        return file
    }

    fun stop(): File? {
        val file = outputFile
        stopInternal(deleteFile = false)
        return file?.takeIf { it.exists() && it.length() > WAV_HEADER_SIZE }
    }

    fun cancel() {
        val file = outputFile
        stopInternal(deleteFile = false)
        file?.delete()
    }

    private fun stopInternal(deleteFile: Boolean) {
        active = false
        thread?.let { runCatching { it.join(2000) } }
        thread = null
        recorder?.let { rec ->
            runCatching { if (rec.recordingState == AudioRecord.RECORDSTATE_RECORDING) rec.stop() }
            runCatching { rec.release() }
        }
        recorder = null
        if (deleteFile) outputFile?.delete()
        outputFile = null
    }

    /** En-tête WAV canonique (44 octets) pour PCM 16 bits mono. */
    private fun wavHeader(dataLen: Int): ByteArray {
        val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
        val blockAlign = CHANNELS * BITS_PER_SAMPLE / 8
        val totalLen = dataLen + WAV_HEADER_SIZE - 8
        val h = ByteArray(WAV_HEADER_SIZE)
        fun str(off: Int, s: String) { s.forEachIndexed { i, c -> h[off + i] = c.code.toByte() } }
        fun le32(off: Int, v: Int) {
            h[off] = (v and 0xff).toByte()
            h[off + 1] = ((v shr 8) and 0xff).toByte()
            h[off + 2] = ((v shr 16) and 0xff).toByte()
            h[off + 3] = ((v shr 24) and 0xff).toByte()
        }
        fun le16(off: Int, v: Int) {
            h[off] = (v and 0xff).toByte()
            h[off + 1] = ((v shr 8) and 0xff).toByte()
        }
        str(0, "RIFF"); le32(4, totalLen); str(8, "WAVE")
        str(12, "fmt "); le32(16, 16); le16(20, 1) /* PCM */; le16(22, CHANNELS)
        le32(24, SAMPLE_RATE); le32(28, byteRate); le16(32, blockAlign); le16(34, BITS_PER_SAMPLE)
        str(36, "data"); le32(40, dataLen)
        return h
    }

    companion object {
        private const val TAG = "AudioRecorder"
        private const val SAMPLE_RATE = 16_000
        private const val CHANNELS = 1
        private const val BITS_PER_SAMPLE = 16
        private const val WAV_HEADER_SIZE = 44
    }
}
