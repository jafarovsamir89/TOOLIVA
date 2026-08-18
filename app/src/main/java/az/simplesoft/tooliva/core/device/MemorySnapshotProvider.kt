package az.simplesoft.tooliva.core.device

import android.app.ActivityManager
import android.content.Context

data class MemorySnapshot(
    val totalBytes: Long,
    val availableBytes: Long,
    val lowMemory: Boolean,
    val thresholdBytes: Long,
) {
    val usedEstimateBytes: Long get() = (totalBytes - availableBytes).coerceAtLeast(0L)
    val pressureLabel: String get() = if (lowMemory) "High" else "Normal"
}

class MemorySnapshotProvider(context: Context) {
    private val activityManager = context.applicationContext.getSystemService(ActivityManager::class.java)

    fun read(): MemorySnapshot? {
        val manager = activityManager ?: return null
        return ActivityManager.MemoryInfo().also(manager::getMemoryInfo).let { info ->
            MemorySnapshot(
                totalBytes = info.totalMem.coerceAtLeast(0L),
                availableBytes = info.availMem.coerceIn(0L, info.totalMem.coerceAtLeast(0L)),
                lowMemory = info.lowMemory,
                thresholdBytes = info.threshold.coerceAtLeast(0L),
            )
        }
    }
}
