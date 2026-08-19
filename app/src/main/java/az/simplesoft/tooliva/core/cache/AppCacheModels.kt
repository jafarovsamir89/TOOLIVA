package az.simplesoft.tooliva.core.cache

enum class CacheAppCategory {
    BROWSER,
    VIDEO,
}

enum class CacheMeasurementState {
    MEASURED,
    ZERO,
    UNAVAILABLE,
}

data class CacheMeasurement(
    val bytes: Long?,
    val state: CacheMeasurementState,
    val errorCode: String? = null,
)

data class CacheAppEntry(
    val packageName: String,
    val appLabel: String,
    val category: CacheAppCategory,
    val cacheBytes: Long?,
    val measurementState: CacheMeasurementState,
    val selected: Boolean = false,
)

object CacheSelectionRules {
    fun toggle(selected: Set<String>, packageName: String): Set<String> =
        if (packageName in selected) selected - packageName else selected + packageName

    fun selectAll(entries: List<CacheAppEntry>): Set<String> =
        entries.filter { it.measurementState == CacheMeasurementState.MEASURED && (it.cacheBytes ?: 0L) > 0L }
            .mapTo(linkedSetOf()) { it.packageName }

    fun selectedBytes(entries: List<CacheAppEntry>, selected: Set<String>): Long =
        entries.asSequence()
            .filter { it.packageName in selected }
            .sumOf { it.cacheBytes ?: 0L }

    fun totalMeasuredBytes(entries: List<CacheAppEntry>): Long =
        entries.sumOf { it.cacheBytes ?: 0L }

}
