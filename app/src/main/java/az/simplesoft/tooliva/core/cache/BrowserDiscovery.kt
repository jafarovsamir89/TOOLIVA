package az.simplesoft.tooliva.core.cache

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri

data class DiscoveredCacheApp(
    val packageName: String,
    val appLabel: String,
    val applicationInfo: ApplicationInfo,
    val category: CacheAppCategory,
)

class BrowserDiscovery(private val packageManager: PackageManager) {
    fun discover(): List<DiscoveredCacheApp> {
        val packages = linkedMapOf<String, DiscoveredCacheApp>()
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://tooliva.local"))
            .addCategory(Intent.CATEGORY_BROWSABLE)
        packageManager.queryIntentActivities(browserIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .forEach { resolveInfo ->
                val appInfo = resolveInfo.activityInfo?.applicationInfo ?: return@forEach
                packages[appInfo.packageName] = DiscoveredCacheApp(
                    packageName = appInfo.packageName,
                    appLabel = appInfo.loadLabel(packageManager).toString(),
                    applicationInfo = appInfo,
                    category = CacheAppCategory.BROWSER,
                )
            }

        runCatching {
            packageManager.getApplicationInfo(YOUTUBE_PACKAGE, PackageManager.MATCH_ALL)
        }.onSuccess { appInfo ->
            packages[appInfo.packageName] = DiscoveredCacheApp(
                packageName = appInfo.packageName,
                appLabel = appInfo.loadLabel(packageManager).toString(),
                applicationInfo = appInfo,
                category = CacheAppCategory.VIDEO,
            )
        }

        return packages.values.sortedWith(compareBy({ it.category }, { it.appLabel.lowercase() }))
    }

    companion object {
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
    }
}
