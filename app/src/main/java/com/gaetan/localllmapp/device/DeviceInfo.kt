package com.gaetan.localllmapp.device

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import com.gaetan.localllmapp.data.ModelVariant

enum class TensorGeneration {
    NONE,
    G3,
    G4,
    G5,
    UNKNOWN,
}

/** Famille de SoC, pour une expérience universelle (pas seulement Pixel/Tensor). */
enum class SocVendor {
    GOOGLE_TENSOR,
    QUALCOMM,
    MEDIATEK,
    SAMSUNG_EXYNOS,
    OTHER,
}

enum class CompatibilityVerdict {
    EXCELLENT,
    OK,
    INSUFFICIENT,
}

/** Une ligne du scan de compatibilité (écran 01). */
data class CompatibilityCheck(
    val title: String,
    val detail: String,
    val passed: Boolean,
)

/** Résultat du scan appareil affiché sur l'écran de compatibilité. */
data class CompatibilityReport(
    val verdict: CompatibilityVerdict,
    val totalRamBytes: Long,
    val freeStorageBytes: Long,
    val socLabel: String,
    val npuAvailable: Boolean,
    val checks: List<CompatibilityCheck>,
) {
    val verdictLabel: String
        get() = when (verdict) {
            CompatibilityVerdict.EXCELLENT -> "Compatibilité excellente"
            CompatibilityVerdict.OK -> "Compatible"
            CompatibilityVerdict.INSUFFICIENT -> "Ressources insuffisantes"
        }
}

