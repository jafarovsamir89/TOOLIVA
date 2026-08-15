package az.simplesoft.tooliva.core.device

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.PowerManager
import android.os.StatFs

data class DeviceSnapshot(
    val storageTotalBytes: Long,
    val storageAvailableBytes: Long,
    val batteryPercent: Int?,
    val thermalStatus: Int?,
) {
    val storageUsedBytes: Long = (storageTotalBytes - storageAvailableBytes).coerceAtLeast(0L)
    val storageUsedFraction: Float = if (storageTotalBytes > 0L) {
        (storageUsedBytes.toDouble() / storageTotalBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    } else {
        0f
    }
}

class DeviceSnapshotProvider(private val context: Context) {

    fun read(): DeviceSnapshot {
        val statFs = StatFs(Environment.getDataDirectory().absolutePath)
        val total = statFs.totalBytes
        val available = statFs.availableBytes

        val batteryManager = context.getSystemService(BatteryManager::class.java)
        val rawBattery = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val battery = rawBattery?.takeIf { it in 0..100 }

        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.getSystemService(PowerManager::class.java)?.currentThermalStatus
        } else {
            null
        }

        return DeviceSnapshot(
            storageTotalBytes = total,
            storageAvailableBytes = available,
            batteryPercent = battery,
            thermalStatus = thermal,
        )
    }
}
