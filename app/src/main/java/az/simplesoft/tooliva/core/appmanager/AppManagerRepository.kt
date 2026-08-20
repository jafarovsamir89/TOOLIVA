package az.simplesoft.tooliva.core.appmanager

import android.app.usage.StorageStatsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.os.Build
import android.os.storage.StorageManager
import android.provider.Settings
import az.simplesoft.tooliva.core.cache.UsageAccessChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppManagerRepository(context: Context) {
    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val usageAccessChecker = UsageAccessChecker(appContext)
    private val usageStatsManager = appContext.getSystemService(UsageStatsManager::class.java)
    private val storageStatsManager = appContext.getSystemService(StorageStatsManager::class.java)

    fun isUsageAccessGranted(): Boolean = usageAccessChecker.isGranted()

    fun usageAccessIntent(): Intent = usageAccessChecker.settingsIntent()

    suspend fun readVisibleApps(): List<AppItem> = withContext(Dispatchers.IO) {
        packageManager.getInstalledApplications(0)
            .map { info ->
                val packageName = info.packageName
                val packageInfo = runCatching { packageManager.getPackageInfo(packageName, 0) }.getOrNull()
                val label = runCatching { normalizedAppLabel(info.loadLabel(packageManager)?.toString(), packageName) }
                    .getOrDefault(packageName)
                AppItem(
                    packageName = packageName,
                    label = label,
                    versionName = packageInfo?.versionName,
                    versionCode = packageInfo?.let {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) it.longVersionCode else it.versionCode.toLong()
                    },
                    firstInstallTime = packageInfo?.firstInstallTime ?: 0L,
                    lastUpdateTime = packageInfo?.lastUpdateTime ?: 0L,
                    isSystem = info.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0,
                    isEnabled = info.enabled,
                    isLaunchable = packageManager.getLaunchIntentForPackage(packageName) != null,
                    isTooliva = packageName == appContext.packageName,
                )
            }
            .sortedWith(compareBy<AppItem> { it.label.lowercase() }.thenBy { it.packageName })
    }

    suspend fun readUsage(): Map<String, AppUsageInfo> = withContext(Dispatchers.IO) {
        if (!usageAccessChecker.isGranted() || usageStatsManager == null) return@withContext emptyMap()
        val now = System.currentTimeMillis()
        val begin = now - TimeUnit.DAYS.toMillis(365)
        val aggregate = runCatching { usageStatsManager.queryAndAggregateUsageStats(begin, now) }.getOrNull()
            ?: return@withContext emptyMap()
        aggregate.mapValues { (_, usage) -> AppUsageInfo.Available(usage.lastTimeUsed.takeIf { it > 0L }) }
    }

    suspend fun readStorage(packageName: String): AppStorageInfo = withContext(Dispatchers.IO) {
        if (!usageAccessChecker.isGranted()) return@withContext AppStorageInfo.Unavailable(StorageUnavailableReason.USAGE_ACCESS_REQUIRED)
        val manager = storageStatsManager ?: return@withContext AppStorageInfo.Unavailable(StorageUnavailableReason.SERVICE_MISSING)
        try {
            val info = packageManager.getApplicationInfo(packageName, 0)
            val uuid = info.storageUuid ?: StorageManager.UUID_DEFAULT
            val stats = manager.queryStatsForPackage(uuid, packageName, Process.myUserHandle())
            AppStorageInfo.Available(
                appBytes = stats.appBytes.coerceAtLeast(0L),
                dataBytes = stats.dataBytes.coerceAtLeast(0L),
                cacheBytes = stats.cacheBytes.coerceAtLeast(0L),
            )
        } catch (_: PackageManager.NameNotFoundException) {
            AppStorageInfo.Unavailable(StorageUnavailableReason.PACKAGE_DISAPPEARED)
        } catch (_: SecurityException) {
            AppStorageInfo.Unavailable(StorageUnavailableReason.SECURITY_RESTRICTED)
        } catch (_: IOException) {
            AppStorageInfo.Unavailable(StorageUnavailableReason.IO_FAILURE)
        } catch (_: RuntimeException) {
            AppStorageInfo.Unavailable(StorageUnavailableReason.UNKNOWN)
        }
    }

    suspend fun readStorageProgressively(
        items: List<AppItem>,
        onItem: suspend (String, AppStorageInfo) -> Unit,
    ) = withContext(Dispatchers.IO) {
        // Sequential requests keep the first screen responsive and avoid a storage query burst.
        for (item in items) {
            onItem(item.packageName, readStorage(item.packageName))
        }
    }

    fun appInfoIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:$packageName")
        }

    fun launchIntent(packageName: String): Intent? = packageManager.getLaunchIntentForPackage(packageName)

    fun isInstalled(packageName: String): Boolean = runCatching {
        packageManager.getApplicationInfo(packageName, 0)
    }.isSuccess
}
