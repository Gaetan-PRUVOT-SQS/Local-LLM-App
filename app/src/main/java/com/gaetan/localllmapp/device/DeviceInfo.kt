package com.gaetan.localllmapp.device

import android.os.Build
import com.gaetan.localllmapp.data.ModelVariant

enum class TensorGeneration {
    NONE,
    G3,
    G4,
    G5,
    UNKNOWN,
}

object DeviceInfo {
    val socModel: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

    val isPixelTensor: Boolean
        get() = socModel.contains("tensor", ignoreCase = true) ||
            Build.BOARD.contains("tensor", ignoreCase = true) ||
            Build.HARDWARE.contains("tensor", ignoreCase = true)

    val tensorGeneration: TensorGeneration
        get() = when {
            !isPixelTensor -> TensorGeneration.NONE
            matchesTensorGeneration("g5") -> TensorGeneration.G5
            matchesTensorGeneration("g4") -> TensorGeneration.G4
            matchesTensorGeneration("g3") -> TensorGeneration.G3
            else -> TensorGeneration.UNKNOWN
        }

    val supportsGemma4Npu: Boolean
        get() = tensorGeneration == TensorGeneration.G5

    fun availableModelVariants(): List<ModelVariant> {
        return if (supportsGemma4Npu) {
            ModelVariant.entries
        } else {
            listOf(ModelVariant.Q4)
        }
    }

    fun recommendedModelVariant(): ModelVariant {
        return if (supportsGemma4Npu) {
            ModelVariant.PIXEL_TENSOR_G5
        } else {
            ModelVariant.Q4
        }
    }

    fun recommendedBackendLabel(): String {
        return when {
            supportsGemma4Npu -> "NPU (Tensor G5)"
            isPixelTensor -> "GPU (Tensor ${tensorGeneration.name})"
            else -> "GPU"
        }
    }

    fun deviceSummary(): String {
        return when (tensorGeneration) {
            TensorGeneration.G3 ->
                "Pixel Tensor G3 (ex. Pixel 8 Pro) — Gemma 4 E2B supporté, GPU recommandé"
            TensorGeneration.G4 ->
                "Pixel Tensor G4 — Gemma 4 E2B supporté, GPU recommandé"
            TensorGeneration.G5 ->
                "Pixel Tensor G5 — Gemma 4 E2B + variante NPU disponible"
            TensorGeneration.UNKNOWN ->
                if (isPixelTensor) {
                    "Pixel Tensor — Gemma 4 E2B supporté via le modèle universel"
                } else {
                    "Appareil standard — Gemma 4 E2B via GPU/CPU"
                }
            TensorGeneration.NONE -> "Appareil non-Tensor — Gemma 4 E2B via GPU/CPU"
        }
    }

    private fun matchesTensorGeneration(generation: String): Boolean {
        val normalizedSoc = socModel.lowercase()
        val normalizedDevice = Build.DEVICE.lowercase()
        return normalizedSoc.contains("tensor_$generation") ||
            normalizedSoc.contains("_$generation") ||
            normalizedSoc.endsWith(generation) ||
            when (generation) {
                "g3" -> normalizedDevice in G3_DEVICES
                "g4" -> normalizedDevice in G4_DEVICES
                "g5" -> normalizedDevice in G5_DEVICES
                else -> false
            }
    }

    private val G3_DEVICES = setOf("shiba", "husky", "akita")
    private val G4_DEVICES = setOf("tokay", "comet", "caiman", "komodo", "ripcurrent")
    private val G5_DEVICES = setOf("frankel", "blazer", "mustang", "rango")
}