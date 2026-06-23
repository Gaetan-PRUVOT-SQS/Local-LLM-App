package com.gaetan.localllmapp.data

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

    /** true une fois l'onboarding (compatibilité → choix → téléchargement) terminé. */
    fun isOnboardingComplete(): Boolean = prefs.getBoolean(KEY_ONBOARDING_DONE, false)

    fun setOnboardingComplete(done: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, done).apply()
    }

    fun isWifiOnly(): Boolean = prefs.getBoolean(KEY_WIFI_ONLY, true)

    fun setWifiOnly(wifiOnly: Boolean) {
        prefs.edit().putBoolean(KEY_WIFI_ONLY, wifiOnly).apply()
    }

    /** Licence Gemma acceptée par l'utilisateur (requis avant téléchargement). */
    fun isLicenseAccepted(): Boolean = prefs.getBoolean(KEY_LICENSE, false)

    fun setLicenseAccepted(accepted: Boolean) {
        prefs.edit().putBoolean(KEY_LICENSE, accepted).apply()
    }

    companion object {
        private const val PREFS_NAME = "gemma_chat_prefs"
        private const val KEY_VARIANT = "model_variant"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_WIFI_ONLY = "wifi_only"
        private const val KEY_LICENSE = "license_accepted"
    }
}