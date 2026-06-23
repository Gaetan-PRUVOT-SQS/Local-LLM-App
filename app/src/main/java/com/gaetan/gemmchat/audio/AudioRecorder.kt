package com.gaetan.gemmchat.audio

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

class AudioRecorder(
    private val context: Context,
) {
    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null

    val isRecording: Boolean
        get() = recorder != null

    fun start(): File {
        stopInternal(release = true)

        val recordingsDir = File(context.cacheDir, "recordings").apply { mkdirs() }
        val file = File(recordingsDir, "voice_${System.currentTimeMillis()}.m4a")

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        mediaRecorder.setAudioSamplingRate(16_000)
        mediaRecorder.setAudioEncodingBitRate(96_000)
        mediaRecorder.setOutputFile(file.absolutePath)
        mediaRecorder.prepare()
        mediaRecorder.start()

        recorder = mediaRecorder
        outputFile = file
        return file
    }

    fun stop(): File? {
        val file = outputFile
        stopInternal(release = true)
        return file?.takeIf { it.exists() && it.length() > 0 }
    }

    fun cancel() {
        val file = outputFile
        stopInternal(release = true)
        file?.delete()
    }

    private fun stopInternal(release: Boolean) {
        val activeRecorder = recorder ?: return
        try {
            activeRecorder.stop()
        } catch (_: IllegalStateException) {
            // Recorder stopped too quickly.
        }
        if (release) {
            activeRecorder.release()
        }
        recorder = null
        outputFile = null
    }
}