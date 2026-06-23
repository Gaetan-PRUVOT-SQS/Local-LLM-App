package com.gaetan.gemmchat.data

import android.content.Context
import java.io.File

class ModelRepository(
    private val context: Context,
    private val preferences: ModelPreferences,
) {
    private val modelsDir: File
        get() = File(context.filesDir, "models").also { it.mkdirs() }

    val selectedVariant: ModelVariant
        get() = preferences.getSelectedVariant()

    fun setSelectedVariant(variant: ModelVariant) {
        preferences.saveSelectedVariant(variant)
    }

    fun modelFile(variant: ModelVariant = selectedVariant): File =
        File(modelsDir, variant.fileName)

    fun partialFile(variant: ModelVariant = selectedVariant): File =
        File(modelsDir, "${variant.fileName}.partial")

    fun isModelReady(variant: ModelVariant = selectedVariant): Boolean {
        val file = modelFile(variant)
        return file.exists() && file.length() == variant.expectedSizeBytes
    }

    fun deleteModel(variant: ModelVariant = selectedVariant) {
        modelFile(variant).delete()
        partialFile(variant).delete()
    }

    fun deleteAllModels() {
        ModelVariant.entries.forEach { deleteModel(it) }
    }
}