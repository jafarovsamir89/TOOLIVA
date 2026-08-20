package az.simplesoft.tooliva.core.notifications

import android.content.Context

class NotificationHistoryPreferences(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    var paused: Boolean
        get() = preferences.getBoolean(KEY_PAUSED, false)
        set(value) { preferences.edit().putBoolean(KEY_PAUSED, value).apply() }

    var includeOngoing: Boolean
        get() = preferences.getBoolean(KEY_INCLUDE_ONGOING, false)
        set(value) { preferences.edit().putBoolean(KEY_INCLUDE_ONGOING, value).apply() }

    var retention: NotificationRetention
        get() = NotificationRetention.entries.firstOrNull { it.name == preferences.getString(KEY_RETENTION, null) } ?: NotificationRetention.THIRTY_DAYS
        set(value) { preferences.edit().putString(KEY_RETENTION, value.name).apply() }

    fun excludedPackages(): Set<String> = preferences.getStringSet(KEY_EXCLUDED, emptySet()).orEmpty()

    fun setExcluded(packageName: String, excluded: Boolean) {
        val next = excludedPackages().toMutableSet()
        if (excluded) next += packageName else next -= packageName
        preferences.edit().putStringSet(KEY_EXCLUDED, next).apply()
    }

    private companion object {
        const val FILE_NAME = "tooliva_notification_history"
        const val KEY_PAUSED = "paused"
        const val KEY_INCLUDE_ONGOING = "include_ongoing"
        const val KEY_RETENTION = "retention"
        const val KEY_EXCLUDED = "excluded_packages"
    }
}
