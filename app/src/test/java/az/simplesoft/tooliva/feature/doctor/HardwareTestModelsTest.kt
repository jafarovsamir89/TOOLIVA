package az.simplesoft.tooliva.feature.doctor

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test

class HardwareTestModelsTest {
    @Test fun touchGridDeduplicatesCellsAndClampsEdges() {
        var coverage = TouchCoverage(columns = 2, rows = 2)
        coverage = coverage.touchCell(0, 0).touchCell(0, 0).touchPoint(100f, 100f, 100f, 100f)
        assertEquals(2, coverage.count)
        assertFalse(coverage.complete)
        coverage = coverage.touchCell(1, 0).touchCell(0, 1).touchPoint(1000f, 1000f, 100f, 100f)
        assertTrue(coverage.complete)
        assertEquals(0, coverage.reset().count)
    }

    @Test fun proximityRequiresFarNearFarSequence() {
        val tracker = ProximitySequenceTracker()
        tracker.onNear(false)
        tracker.onNear(true)
        assertFalse(tracker.completed)
        tracker.onNear(false)
        assertTrue(tracker.completed)
    }

    @Test fun microphoneAmplitudeDistinguishesSilenceAndSignal() {
        assertEquals(0f, microphoneAmplitude(ShortArray(32)), 0.0001f)
        val tone = ShortArray(32) { if (it % 2 == 0) 16_000 else -16_000 }
        assertTrue(microphoneAmplitude(tone) > 0.4f)
        assertTrue(sensorValuesChanged(listOf(0f, 0f), listOf(0.5f, 0f)))
    }
}
