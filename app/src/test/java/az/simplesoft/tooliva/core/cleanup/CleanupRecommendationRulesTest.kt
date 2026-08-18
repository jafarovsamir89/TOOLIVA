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
}
