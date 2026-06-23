package com.gaetan.localllmapp.data

object BundledModel {
    const val FILE_NAME = "gemma-4-E2B-it.litertlm"
    const val EXPECTED_SIZE_BYTES = 2_588_147_712L
    const val DISPLAY_NAME = "Gemma 4 E2B Q4"
    const val HF_REPO = "litert-community/gemma-4-E2B-it-litert-lm"
}

/**
 * Modèles téléchargeables au 1er lancement. [downloadUrl] est volontairement agnostique :
 * pointe par défaut sur Hugging Face, mais peut être remplacé par n'importe quelle URL
 * directe (repo HF public, CDN, bucket) si le repo source est gated.
 */
enum class ModelVariant(
    val fileName: String,
    val expectedSizeBytes: Long,
    val label: String,
    val description: String,
    val supportsNpu: Boolean,
    val hfRepo: String,
    val tags: List<String>,
    /** SHA-256 attendu (vide → vérification d'intégrité ignorée). */
    val sha256: String = "",
    /** false → affiché mais non téléchargeable (« bientôt »). */
    val available: Boolean = true,
    val recommended: Boolean = false,
) {
    Q4(
        fileName = BundledModel.FILE_NAME,
        expectedSizeBytes = BundledModel.EXPECTED_SIZE_BYTES,
        label = "Gemma 4 E2B",
        description = "Édition Edge · 2 Md effectifs",
        supportsNpu = false,
        hfRepo = BundledModel.HF_REPO,
        tags = listOf("128K contexte", "~45 tok/s", "Texte · Vision · Audio"),
        sha256 = "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c",
        recommended = true,
    ),
    PIXEL_TENSOR_G5(
        fileName = "gemma-4-E2B-it_Google_Tensor_G5.litertlm",
        expectedSizeBytes = 3_953_110_901L,
        label = "Pixel Tensor G5 (NPU)",
        description = "NPU — Pixel 10 / Tensor G5 uniquement",
        supportsNpu = true,
        hfRepo = "litert-community/gemma-4-E2B-it-litert-lm",
        tags = listOf("NPU QNN", "128K contexte", "Texte · Vision · Audio"),
    ),
    E4B(
        fileName = "gemma-4-E4B-it.litertlm",
        expectedSizeBytes = 4_300_000_000L,
        label = "Gemma 4 E4B",
        description = "Qualité supérieure · 4 Md effectifs",
        supportsNpu = false,
        hfRepo = "litert-community/gemma-4-E4B-it-litert-lm",
        tags = listOf("Plus précis", "Plus lourd"),
        available = false,
    );

    /**
     * URL directe du fichier modèle. Surchargeable au build (miroir non-gated) via
     * `MODEL_URL_OVERRIDE` ; sinon résolution Hugging Face par défaut.
     */
    val downloadUrl: String
        get() {
            val override = com.gaetan.localllmapp.BuildConfig.MODEL_URL_OVERRIDE
            return if (this == Q4 && override.isNotBlank()) {
                override
            } else {
                "https://huggingface.co/$hfRepo/resolve/main/$fileName"
            }
        }

    val sizeLabel: String
        get() = formatBytes(expectedSizeBytes)

    companion object {
        val bundled: ModelVariant = Q4

        fun formatBytes(bytes: Long): String {
            val go = bytes.toDouble() / 1_000_000_000.0
            return if (go >= 1.0) {
                String.format(java.util.Locale.FRANCE, "%.1f Go", go)
            } else {
                String.format(java.util.Locale.FRANCE, "%.0f Mo", bytes.toDouble() / 1_000_000.0)
            }
        }
    }
}