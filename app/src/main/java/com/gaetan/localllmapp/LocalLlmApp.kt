package com.gaetan.localllmapp

import android.app.Application
import android.util.Log
import java.io.File

/**
 * Application : enregistre un handler de crash qui persiste la stacktrace dans
 * `filesDir/last_crash.txt` (utile pour le support, sans dépendance externe type Crashlytics).
 */
class LocalLlmApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            runCatching {
                File(filesDir, "last_crash.txt").writeText(
                    buildString {
                        append("time=").append(System.currentTimeMillis()).append('\n')
                        append("thread=").append(thread.name).append('\n')
                        append("version=").append(BuildConfig.VERSION_NAME).append('\n')
                        append('\n')
                        append(Log.getStackTraceString(throwable))
                    },
                )
            }
            previous?.uncaughtException(thread, throwable)
        }
    }
}
