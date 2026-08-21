package az.simplesoft.tooliva.core.files

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.ZipOutputStream

class ArchiveDocumentServiceTest {
    @Test
    fun zipPreviewAndExtractionStayInsideDestination() {
        val root = java.nio.file.Files.createTempDirectory("tooliva-archive-test").toFile()
        try {
            val source = File(root, "note.txt").apply { writeText("hello") }
            val archive = File(root, "sample.zip")
            ZipOutputStream(archive.outputStream()).use { zip ->
                zip.putNextEntry(java.util.zip.ZipEntry(source.name))
                zip.write(source.readBytes())
                zip.closeEntry()
            }
            val service = ArchiveDocumentService()
            val preview = service.preview(archive) as FilePreview.Archive
            assertEquals(listOf("note.txt"), preview.entries)
            val destination = File(root, "out").apply { mkdirs() }
            assertEquals(1, service.extractZip(archive, destination))
            assertTrue(File(destination, "note.txt").readText() == "hello")
        } finally {
            root.deleteRecursively()
        }
    }
}
