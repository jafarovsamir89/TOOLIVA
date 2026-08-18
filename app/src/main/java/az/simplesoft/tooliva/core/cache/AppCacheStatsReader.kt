package az.simplesoft.tooliva.core.cache

import android.app.usage.StorageStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.os.storage.StorageManager
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppCacheStatsReader(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val storageStats = appContext.getSystemService(StorageStatsManager::class.java)

    suspend fun read(app: DiscoveredCacheApp): CacheMeasurement = withContext(Dispatchers.IO) {
        if (storageStats == null) return@withContext CacheMeasurement(null, CacheMeasurementState.UNAVAILABLE, "service_missing")
        try {
            val applicationInfo = packageManager.getApplicationInfo(app.packageName, PackageManager.MATCH_ALL)
            val uuid = applicationInfo.storageUuid ?: StorageManager.UUID_DEFAULT
            val stats = storageStats.queryStatsForPackage(uuid, app.packageName, Process.myUserHandle())
            val bytes = stats.cacheBytes.coerceAtLeast(0L)
            CacheMeasurement(
                bytes = bytes.takeIf { it > 0L } ?: 0L,
                state = if (bytes > 0L) CacheMeasurementState.MEASURED else CacheMeasurementState.ZERO,
            )
        } catch (_: SecurityException) {
            CacheMeasurement(null, CacheMeasurementState.UNAVAILABLE, "security")
        } catch (_: IOException) {
            CacheMeasurement(null, CacheMeasurementState.UNAVAILABLE, "io")
        } catch (_: PackageManager.NameNotFoundException) {
            CacheMeasurement(null, CacheMeasurementState.UNAVAILABLE, "package_missing")
        } catch (_: RuntimeException) {
            CacheMeasurement(null, CacheMeasurementState.UNAVAILABLE, "runtime")
        }
    }

    suspend fun readAll(apps: List<DiscoveredCacheApp>): Map<String, CacheMeasurement> = withContext(Dispatchers.IO) {
        apps.associate { app -> app.packageName to read(app) }
    }
}
