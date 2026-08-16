package az.simplesoft.tooliva.core.media

import org.junit.Assert.assertEquals
import org.junit.Test

class CleanupResultTest {

    @Test
    fun canceledCleanupReportsNoChangedBytes() {
        val prepared = PreparedCleanupDeletion(
            requested = emptyList(),
            eligible = emptyList(),
            missingBeforeAction = emptyList(),
        )

        val result = CleanupResult.canceled(prepared)

        assertEquals(CleanupResultStatus.CANCELED, result.status)
        assertEquals(0, result.removedFromActiveCount)
        assertEquals(0L, result.freedBytes)
        assertEquals(0L, result.trashedBytes)
    }

    @Test
    fun partialCleanupKeepsVerifiedAndUnconfirmedTotalsSeparate() {
        val result = CleanupResult(
            status = CleanupResultStatus.PARTIAL,
            requestedCount = 3,
            requestedBytes = 6_000L,
            removedFromActiveCount = 2,
            removedFromActiveBytes = 4_000L,
            trashedCount = 1,
            trashedBytes = 1_000L,
            freedCount = 1,
            freedBytes = 3_000L,
            missingBeforeCount = 0,
            missingBeforeBytes = 0L,
            failedCount = 1,
            failedBytes = 2_000L,
            unchangedCount = 1,
            unchangedBytes = 2_000L,
        )

        assertEquals(4_000L, result.trashedBytes + result.freedBytes)
        assertEquals(2_000L, result.failedBytes)
    }
}
