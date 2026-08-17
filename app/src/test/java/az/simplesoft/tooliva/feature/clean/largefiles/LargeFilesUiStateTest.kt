package az.simplesoft.tooliva.feature.clean.largefiles

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LargeFilesUiStateTest {

    @Test
    fun emptyStateHasNoSelectionOrBytes() {
        val state = LargeFilesUiState()

        assertTrue(state.selectedFiles.isEmpty())
        assertEquals(0L, state.selectedBytes)
    }
}
