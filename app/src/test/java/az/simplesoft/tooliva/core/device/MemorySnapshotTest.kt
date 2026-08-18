package az.simplesoft.tooliva.core.device

import org.junit.Assert.assertEquals
import org.junit.Test

class MemorySnapshotTest {
    @Test
    fun usedEstimateNeverBecomesNegative() {
        val snapshot = MemorySnapshot(100L, 150L, false, 10L)
        assertEquals(0L, snapshot.usedEstimateBytes)
        assertEquals("Normal", snapshot.pressureLabel)
    }

    @Test
    fun lowMemoryIsReportedAsHighPressure() {
        assertEquals("High", MemorySnapshot(100L, 10L, true, 20L).pressureLabel)
    }
}
