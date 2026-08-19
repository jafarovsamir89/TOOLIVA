package az.simplesoft.tooliva.feature.files

import az.simplesoft.tooliva.core.storage.StorageSortOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FileManagerRulesTest {
    @Test fun validatesNamesWithoutAllowingTraversal() {
        assertNull(FileManagerRules.validateName("report.pdf"))
        assertEquals("Name cannot contain a path separator.", FileManagerRules.validateName("../secret"))
        assertEquals("This name is reserved.", FileManagerRules.validateName(".."))
    }

    @Test fun sortsFoldersBeforeFilesAndUsesRequestedOrder() {
        val file = entry("z.txt", 100, false)
        val folder = entry("a-folder", 0, true)
        val newer = entry("new.txt", 200, false)
        assertEquals(listOf(folder, newer, file), FileManagerRules.sortedItems(listOf(file, newer, folder), StorageSortOrder.NEWEST))
    }

    @Test fun keepsBothNamePreservingExtension() {
        assertEquals("photo (2).jpg", FileManagerRules.keepBothName("photo.jpg", setOf("photo.jpg", "photo (1).jpg")))
    }

    @Test fun selectedBytesOnlyCountsVisibleSelectedEntries() {
        val one = entry("one.bin", 10, false)
        val two = entry("two.bin", 20, false)
        assertEquals(20L, FileManagerRules.selectedBytesItems(listOf(one, two), setOf(two.path)))
    }

    private fun entry(name: String, size: Long, folder: Boolean) = FileManagerItem(
        name = name, path = "/storage/emulated/0/$name", sizeBytes = size, modifiedAtMillis = size, isDirectory = folder,
    )
}