object DeviceInfo {
    val socModel: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.ifBlank { Build.HARDWARE }
        } else {
            Build.HARDWARE
        }

    private val socManufacturer: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Build.SOC_MANUFACTURER.orEmpty() else ""

    private fun haystack(): String = listOf(
        socManufacturer, socModel, Build.HARDWARE, Build.BOARD, Build.MANUFACTURER,
    ).joinToString(" ").lowercase()

    /** Détection multi-vendeur du SoC. */
    val socVendor: SocVendor
        get() {
            val h = haystack()
            return when {
                "tensor" in h -> SocVendor.GOOGLE_TENSOR
                "qualcomm" in h || "snapdragon" in h || "qcom" in h ||
                    Regex("\\bsm[0-9]{3,4}\\b").containsMatchIn(h) -> SocVendor.QUALCOMM
                "mediatek" in h || "dimensity" in h || "helio" in h ||
                    Regex("\\bmt[0-9]{3,4}\\b").containsMatchIn(h) -> SocVendor.MEDIATEK
                "exynos" in h || Regex("\\bs5e[0-9]{3,4}\\b").containsMatchIn(h) ||
                    ("samsung" in h && "universal" in h) -> SocVendor.SAMSUNG_EXYNOS
                else -> SocVendor.OTHER
            }
        }

    val isPixelTensor: Boolean
        get() = socVendor == SocVendor.GOOGLE_TENSOR

    /** NPU matériel détecté (informatif, multi-vendeur). */
    val hasNpu: Boolean
        get() = when (socVendor) {
            SocVendor.GOOGLE_TENSOR -> tensorGeneration != TensorGeneration.NONE
            SocVendor.QUALCOMM, SocVendor.MEDIATEK, SocVendor.SAMSUNG_EXYNOS -> true
            SocVendor.OTHER -> false
        }

    /** Libellé du moteur NPU selon le vendeur. */
    val npuLabel: String
        get() = when (socVendor) {
            SocVendor.GOOGLE_TENSOR -> "NPU Tensor"
            SocVendor.QUALCOMM -> "NPU Hexagon (QNN)"
            SocVendor.MEDIATEK -> "APU MediaTek"
            SocVendor.SAMSUNG_EXYNOS -> "NPU Exynos"
            SocVendor.OTHER -> ""
        }

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

    /** Espace de stockage interne disponible (octets). */
    fun freeStorageBytes(context: Context): Long {
        val stat = StatFs(context.filesDir.absolutePath)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    /** Scan complet RAM/CPU/stockage/NPU pour l'écran de compatibilité (01). */
    fun compatibilityReport(
        context: Context,
        targetVariant: ModelVariant = recommendedModelVariant(),
    ): CompatibilityReport {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val totalRam = memInfo.totalMem

        val stat = StatFs(context.filesDir.absolutePath)
        val freeStorage = stat.availableBlocksLong * stat.blockSizeLong

        val needBytes = targetVariant.expectedSizeBytes
        // marge : extraction + runtime. RAM minimale ~3 Go, stockage = taille modèle + 10%.
        val ramOk = totalRam >= 3_000_000_000L
        val ramAmple = totalRam >= 6_000_000_000L
        val storageOk = freeStorage >= (needBytes * 1.1).toLong()
        val npu = hasNpu

        val checks = listOf(
            CompatibilityCheck(
                title = "Mémoire vive",
                detail = "${ModelVariant.formatBytes(totalRam)} · " +
                    if (ramAmple) "largement suffisant" else if (ramOk) "suffisant" else "insuffisant",
                passed = ramOk,
            ),
            CompatibilityCheck(
                title = "Processeur",
                detail = socLabel + if (npu && npuLabel.isNotEmpty()) " · $npuLabel" else "",
                passed = true,
            ),
            CompatibilityCheck(
                title = "Stockage libre",
                detail = "${ModelVariant.formatBytes(freeStorage)} · besoin de ${ModelVariant.formatBytes(needBytes)}",
                passed = storageOk,
            ),
        )

        val verdict = when {
            !ramOk || !storageOk -> CompatibilityVerdict.INSUFFICIENT
            ramAmple -> CompatibilityVerdict.EXCELLENT
            else -> CompatibilityVerdict.OK
        }

        return CompatibilityReport(
            verdict = verdict,
            totalRamBytes = totalRam,
            freeStorageBytes = freeStorage,
            socLabel = socLabel,
            npuAvailable = npu,
            checks = checks,
        )
    }

    /** Libellé lisible du SoC, multi-vendeur (fallback sur le modèle de l'appareil). */
    val socLabel: String
        get() = when (socVendor) {
            SocVendor.GOOGLE_TENSOR -> when (tensorGeneration) {
                TensorGeneration.NONE, TensorGeneration.UNKNOWN -> "Google Tensor"
                else -> "Google Tensor ${tensorGeneration.name}"
            }
            SocVendor.QUALCOMM -> "Qualcomm Snapdragon"
            SocVendor.MEDIATEK -> "MediaTek Dimensity"
            SocVendor.SAMSUNG_EXYNOS -> "Samsung Exynos"
            SocVendor.OTHER -> socModel.takeIf { it.isNotBlank() && !it.equals("unknown", true) }
                ?: "${Build.MANUFACTURER} ${Build.MODEL}".trim()
        }

    /** Tous les appareils utilisent le modèle universel ; la variante NPU Tensor reste optionnelle. */
    fun availableModelVariants(): List<ModelVariant> = ModelVariant.entries.toList()

    /** Modèle universel par défaut sur **tous** les appareils. */
    fun recommendedModelVariant(): ModelVariant = ModelVariant.Q4

    fun recommendedBackendLabel(): String = when {
        supportsGemma4Npu -> "NPU ($socLabel)"
        hasNpu -> "GPU (NPU détecté)"
        else -> "GPU"
    }

    fun deviceSummary(): String {
        val ram = socLabel
        return when (socVendor) {
            SocVendor.GOOGLE_TENSOR ->
                if (supportsGemma4Npu) "$ram — Gemma 4 E2B, accélération NPU disponible"
                else "$ram — Gemma 4 E2B via GPU/CPU"
            SocVendor.QUALCOMM, SocVendor.MEDIATEK, SocVendor.SAMSUNG_EXYNOS ->
                "$ram — Gemma 4 E2B via GPU (${npuLabel} détecté)"
            SocVendor.OTHER -> "$ram — Gemma 4 E2B via GPU/CPU"
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