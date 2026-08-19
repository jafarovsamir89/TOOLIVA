package az.simplesoft.tooliva.feature.clean.duplicates

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class FileHasherTest {
    @Test fun sameContentHasSameShaAndDifferentContentDoesNot() {
        withTempDirectory { directory ->
            val first = File(directory, "a.txt").apply { writeText("hello") }
            val second = File(directory, "b.txt").apply { writeText("hello") }
            val third = File(directory, "c.txt").apply { writeText("world") }
            assertEquals(FileHasher.sha256(first), FileHasher.sha256(second))
            assertFalse(FileHasher.sha256(first) == FileHasher.sha256(third))
            assertTrue(runBlocking { ExactFileVerifier.verify(first, second) })
            assertFalse(runBlocking { ExactFileVerifier.verify(first, third) })
        }
    }

    @Test fun hashingUsesStreamingPathForSyntheticLargeFile() {
        withTempDirectory { directory ->
            val file = File(directory, "large.bin")
            file.outputStream().use { output ->
                repeat(4_096) { output.write(ByteArray(1024) { (it % 251).toByte() }) }
            }
            val hash = FileHasher.sha256(file)
            assertEquals(64, hash.length)
            assertTrue(hash.all { it in "0123456789abcdef" })
        }
    }

    @Test fun changingFileBeforeEntryHashIsRejected() {
        withTempDirectory { directory ->
            val file = File(directory, "changing.txt").apply { writeText("before") }
            val expectedSize = file.length()
            val expectedModified = file.lastModified()
            file.writeText("after with a different size")
            val result = runBlocking { FileHasher.hash(file, expectedSize, expectedModified) }
            assertTrue(result is FileHashResult.Invalid)
        }
    }

    private fun withTempDirectory(block: (File) -> Unit) {
        val directory = Files.createTempDirectory("tooliva-duplicates-test").toFile()
        try { block(directory) } finally { directory.deleteRecursively() }
    }
}
