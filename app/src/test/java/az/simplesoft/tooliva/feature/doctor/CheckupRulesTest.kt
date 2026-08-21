package az.simplesoft.tooliva.feature.doctor

import android.os.PowerManager
import az.simplesoft.tooliva.core.device.BatterySnapshot
import az.simplesoft.tooliva.core.device.DeviceInfo
import az.simplesoft.tooliva.core.device.DisplaySnapshot
import az.simplesoft.tooliva.core.device.MemorySnapshot
import az.simplesoft.tooliva.core.device.PhoneDoctorSnapshot
import az.simplesoft.tooliva.core.device.SystemInfo
import az.simplesoft.tooliva.core.device.ThermalSnapshot
import az.simplesoft.tooliva.core.settings.ToolivaLanguage
import az.simplesoft.tooliva.ui.ToolivaStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CheckupRulesTest {
    @Test
    fun attentionSummaryIncludesOnlyRealReportedProblems() {
        val attention = checkupAttentionItems(snapshot(lowMemory = true), failedHardwareTests = 2)

        assertEquals(4, attention.size)
        assertTrue(attention.any { it.contains("Memory") })
        assertTrue(attention.any { it.contains("Thermal") })
        assertTrue(attention.any { it.contains("battery") })
        assertTrue(attention.any { it.contains("2 hardware") })
    }

    @Test
    fun normalSnapshotDoesNotInventAttentionItems() {
        val normal = snapshot(lowMemory = false).copy(
            battery = snapshot(lowMemory = false).battery.copy(health = "Good"),
            thermal = ThermalSnapshot(PowerManager.THERMAL_STATUS_NONE, "Normal"),
        )
        assertTrue(checkupAttentionItems(normal, failedHardwareTests = 0).isEmpty())
    }

    @Test
    fun checkupCopyChangesWithSelectedLanguage() {
        assertEquals("Проверка телефона", ToolivaStrings.forLanguage(ToolivaLanguage.RUSSIAN).checkup.checkMyPhone)
        assertEquals("Telefonumu kontrol et", ToolivaStrings.forLanguage(ToolivaLanguage.TURKISH).checkup.checkMyPhone)
    }

    private fun snapshot(lowMemory: Boolean): PhoneDoctorSnapshot = PhoneDoctorSnapshot(
        device = DeviceInfo("Tooliva", "Tooliva", "Test device", "test", "test", "test", "test"),
        system = SystemInfo("13", 33, "2026-01-01", "test", listOf("arm64-v8a")),
        memory = MemorySnapshot(2_000L, 1_000L, lowMemory, 100L),
        storage = emptyList(),
        battery = BatterySnapshot(50, "Charging", "USB", 30f, 4f, "Li-ion", "Overheat", null, null),
        thermal = ThermalSnapshot(PowerManager.THERMAL_STATUS_MODERATE, "Moderate"),
        display = DisplaySnapshot(1080, 2400, 3f, 440, 120f),
        sensors = emptyList(),
    )
}
