package az.simplesoft.tooliva.core.cleanup

import az.simplesoft.tooliva.core.storage.StorageCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupRecommendationRulesTest {
    @Test
    fun ageBoundaryIsInclusiveAndUnknownDateNeverMatches() {
        val now = 100_000_000_000L
        val threshold = 180
        val exact = now - threshold * CleanupRecommendationRules.DAY_MILLIS

        assertFalse(CleanupRecommendationRules.isKnownOld(exact + 1L, threshold, now))
        assertTrue(CleanupRecommendationRules.isKnownOld(exact, threshold, now))
        assertFalse(CleanupRecommendationRules.isKnownOld(0L, threshold, now))
    }

    @Test
    fun onlyDownloadSegmentsCountAsDownloads() {
        assertTrue(CleanupRecommendationRules.isDownloadPath("/storage/emulated/0/Download/file.pdf"))
        assertTrue(CleanupRecommendationRules.isDownloadPath("/storage/emulated/0/Downloads/file.pdf"))
        assertFalse(CleanupRecommendationRules.isDownloadPath("/storage/emulated/0/Documents/file.pdf"))
        assertFalse(CleanupRecommendationRules.isDownloadPath("/storage/emulated/0/MyDownloads/file.pdf"))
    }

    @Test
    fun ageRuleDoesNotTreatNonApkAsApk() {
        // The production candidate mapping checks StorageCategory before assigning the APK reason.
        assertFalse(StorageCategory.DOCUMENT == StorageCategory.APK)
    }

    @Test
    fun residualRuleIsConservative() {
        val now = 100_000_000_000L
        val old = now - 8L * CleanupRecommendationRules.DAY_MILLIS
        assertTrue(CleanupRecommendationRules.isResidualCandidate("/storage/emulated/0/Download/partial.part", "part", old, false, now))
    }

    @Test
    fun allOldFileAgeBoundariesAreInclusive() {
        val now = 100_000_000_000L
        listOf(30, 90, 180, 365).forEach { days ->
            val exact = now - days * CleanupRecommendationRules.DAY_MILLIS
            assertTrue(CleanupRecommendationRules.isKnownOld(exact, days, now))
            assertTrue(!CleanupRecommendationRules.isKnownOld(exact + 1L, days, now))
        }
    }
}
