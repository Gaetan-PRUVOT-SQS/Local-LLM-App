package com.gaetan.gemmchat.data

object BundledModel {
    const val FILE_NAME = "gemma-4-E2B-it.litertlm"
    const val EXPECTED_SIZE_BYTES = 2_588_147_712L
    const val DISPLAY_NAME = "Gemma 4 E2B Q4"
    const val HF_REPO = "litert-community/gemma-4-E2B-it-litert-lm"
}

enum class ModelVariant(
    val fileName: String,
    val expectedSizeBytes: Long,
    val label: String,
    val description: String,
    val supportsNpu: Boolean,
) {
    Q4(
        fileName = BundledModel.FILE_NAME,
        expectedSizeBytes = BundledModel.EXPECTED_SIZE_BYTES,
        label = BundledModel.DISPLAY_NAME,
        description = "QAT 2/4/8 bits — multimodal, GPU/CPU (Pixel 8 Pro inclus)",
        supportsNpu = false,
    ),
    PIXEL_TENSOR_G5(
        fileName = "gemma-4-E2B-it_Google_Tensor_G5.litertlm",
        expectedSizeBytes = 3_953_110_901L,
        label = "Pixel Tensor G5 (NPU)",
        description = "NPU — Pixel 10 / Tensor G5 uniquement",
        supportsNpu = true,
    );

    /** URL de téléchargement direct (public, non-gated) sur Hugging Face. */
    val downloadUrl: String
        get() = "https://huggingface.co/${BundledModel.HF_REPO}/resolve/main/$fileName"

    companion object {
        val bundled: ModelVariant = Q4
    }
}