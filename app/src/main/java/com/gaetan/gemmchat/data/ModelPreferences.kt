package com.gaetan.gemmchat.data

import android.content.Context

class ModelPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getSelectedVariant(): ModelVariant {
        val stored = prefs.getString(KEY_VARIANT, null)
        if (stored == "STANDARD") return ModelVariant.Q4
        return ModelVariant.entries.find { it.name == stored }
            ?: ModelVariant.Q4
    }

    fun saveSelectedVariant(variant: ModelVariant) {
        prefs.edit().putString(KEY_VARIANT, variant.name).apply()
    }

    companion object {
        private const val PREFS_NAME = "gemma_chat_prefs"
        private const val KEY_VARIANT = "model_variant"
    }
}