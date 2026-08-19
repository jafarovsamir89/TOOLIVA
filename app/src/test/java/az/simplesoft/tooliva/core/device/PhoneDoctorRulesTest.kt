package az.simplesoft.tooliva.core.device

import android.os.BatteryManager
import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PhoneDoctorRulesTest {
    @Test fun thermalMappingCoversOfficialStatusesAndUnknown() {
        assertEquals("Normal", thermalLabel(PowerManager.THERMAL_STATUS_NONE))
        assertEquals("Light", thermalLabel(PowerManager.THERMAL_STATUS_LIGHT))
        assertEquals("Moderate", thermalLabel(PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals("Severe", thermalLabel(PowerManager.THERMAL_STATUS_SEVERE))
        assertEquals("Critical", thermalLabel(PowerManager.THERMAL_STATUS_CRITICAL))
        assertEquals("Emergency", thermalLabel(PowerManager.THERMAL_STATUS_EMERGENCY))
        assertEquals("Shutdown", thermalLabel(PowerManager.THERMAL_STATUS_SHUTDOWN))
        assertEquals("Unknown", thermalLabel(99))
        assertEquals("Unavailable on this Android version", thermalLabel(null))
    }

    @Test fun batteryMappingsDoNotCrashOnUnknownValues() {
        assertEquals("Charging", batteryStatusLabel(BatteryManager.BATTERY_STATUS_CHARGING))
        assertEquals("Discharging", batteryStatusLabel(BatteryManager.BATTERY_STATUS_DISCHARGING))
        assertEquals("Full", batteryStatusLabel(BatteryManager.BATTERY_STATUS_FULL))
        assertEquals("Not charging", batteryStatusLabel(BatteryManager.BATTERY_STATUS_NOT_CHARGING))
        assertEquals("Unknown", batteryStatusLabel(123))
        assertEquals("Wireless", batterySourceLabel(BatteryManager.BATTERY_PLUGGED_WIRELESS))
        assertEquals("Battery", batterySourceLabel(0))
        assertEquals("Good", batteryHealthLabel(BatteryManager.BATTERY_HEALTH_GOOD))
        assertEquals("Unknown", batteryHealthLabel(123))
        assertNull(batteryHealthLabel(null))
        assertNull(validBatteryProperty(Int.MIN_VALUE))
    }

    @Test fun sensorTypesHaveTruthfulGroups() {
        assertEquals(SensorGroup.MOTION, sensorGroup(android.hardware.Sensor.TYPE_ACCELEROMETER))
        assertEquals(SensorGroup.POSITION, sensorGroup(android.hardware.Sensor.TYPE_MAGNETIC_FIELD))
        assertEquals(SensorGroup.ENVIRONMENT, sensorGroup(android.hardware.Sensor.TYPE_LIGHT))
        assertEquals(SensorGroup.OTHER, sensorGroup(10_000))
        assertEquals("Gyroscope", sensorTypeLabel(android.hardware.Sensor.TYPE_GYROSCOPE))
    }
}
