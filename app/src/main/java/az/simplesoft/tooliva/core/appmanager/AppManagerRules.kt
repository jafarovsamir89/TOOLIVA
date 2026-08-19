package az.simplesoft.tooliva.core.appmanager

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

fun normalizedAppLabel(label: String?, packageName: String): String =
    label?.trim().orEmpty().ifBlank { packageName }

fun AppItem.isRemovable(): Boolean = !isSystem && !isTooliva

fun matchesAppSearch(item: AppItem, query: String): Boolean {
    val normalized = query.trim().lowercase()
    if (normalized.isEmpty()) return true
    return item.label.lowercase().contains(normalized) || item.packageName.lowercase().contains(normalized)
}

fun daysSinceLastUse(lastTimeUsed: Long?, nowMillis: Long): Long? {
    if (lastTimeUsed == null || lastTimeUsed <= 0L || lastTimeUsed > nowMillis) return null
    return (nowMillis - lastTimeUsed) / MILLIS_PER_DAY
}

fun isRarelyUsed(item: AppItem, nowMillis: Long, thresholdDays: Int): Boolean =
    item.usage is AppUsageInfo.Available &&
        daysSinceLastUse(item.usage.lastTimeUsed, nowMillis)?.let { it >= thresholdDays } == true

fun filteredAndSortedApps(
    items: List<AppItem>,
    filter: AppFilter,
    query: String,
    sort: AppSort,
    nowMillis: Long,
    rarelyUsedDays: Int,
): List<AppItem> {
    val filtered = items.asSequence()
        .filter { matchesAppSearch(it, query) }
        .filter {
            when (filter) {
                AppFilter.ALL -> true
                AppFilter.USER -> !it.isSystem
                AppFilter.SYSTEM -> it.isSystem
                AppFilter.RARELY_USED -> isRarelyUsed(it, nowMillis, rarelyUsedDays)
            }
        }

    return when (sort) {
        AppSort.NAME_ASC -> filtered.sortedWith(compareBy<AppItem> { it.label.lowercase() }.thenBy { it.packageName })
        AppSort.NAME_DESC -> filtered.sortedWith(compareByDescending<AppItem> { it.label.lowercase() }.thenBy { it.packageName })
        AppSort.INSTALLED_NEWEST -> filtered.sortedWith(compareByDescending<AppItem> { it.firstInstallTime }.thenBy { it.label.lowercase() })
        AppSort.INSTALLED_OLDEST -> filtered.sortedWith(compareBy<AppItem> { it.firstInstallTime }.thenBy { it.label.lowercase() })
        AppSort.UPDATED_NEWEST -> filtered.sortedWith(compareByDescending<AppItem> { it.lastUpdateTime }.thenBy { it.label.lowercase() })
        AppSort.LARGEST -> filtered.sortedWith(compareByDescending<AppItem> { (it.storage as? AppStorageInfo.Available)?.totalBytes ?: -1L }.thenBy { it.label.lowercase() })
        AppSort.SMALLEST -> filtered.sortedWith(compareBy<AppItem> { (it.storage as? AppStorageInfo.Available)?.totalBytes ?: Long.MAX_VALUE }.thenBy { it.label.lowercase() })
        AppSort.RECENTLY_USED -> filtered.sortedWith(compareByDescending<AppItem> { (it.usage as? AppUsageInfo.Available)?.lastTimeUsed ?: -1L }.thenBy { it.label.lowercase() })
        AppSort.LEAST_RECENTLY_USED -> filtered.sortedWith(compareBy<AppItem> { (it.usage as? AppUsageInfo.Available)?.lastTimeUsed ?: Long.MAX_VALUE }.thenBy { it.label.lowercase() })
    }.toList()
}
