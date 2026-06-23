package com.gaetan.localllmapp.data

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

    /**
     * Modèle utilisable s'il est présent et d'une taille plausible. Tolérant pour accepter
     * aussi bien un modèle téléchargé qu'un fichier importé manuellement par l'utilisateur.
     */
    fun isModelReady(variant: ModelVariant = selectedVariant): Boolean {
        val file = modelFile(variant)
        return file.exists() && file.length() >= MIN_MODEL_BYTES
    }

    /** Un modèle (quel qu'il soit) est-il déjà présent dans le stockage interne ? */
    fun anyModelPresent(): Boolean = ModelVariant.entries.any { isModelReady(it) }

    companion object {
        /** Garde-fou : en dessous on considère le fichier incomplet/invalide. */
        const val MIN_MODEL_BYTES = 200_000_000L
    }

    fun deleteModel(variant: ModelVariant = selectedVariant) {
        modelFile(variant).delete()
        partialFile(variant).delete()
    }

    fun deleteAllModels() {
        ModelVariant.entries.forEach { deleteModel(it) }
    }
}