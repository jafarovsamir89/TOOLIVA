package az.simplesoft.tooliva.core.storage

import org.junit.Assert.assertEquals
import org.junit.Test

class StorageAccessStateTest {
    @Test
    fun grantedAllFilesAccessUsesFullMode() {
        assertEquals(
            StorageAccessMode.FULL,
            StorageAccessState(fullStorageSupported = true, allFilesAccessGranted = true).mode,
        )
    }

    @Test
    fun deniedOrUnsupportedAccessUsesLimitedMode() {
        assertEquals(
            StorageAccessMode.LIMITED,
            StorageAccessState(fullStorageSupported = true, allFilesAccessGranted = false).mode,
        )
        assertEquals(
            StorageAccessMode.LIMITED,
            StorageAccessState(fullStorageSupported = false, allFilesAccessGranted = false).mode,
        )
    }
}

