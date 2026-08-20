package az.simplesoft.tooliva.feature.clean.swipe

import android.net.Uri
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CleanupSwipeRulesTest {
    @Test
    fun decisionsSelectBytesAndUndoRestoresPreviousCard() {
        val first = entry("/Download/large.zip", 300L)
        val second = entry("/Download/photo.jpg", 200L)
        val session = CleanupSwipeSession(CleanupSwipeCategory.DOWNLOADS, listOf(first, second))

        val reviewed = session.applyDecision(SwipeDecision.DELETE).applyDecision(SwipeDecision.KEEP)
        assertEquals(2, reviewed.decidedCount)
        assertEquals(300L, reviewed.selectedDeleteBytes)
        assertTrue(reviewed.isComplete)

        val undone = reviewed.undoLast()
        assertEquals(second.path, undone.current?.path)
        assertEquals(1, undone.decidedCount)
        assertEquals(300L, undone.selectedDeleteBytes)
    }

    @Test
    fun categoryRulesAreReviewOnlyAndLargeMeansAtLeastOneHundredMb() {
        val screenshot = entry("/Pictures/Screenshots/Screenshot_1.png", 1L, StorageCategory.IMAGE)
        val largeDocument = entry("/Download/report.pdf", 100L * 1024L * 1024L, StorageCategory.DOCUMENT)

        assertTrue(CleanupSwipeCategory.SCREENSHOTS.matches(screenshot))
        assertTrue(CleanupSwipeCategory.LARGE_FILES.matches(largeDocument))
        assertFalse(CleanupSwipeCategory.VIDEOS.matches(screenshot))
    }

    private fun entry(path: String, bytes: Long, category: StorageCategory = StorageCategory.OTHER) = StorageEntry(
        ref = Uri.parse("file://$path"),
        name = path.substringAfterLast('/'),
        path = path,
        category = category,
        sizeBytes = bytes,
        modifiedAtMillis = 1L,
        mimeType = null,
        extension = null,
    )
}
