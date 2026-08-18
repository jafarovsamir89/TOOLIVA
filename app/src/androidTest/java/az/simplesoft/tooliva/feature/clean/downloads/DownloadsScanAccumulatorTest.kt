package az.simplesoft.tooliva.feature.clean.downloads

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadsScanAccumulatorTest {
    @Test
    fun malformedEntryAndWarningDoNotBreakProgressiveAccumulator() {
        val accumulator = DownloadsScanAccumulator()
        val malformed = StorageEntry(
            ref = Uri.parse("file:///Download/unknown"),
            name = "unknown",
            path = "",
            category = StorageCategory.OTHER,
            sizeBytes = 0L,
            modifiedAtMillis = 0L,
            mimeType = null,
            extension = null,
        )

        val added = runCatching { accumulator.add(malformed) }.isSuccess

        assertTrue(added)
        assertEquals(1, accumulator.snapshot().size)
        assertEquals("unknown", accumulator.snapshot().single().name)
    }

    @Test
    fun stateFiltersAgeSizeSearchAndCategory() {
        val now = 1_000_000_000L
        val oldApk = entry("old.apk", StorageCategory.APK, 600L * 1024L * 1024L, now - 200 * DownloadsAnalyzerRules.DAY_MILLIS)
        val newZip = entry("new.zip", StorageCategory.ARCHIVE, 120L * 1024L * 1024L, now - 2 * DownloadsAnalyzerRules.DAY_MILLIS)
        val state = DownloadsAnalyzerUiState(
            files = listOf(oldApk, newZip),
            nowMillis = now,
            categoryFilter = DownloadsCategoryFilter.APK,
            ageFilter = DownloadsAgeFilter(180, "180+ days"),
            sizeFilter = DownloadsSizeFilter(500L * 1024L * 1024L, "500 MB+"),
            sortOrder = StorageSortOrder.NAME,
            searchQuery = "old",
        )

        assertEquals(listOf(oldApk), state.visibleFiles)
        assertEquals(DownloadsCategoryFilter.MEDIA, DownloadsAnalyzerRules.analyzerCategory(entry("x.png", StorageCategory.IMAGE, 1, now)))
    }

    private fun entry(name: String, category: StorageCategory, bytes: Long, modified: Long) = StorageEntry(
        ref = Uri.parse("file:///Download/$name"),
        name = name,
        path = "/storage/emulated/0/Download/$name",
        category = category,
        sizeBytes = bytes,
        modifiedAtMillis = modified,
        mimeType = null,
        extension = name.substringAfterLast('.', ""),
    )
}
