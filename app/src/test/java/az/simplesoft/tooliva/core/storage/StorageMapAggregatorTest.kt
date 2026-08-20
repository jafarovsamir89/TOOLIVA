package az.simplesoft.tooliva.core.storage

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class StorageMapAggregatorTest {
    @Test
    fun syntheticTreeAggregatesBytesIntoFoldersWithoutDoubleCounting() {
        val root = "/storage/emulated/0"
        val aggregator = StorageMapAggregator(mapOf(root to "Internal storage"))
        aggregator.addFile(root, "$root/Download/archive.zip", 100L)
        aggregator.addFile(root, "$root/Pictures/Screenshots/shot.png", 200L)
        aggregator.addFile(root, "$root/Pictures/photo.jpg", 300L)

        val result = aggregator.build()
        val rootNode = result.roots.single()
        val pictures = rootNode.children.single { it.name == "Pictures" }
        val screenshots = pictures.children.single { it.name == "Screenshots" }

        assertEquals(3L, result.filesChecked)
        assertEquals(600L, result.bytesCounted)
        assertEquals(600L, rootNode.totalBytes)
        assertEquals(500L, pictures.totalBytes)
        assertEquals(200L, screenshots.totalBytes)
        assertEquals(3L, rootNode.fileCount)
        assertEquals(4L, result.foldersFound)
        assertEquals(83, pictures.percentOf(rootNode))
        assertNotNull(result.find("$root/Download"))
    }
}
