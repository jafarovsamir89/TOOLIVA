package az.simplesoft.tooliva.feature.clean

import az.simplesoft.tooliva.core.storage.StorageCategory
import java.util.EnumSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanerAnalysisRulesTest {
    private val now = 1_000_000_000_000L

    @Test
    fun oneFileCanBeShownInReviewCategoriesWithoutBeingCalledJunk() {
        val buckets = CleanerAnalysisRules.bucketsFor("/storage/emulated/0/Download/report.pdf", "report.pdf", StorageCategory.DOCUMENT, 1L, "pdf", now - 181L * 86_400_000L, false, now, 180)
        assertTrue(CleanerBucket.DOWNLOADS in buckets)
        assertTrue(CleanerBucket.DOCUMENTS in buckets)
        assertTrue(CleanerBucket.OLD_FILES in buckets)
    }

    @Test
    fun largeFilesIncludesNonMediaFiles() {
        val buckets = CleanerAnalysisRules.bucketsFor("/storage/emulated/0/Download/archive.zip", "archive.zip", StorageCategory.ARCHIVE, 100L * 1024L * 1024L, "zip", now, false, now, 180)
        assertTrue(CleanerBucket.LARGE_FILES in buckets)
        assertTrue(CleanerBucket.ARCHIVES in buckets)
    }

    @Test
    fun residualRuleRequiresDownloadLocationKnownAgeAndApprovedExtension() {
        assertTrue(CleanerAnalysisRules.isResidualCandidate("/storage/emulated/0/Download/video.part", "part", now - 8L * 86_400_000L, false, now))
        assertFalse(CleanerAnalysisRules.isResidualCandidate("/storage/emulated/0/Documents/video.part", "part", now - 8L * 86_400_000L, false, now))
        assertFalse(CleanerAnalysisRules.isResidualCandidate("/storage/emulated/0/Download/report.pdf", "pdf", now - 8L * 86_400_000L, false, now))
        assertFalse(CleanerAnalysisRules.isResidualCandidate("/storage/emulated/0/Download/new.part", "part", now - 6L * 86_400_000L, false, now))
    }

    @Test
    fun emptyFolderRulesRejectProtectedAndRootLikePaths() {
        assertTrue(CleanerAnalysisRules.isSafeEmptyFolderCandidate("/storage/emulated/0/Download/empty"))
        assertFalse(CleanerAnalysisRules.isSafeEmptyFolderCandidate("/storage/emulated/0"))
        assertFalse(CleanerAnalysisRules.isSafeEmptyFolderCandidate("/storage/emulated/0/Android/data"))
        assertFalse(CleanerAnalysisRules.isSafeEmptyFolderCandidate("/storage/emulated/0/Tooliva"))
    }
}
