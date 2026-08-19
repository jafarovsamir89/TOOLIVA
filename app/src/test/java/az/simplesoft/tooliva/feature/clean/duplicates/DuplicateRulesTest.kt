package az.simplesoft.tooliva.feature.clean.duplicates

import az.simplesoft.tooliva.core.storage.StorageCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateRulesTest {
    @Test fun uniqueZeroByteAndDirectoriesNeverBecomeHashCandidates() {
        val candidates = DuplicateRules.candidateGroups(
            listOf(
                metadata("unique", 10),
                metadata("same-a", 20),
                metadata("same-b", 20),
                metadata("empty-a", 0),
                metadata("empty-b", 0),
                metadata("folder", 20, directory = true),
            ),
        )
        assertEquals(listOf(listOf("same-a", "same-b")), candidates.map { it.map(DuplicateMetadata::path) })
    }

    @Test fun recoverableBytesLeaveOneCopy() {
        assertEquals(100L, DuplicateRules.recoverableBytes(100, 2))
        assertEquals(200L, DuplicateRules.recoverableBytes(100, 3))
        assertEquals(0L, DuplicateRules.recoverableBytes(100, 1))
    }

    @Test fun keepThisCopySelectsOnlyOtherCopies() {
        val paths = listOf("a", "b", "c", "d", "e")
        val selected = DuplicateRules.keepThisCopy(paths, "c", emptySet())
        assertFalse("c" in selected)
        assertEquals(4, selected.size)
    }

    @Test fun selectingLastCopyIsPrevented() {
        val paths = listOf("a", "b")
        assertTrue(DuplicateRules.canSelect("a", paths, emptySet()))
        assertFalse(DuplicateRules.canSelect("b", paths, setOf("a")))
        assertTrue(DuplicateRules.canSelect("a", paths, setOf("a")))
    }

    private fun metadata(path: String, size: Long, directory: Boolean = false) = DuplicateMetadata(
        path = path,
        sizeBytes = size,
        modifiedAtMillis = 1L,
        category = StorageCategory.OTHER,
        isDirectory = directory,
    )
}
