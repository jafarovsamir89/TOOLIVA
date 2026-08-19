package az.simplesoft.tooliva.core.device

data class PhoneDoctorSnapshot(
    val device: DeviceInfo,
    val system: SystemInfo,
    val memory: MemorySnapshot?,
    val storage: List<StorageVolumeSnapshot>,
    val battery: BatterySnapshot,
    val thermal: ThermalSnapshot,
    val display: DisplaySnapshot,
    val sensors: List<SensorSnapshot>,
)

data class DeviceInfo(
    val manufacturer: String,
    val brand: String,
    val model: String,
    val device: String,
    val product: String,
    val hardware: String,
    val board: String,
)

data class SystemInfo(
    val androidVersion: String,
    val sdkLevel: Int,
    val securityPatch: String,
    val buildDisplay: String,
    val supportedAbis: List<String>,
)

data class StorageVolumeSnapshot(
    val label: String,
    val path: String?,
    val totalBytes: Long,
    val availableBytes: Long,
    val removable: Boolean,
) {
    val usedBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
}

data class BatterySnapshot(
    val levelPercent: Int?,
    val status: String,
    val powerSource: String,
    val temperatureCelsius: Float?,
    val voltageVolts: Float?,
    val technology: String?,
    val health: String?,
    val currentNowMilliamps: Float?,
    val currentAverageMilliamps: Float?,
)

data class ThermalSnapshot(
    val status: Int?,
    val label: String,
)

data class DisplaySnapshot(
    val widthPixels: Int?,
    val heightPixels: Int?,
    val density: Float?,
    val densityDpi: Int?,
    val refreshRateHz: Float?,
)

data class SensorSnapshot(
    val name: String,
    val type: Int,
    val typeLabel: String,
    val vendor: String,
    val version: Int,
    val maxRange: Float,
    val resolution: Float,
    val powerMa: Float,
    val group: SensorGroup,
)

enum class SensorGroup(val label: String) {
    MOTION("Motion"),
    POSITION("Position"),
    ENVIRONMENT("Environment"),
    OTHER("Other"),
}

internal fun cleanDeviceField(value: String?, fallback: String = "Unavailable"): String =
    value?.trim()?.takeIf { it.isNotEmpty() } ?: fallback

internal fun thermalLabel(status: Int?): String = when (status) {
    null -> "Unavailable on this Android version"
    android.os.PowerManager.THERMAL_STATUS_NONE -> "Normal"
    android.os.PowerManager.THERMAL_STATUS_LIGHT -> "Light"
    android.os.PowerManager.THERMAL_STATUS_MODERATE -> "Moderate"
    android.os.PowerManager.THERMAL_STATUS_SEVERE -> "Severe"
    android.os.PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
    android.os.PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
    android.os.PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown"
    else -> "Unknown"
}

internal fun batteryStatusLabel(status: Int?): String = when (status) {
    android.os.BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
    android.os.BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
    android.os.BatteryManager.BATTERY_STATUS_FULL -> "Full"
    android.os.BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not charging"
    android.os.BatteryManager.BATTERY_STATUS_UNKNOWN, null -> "Unknown"
    else -> "Unknown"
}

internal fun batterySourceLabel(plugged: Int?): String = when (plugged) {
    android.os.BatteryManager.BATTERY_PLUGGED_AC -> "AC"
    android.os.BatteryManager.BATTERY_PLUGGED_USB -> "USB"
    android.os.BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
    0 -> "Battery"
    else -> "Unknown"
}

internal fun batteryHealthLabel(health: Int?): String? = when (health) {
    android.os.BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
    android.os.BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
    android.os.BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
    android.os.BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over voltage"
    android.os.BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Unspecified failure"
    android.os.BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
    null -> null
    else -> "Unknown"
}

internal fun validBatteryProperty(value: Int): Int? = value.takeUnless {
    it == Int.MIN_VALUE || it == Int.MAX_VALUE
}

internal fun sensorGroup(type: Int): SensorGroup = when (type) {
    android.hardware.Sensor.TYPE_ACCELEROMETER,
    android.hardware.Sensor.TYPE_GRAVITY,
    android.hardware.Sensor.TYPE_GYROSCOPE,
    android.hardware.Sensor.TYPE_LINEAR_ACCELERATION,
    android.hardware.Sensor.TYPE_ROTATION_VECTOR,
    android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR,
    android.hardware.Sensor.TYPE_SIGNIFICANT_MOTION,
    android.hardware.Sensor.TYPE_STEP_DETECTOR,
    android.hardware.Sensor.TYPE_STEP_COUNTER -> SensorGroup.MOTION
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD,
    android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR,
    android.hardware.Sensor.TYPE_PROXIMITY -> SensorGroup.POSITION
    android.hardware.Sensor.TYPE_LIGHT,
    android.hardware.Sensor.TYPE_PRESSURE,
    android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE,
    android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY -> SensorGroup.ENVIRONMENT
    else -> SensorGroup.OTHER
}

internal fun sensorTypeLabel(type: Int): String = when (type) {
    android.hardware.Sensor.TYPE_ACCELEROMETER -> "Accelerometer"
    android.hardware.Sensor.TYPE_MAGNETIC_FIELD -> "Magnetic field"
    android.hardware.Sensor.TYPE_GYROSCOPE -> "Gyroscope"
    android.hardware.Sensor.TYPE_LIGHT -> "Light"
    android.hardware.Sensor.TYPE_PRESSURE -> "Pressure"
    android.hardware.Sensor.TYPE_PROXIMITY -> "Proximity"
    android.hardware.Sensor.TYPE_GRAVITY -> "Gravity"
    android.hardware.Sensor.TYPE_LINEAR_ACCELERATION -> "Linear acceleration"
    android.hardware.Sensor.TYPE_ROTATION_VECTOR -> "Rotation vector"
    android.hardware.Sensor.TYPE_GAME_ROTATION_VECTOR -> "Game rotation vector"
    android.hardware.Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR -> "Geomagnetic rotation vector"
    android.hardware.Sensor.TYPE_STEP_COUNTER -> "Step counter"
    android.hardware.Sensor.TYPE_STEP_DETECTOR -> "Step detector"
    android.hardware.Sensor.TYPE_RELATIVE_HUMIDITY -> "Relative humidity"
    android.hardware.Sensor.TYPE_AMBIENT_TEMPERATURE -> "Ambient temperature"
    android.hardware.Sensor.TYPE_SIGNIFICANT_MOTION -> "Significant motion"
    else -> "Sensor type $type"
}
