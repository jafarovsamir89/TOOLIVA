package az.simplesoft.tooliva.feature.clean.duplicates

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.nio.file.Files

class DuplicateFingerprintCacheTest {
    @Test fun persistsAndReusesOnlyUnchangedMetadata() {
        val directory = Files.createTempDirectory("tooliva-fingerprint-cache").toFile()
        try {
            val file = directory.resolve("fingerprints.txt")
            val first = DuplicateFingerprintCache(file)
            first.put("/storage/emulated/0/a.bin", 100L, 200L, "a".repeat(64))
            first.save()

            val second = DuplicateFingerprintCache(file)
            second.load()
            assertEquals("a".repeat(64), second.find("/storage/emulated/0/a.bin", 100L, 200L))
            assertNull(second.find("/storage/emulated/0/a.bin", 101L, 200L))
            assertNull(second.find("/storage/emulated/0/a.bin", 100L, 201L))
        } finally {
            directory.deleteRecursively()
        }
    }
}
