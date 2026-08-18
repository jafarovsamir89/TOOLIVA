package az.simplesoft.tooliva.core.cache

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.provider.Settings

object AccessibilityState {
    fun isCacheCleanerEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty()
        val expected = ComponentName(context, CacheCleaningAccessibilityService::class.java).flattenToString()
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun settingsIntent() = android.content.Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
