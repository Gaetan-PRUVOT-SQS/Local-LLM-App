package com.gaetan.localllmapp

import com.gaetan.localllmapp.data.ModelVariant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelVariantTest {

    @Test
    fun formatBytes_gigabytes() {
        assertEquals("2,6 Go", ModelVariant.formatBytes(2_588_147_712L))
        assertEquals("1,0 Go", ModelVariant.formatBytes(1_000_000_000L))
    }

    @Test
    fun formatBytes_megabytes() {
        assertEquals("500 Mo", ModelVariant.formatBytes(500_000_000L))
    }

    @Test
    fun q4_isRecommendedAndAvailable() {
        assertTrue(ModelVariant.Q4.recommended)
        assertTrue(ModelVariant.Q4.available)
        assertEquals(2_588_147_712L, ModelVariant.Q4.expectedSizeBytes)
    }

    @Test
    fun q4_hasIntegrityHash() {
        assertEquals(64, ModelVariant.Q4.sha256.length) // SHA-256 hex = 64 chars
    }

    @Test
    fun e4b_isNotAvailableYet() {
        assertTrue(!ModelVariant.E4B.available)
    }

    @Test
    fun bundled_isQ4() {
        assertEquals(ModelVariant.Q4, ModelVariant.bundled)
    }
}
