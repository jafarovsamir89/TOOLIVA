package az.simplesoft.tooliva.core.appmanager

sealed interface AppStorageInfo {
    data object NotLoaded : AppStorageInfo

    data class Available(
        val appBytes: Long,
        val dataBytes: Long,
        val cacheBytes: Long,
    ) : AppStorageInfo {
        // StorageStats.dataBytes includes cacheBytes. Do not add cacheBytes again.
        val totalBytes: Long
            get() = safeAdd(appBytes, dataBytes)
    }

    data class Unavailable(val reason: StorageUnavailableReason) : AppStorageInfo
}

enum class StorageUnavailableReason {
    USAGE_ACCESS_REQUIRED,
    SERVICE_MISSING,
    PACKAGE_DISAPPEARED,
    SECURITY_RESTRICTED,
    IO_FAILURE,
    UNKNOWN,
}

sealed interface AppUsageInfo {
    data object NotLoaded : AppUsageInfo
    data class Available(val lastTimeUsed: Long?) : AppUsageInfo
    data object Unavailable : AppUsageInfo
}

data class AppItem(
    val packageName: String,
    val label: String,
    val versionName: String?,
    val versionCode: Long?,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val isSystem: Boolean,
    val isEnabled: Boolean,
    val isLaunchable: Boolean,
    val isTooliva: Boolean,
    val storage: AppStorageInfo = AppStorageInfo.NotLoaded,
    val usage: AppUsageInfo = AppUsageInfo.NotLoaded,
)

enum class AppFilter(val title: String) {
    ALL("All"),
    USER("User"),
    SYSTEM("System"),
    RARELY_USED("Rarely used"),
}

enum class AppSort(val title: String) {
    NAME_ASC("Name A–Z"),
    NAME_DESC("Name Z–A"),
    INSTALLED_NEWEST("Installed newest"),
    INSTALLED_OLDEST("Installed oldest"),
    UPDATED_NEWEST("Updated newest"),
    LARGEST("Largest first"),
    SMALLEST("Smallest first"),
    RECENTLY_USED("Recently used"),
    LEAST_RECENTLY_USED("Least recently used"),
}

data class AppManagerSnapshot(
    val items: List<AppItem> = emptyList(),
    val isLoading: Boolean = false,
    val isEnriching: Boolean = false,
    val filter: AppFilter = AppFilter.USER,
    val rarelyUsedDays: Int = 30,
    val sort: AppSort = AppSort.NAME_ASC,
    val searchQuery: String = "",
    val usageAccessGranted: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
    val detailsPackage: String? = null,
    val pendingUninstallPackage: String? = null,
    val uninstallQueue: List<String> = emptyList(),
    val message: String? = null,
    val error: String? = null,
)

fun safeAdd(first: Long, second: Long): Long {
    if (first <= 0L) return second.coerceAtLeast(0L)
    if (second <= 0L) return first
    return if (Long.MAX_VALUE - first < second) Long.MAX_VALUE else first + second
}
