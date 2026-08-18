package az.simplesoft.tooliva.feature.clean.downloads

import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageFileClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadsAnalyzerRulesTest {
    @Test
    fun classifiesDownloadFileTypesWithoutReadingContents() {
        assertEquals(StorageCategory.APK, StorageFileClassifier.classify("installer.apk"))
        assertEquals(StorageCategory.ARCHIVE, StorageFileClassifier.classify("backup.7z"))
        assertEquals(StorageCategory.ARCHIVE, StorageFileClassifier.classify("backup.tar.gz"))
        assertEquals(StorageCategory.DOCUMENT, StorageFileClassifier.classify("report.pdf"))
        assertEquals(StorageCategory.DOCUMENT, StorageFileClassifier.classify("report.docx"))
        assertEquals(StorageCategory.DOCUMENT, StorageFileClassifier.classify("notes.txt", "text/plain"))
        assertEquals(StorageCategory.IMAGE, StorageFileClassifier.classify("photo.jpg", "image/jpeg"))
        assertEquals(StorageCategory.VIDEO, StorageFileClassifier.classify("clip.mp4", "video/mp4"))
        assertEquals(StorageCategory.AUDIO, StorageFileClassifier.classify("voice.m4a", "audio/mp4"))
        assertEquals(StorageCategory.OTHER, StorageFileClassifier.classify("mystery.bin"))
    }

    @Test
    fun ageAndSizeBoundariesAreInclusive() {
        val now = 10_000_000L
        val thirtyDays = 30 * DownloadsAnalyzerRules.DAY_MILLIS
        assertTrue(DownloadsAnalyzerRules.isOlderThan(now - thirtyDays, now, 30))
        assertFalse(DownloadsAnalyzerRules.isOlderThan(now - thirtyDays + 1, now, 30))
        assertTrue(DownloadsAnalyzerRules.isLargeEnough(100L * 1024L * 1024L, 100L * 1024L * 1024L))
        assertFalse(DownloadsAnalyzerRules.isLargeEnough(99L * 1024L * 1024L, 100L * 1024L * 1024L))
    }

}
