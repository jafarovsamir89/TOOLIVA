package az.simplesoft.tooliva.core.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs
import android.os.storage.StorageManager
import android.view.WindowManager
import java.util.Locale

class PhoneDoctorProvider(private val context: Context) {
    private val appContext = context.applicationContext

    fun read(): PhoneDoctorSnapshot = PhoneDoctorSnapshot(
        device = DeviceInfo(
            manufacturer = cleanDeviceField(Build.MANUFACTURER),
            brand = cleanDeviceField(Build.BRAND),
            model = cleanDeviceField(Build.MODEL),
            device = cleanDeviceField(Build.DEVICE),
            product = cleanDeviceField(Build.PRODUCT),
            hardware = cleanDeviceField(Build.HARDWARE),
            board = cleanDeviceField(Build.BOARD),
        ),
        system = SystemInfo(
            androidVersion = cleanDeviceField(Build.VERSION.RELEASE),
            sdkLevel = Build.VERSION.SDK_INT,
            securityPatch = cleanDeviceField(Build.VERSION.SECURITY_PATCH),
            buildDisplay = cleanDeviceField(Build.DISPLAY),
            supportedAbis = Build.SUPPORTED_ABIS.mapNotNull { it?.trim()?.takeIf(String::isNotEmpty) },
        ),
        memory = MemorySnapshotProvider(appContext).read(),
        storage = readStorage(),
        battery = readBattery(),
        thermal = readThermal(),
        display = readDisplay(),
        sensors = readSensors(),
    )

    private fun readStorage(): List<StorageVolumeSnapshot> {
        val volumes = mutableListOf<StorageVolumeSnapshot>()
        fun add(label: String, path: String?, removable: Boolean) {
            val resolvedPath = path?.takeIf { it.isNotEmpty() }
            val stat = runCatching { StatFs(resolvedPath ?: Environment.getDataDirectory().absolutePath) }.getOrNull()
            if (stat != null) {
                volumes += StorageVolumeSnapshot(label, resolvedPath, stat.totalBytes, stat.availableBytes, removable)
            }
        }
        add("Internal storage", Environment.getDataDirectory().absolutePath, false)
        val manager = appContext.getSystemService(StorageManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            manager?.storageVolumes.orEmpty()
                .filter { it.isRemovable || !it.isPrimary }
                .forEach { volume -> add(if (volume.isRemovable) "Removable storage" else "External storage", volume.directory?.absolutePath, volume.isRemovable) }
        }
        return volumes.distinctBy { it.path ?: it.label }
    }

    private fun readBattery(): BatterySnapshot {
        val intent = runCatching {
            appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        }.getOrNull()
        val manager = appContext.getSystemService(BatteryManager::class.java)
        fun property(id: Int): Int? = manager?.getIntProperty(id)?.let(::validBatteryProperty)
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)?.takeIf { it in 0..100 }
        val temperature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE)
            ?.takeUnless { it == Int.MIN_VALUE }
            ?.div(10f)
        val voltage = intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, Int.MIN_VALUE)
            ?.takeUnless { it == Int.MIN_VALUE }
            ?.div(1000f)
        return BatterySnapshot(
            levelPercent = level,
            status = batteryStatusLabel(intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)),
            powerSource = batterySourceLabel(intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)),
            temperatureCelsius = temperature,
            voltageVolts = voltage,
            technology = intent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY)?.takeIf(String::isNotBlank),
            health = batteryHealthLabel(intent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)),
            currentNowMilliamps = property(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.let { it / 1000f },
            currentAverageMilliamps = property(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE)?.let { it / 1000f },
        )
    }

    private fun readThermal(): ThermalSnapshot {
        val status = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appContext.getSystemService(PowerManager::class.java)?.currentThermalStatus
        } else null
        return ThermalSnapshot(status, thermalLabel(status))
    }

    private fun readDisplay(): DisplaySnapshot {
        val metrics = appContext.resources.displayMetrics
        val display = appContext.getSystemService(WindowManager::class.java)?.defaultDisplay
        return DisplaySnapshot(
            widthPixels = metrics.widthPixels.takeIf { it > 0 },
            heightPixels = metrics.heightPixels.takeIf { it > 0 },
            density = metrics.density.takeIf { it > 0f },
            densityDpi = metrics.densityDpi.takeIf { it > 0 },
            refreshRateHz = display?.refreshRate?.takeIf { it > 0f },
        )
    }

    private fun readSensors(): List<SensorSnapshot> {
        val manager = appContext.getSystemService(SensorManager::class.java) ?: return emptyList()
        return manager.getSensorList(Sensor.TYPE_ALL)
            .distinctBy { "${it.type}|${it.name}|${it.vendor}|${it.version}" }
            .map { sensor ->
                SensorSnapshot(
                    name = sensor.name.ifBlank { sensorTypeLabel(sensor.type) },
                    type = sensor.type,
                    typeLabel = sensorTypeLabel(sensor.type),
                    vendor = sensor.vendor.ifBlank { "Unavailable" },
                    version = sensor.version,
                    maxRange = sensor.maximumRange,
                    resolution = sensor.resolution,
                    powerMa = sensor.power,
                    group = sensorGroup(sensor.type),
                )
            }
            .sortedWith(compareBy({ it.group.ordinal }, { it.typeLabel }, { it.name.lowercase(Locale.US) }))
    }
}
