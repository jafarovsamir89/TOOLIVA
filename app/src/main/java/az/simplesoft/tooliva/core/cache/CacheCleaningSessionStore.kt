package az.simplesoft.tooliva.core.cache

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.max

data class CacheCleaningSession(
    val packages: List<String>,
    val beforeBytes: Map<String, Long>,
    val currentIndex: Int,
    val step: CacheCleaningStep,
    val startedAtMillis: Long,
) {
    val currentPackage: String? get() = packages.getOrNull(currentIndex)
}

data class CacheCleaningCompletion(
    val packages: List<String>,
    val beforeBytes: Map<String, Long>,
    val completedPackages: Set<String>,
    val failedPackages: Set<String>,
)

class CacheCleaningSessionStore(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    fun begin(packages: List<String>, beforeBytes: Map<String, Long>) {
        prefs.edit()
            .putString(KEY_PACKAGES, packages.distinct().joinToString(SEPARATOR))
            .putString(KEY_BEFORE, beforeBytes.entries.joinToString(SEPARATOR) { "${it.key}$PAIR_SEPARATOR${it.value}" })
            .putInt(KEY_INDEX, 0)
            .putString(KEY_STEP, CacheCleaningStep.APP_INFO.name)
            .putLong(KEY_STARTED, System.currentTimeMillis())
            .putString(KEY_COMPLETED, "")
            .putString(KEY_FAILED, "")
            .remove(KEY_RESULT)
            .apply()
    }

    fun active(): CacheCleaningSession? {
        val packages = readPackages()
        if (packages.isEmpty()) return null
        val started = prefs.getLong(KEY_STARTED, 0L)
        if (started <= 0L || System.currentTimeMillis() - started > SESSION_TIMEOUT_MILLIS) {
            clearActive()
            return null
        }
        return CacheCleaningSession(
            packages = packages,
            beforeBytes = readBytes(KEY_BEFORE),
            currentIndex = prefs.getInt(KEY_INDEX, 0).coerceIn(0, max(0, packages.lastIndex)),
            step = runCatching { CacheCleaningStep.valueOf(prefs.getString(KEY_STEP, null).orEmpty()) }.getOrDefault(CacheCleaningStep.FAILED),
            startedAtMillis = started,
        )
    }

    fun setStep(step: CacheCleaningStep) = prefs.edit().putString(KEY_STEP, step.name).apply()

    fun nextPackageAfterSuccess(): String? {
        val session = active() ?: return null
        val nextIndex = session.currentIndex + 1
        val completed = readSet(KEY_COMPLETED) + listOfNotNull(session.currentPackage)
        prefs.edit().putString(KEY_COMPLETED, completed.joinToString(SEPARATOR)).apply()
        if (nextIndex >= session.packages.size) return null
        prefs.edit().putInt(KEY_INDEX, nextIndex).putString(KEY_STEP, CacheCleaningStep.APP_INFO.name).apply()
        return session.packages[nextIndex]
    }

    fun markFailedAndComplete(): CacheCleaningCompletion? {
        val session = active() ?: return null
        val failed = readSet(KEY_FAILED) + listOfNotNull(session.currentPackage)
        val completion = CacheCleaningCompletion(
            packages = session.packages,
            beforeBytes = session.beforeBytes,
            completedPackages = readSet(KEY_COMPLETED),
            failedPackages = failed,
        )
        saveCompletion(completion)
        clearActive()
        return completion
    }

    fun complete(): CacheCleaningCompletion? {
        val session = active() ?: return null
        val completed = readSet(KEY_COMPLETED) + listOfNotNull(session.currentPackage)
        val completion = CacheCleaningCompletion(
            packages = session.packages,
            beforeBytes = session.beforeBytes,
            completedPackages = completed,
            failedPackages = readSet(KEY_FAILED),
        )
        saveCompletion(completion)
        clearActive()
        return completion
    }

    fun consumeCompletion(): CacheCleaningCompletion? {
        val raw = prefs.getString(KEY_RESULT, null) ?: return null
        val parts = raw.split(RESULT_SEPARATOR)
        if (parts.size < 4) return null
        val result = CacheCleaningCompletion(
            packages = parts[0].split(SEPARATOR).filter(String::isNotEmpty),
            beforeBytes = parseBytes(parts[1]),
            completedPackages = parts[2].split(SEPARATOR).filter(String::isNotEmpty).toSet(),
            failedPackages = parts[3].split(SEPARATOR).filter(String::isNotEmpty).toSet(),
        )
        prefs.edit().remove(KEY_RESULT).apply()
        return result
    }

    fun clearActive() {
        prefs.edit().remove(KEY_PACKAGES).remove(KEY_BEFORE).remove(KEY_INDEX).remove(KEY_STEP)
            .remove(KEY_STARTED).remove(KEY_COMPLETED).remove(KEY_FAILED).apply()
    }

    private fun saveCompletion(completion: CacheCleaningCompletion) {
        prefs.edit().putString(
            KEY_RESULT,
            listOf(
                completion.packages.joinToString(SEPARATOR),
                completion.beforeBytes.entries.joinToString(SEPARATOR) { "${it.key}$PAIR_SEPARATOR${it.value}" },
                completion.completedPackages.joinToString(SEPARATOR),
                completion.failedPackages.joinToString(SEPARATOR),
            ).joinToString(RESULT_SEPARATOR),
        ).apply()
    }

    private fun readPackages() = prefs.getString(KEY_PACKAGES, null).orEmpty().split(SEPARATOR).filter(String::isNotEmpty)
    private fun readSet(key: String) = prefs.getString(key, null).orEmpty().split(SEPARATOR).filter(String::isNotEmpty).toSet()
    private fun readBytes(key: String) = parseBytes(prefs.getString(key, null).orEmpty())
    private fun parseBytes(value: String): Map<String, Long> = value.split(SEPARATOR).mapNotNull { pair ->
        val parts = pair.split(PAIR_SEPARATOR, limit = 2)
        parts.getOrNull(1)?.toLongOrNull()?.let { parts[0] to it }
    }.toMap()

    companion object {
        private const val FILE_NAME = "cache_cleaning_session"
        private const val KEY_PACKAGES = "packages"
        private const val KEY_BEFORE = "before"
        private const val KEY_INDEX = "index"
        private const val KEY_STEP = "step"
        private const val KEY_STARTED = "started"
        private const val KEY_COMPLETED = "completed"
        private const val KEY_FAILED = "failed"
        private const val KEY_RESULT = "result"
        private const val SEPARATOR = ","
        private const val PAIR_SEPARATOR = "="
        private const val RESULT_SEPARATOR = "|"
        private const val SESSION_TIMEOUT_MILLIS = 60_000L
    }
}
