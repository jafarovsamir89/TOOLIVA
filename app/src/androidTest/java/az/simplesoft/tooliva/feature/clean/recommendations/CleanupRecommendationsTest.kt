package az.simplesoft.tooliva.feature.clean.recommendations

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import az.simplesoft.tooliva.core.cleanup.CleanupReasonId
import az.simplesoft.tooliva.core.cleanup.CleanupRecommendationRules
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CleanupRecommendationsTest {
    @Test
    fun rulesRejectRecentOutsideUnknownAndDirectories() {
        val now = 2_000_000_000_000L
        assertEquals(null, CleanupRecommendationRules.candidateFor(entry("recent.apk", StorageCategory.APK, now - 179 * CleanupRecommendationRules.DAY_MILLIS, "/Download"), 180, now))
        assertEquals(null, CleanupRecommendationRules.candidateFor(entry("outside.pdf", StorageCategory.DOCUMENT, now - 200 * CleanupRecommendationRules.DAY_MILLIS, "/Documents"), 180, now))
        assertEquals(null, CleanupRecommendationRules.candidateFor(entry("unknown.pdf", StorageCategory.DOCUMENT, 0L, "/Download"), 180, now))
        assertEquals(null, CleanupRecommendationRules.candidateFor(entry("folder", StorageCategory.OTHER, now - 200 * CleanupRecommendationRules.DAY_MILLIS, "/Download", directory = true), 180, now))
    }

    @Test
    fun oldApkGetsSpecificReasonAndIsNeverSelectedByDefault() {
        val now = 2_000_000_000_000L
        val candidate = CleanupRecommendationRules.candidateFor(
            entry("installer.apk", StorageCategory.APK, now - 180 * CleanupRecommendationRules.DAY_MILLIS, "/Download"),
            180,
            now,
        ) ?: error("expected APK candidate")

        assertEquals(CleanupReasonId.OLD_APK_INSTALLER, candidate.reason.id)
        assertFalse(candidate.defaultSelected)
    }

    @Test
    fun apkIsNotDuplicatedAsGenericOldDownloadAndTotalsMatchCandidates() {
        val now = 2_000_000_000_000L
        val apk = entry("installer.apk", StorageCategory.APK, now - 365 * CleanupRecommendationRules.DAY_MILLIS, "/Downloads")
        val pdf = entry("report.pdf", StorageCategory.DOCUMENT, now - 365 * CleanupRecommendationRules.DAY_MILLIS, "/Downloads")
        val accumulator = CleanupCandidateAccumulator()
        accumulator.add(apk, 180, now)
        accumulator.add(apk, 180, now)
        accumulator.add(pdf, 180, now)

        val candidates = accumulator.snapshot()
        assertEquals(2, candidates.size)
        assertEquals(CleanupReasonId.OLD_APK_INSTALLER, candidates.first().reason.id)
        assertEquals(2, candidates.count { it.entry.ref.toString().contains("Downloads") })
        assertTrue(candidates.sumOf { it.entry.sizeBytes } == apk.sizeBytes + pdf.sizeBytes)
    }

    private fun entry(name: String, category: StorageCategory, modified: Long, parent: String, directory: Boolean = false) = StorageEntry(
        ref = Uri.parse("file://$parent/$name"),
        name = name,
        path = "$parent/$name",
        category = category,
        sizeBytes = 10L,
        modifiedAtMillis = modified,
        mimeType = null,
        extension = name.substringAfterLast('.', ""),
        isDirectory = directory,
    )
}
