package az.simplesoft.tooliva.core.appmanager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppManagerRulesTest {
    private val now = 1_700_000_000_000L

    @Test
    fun blankLabelFallsBackToPackageName() {
        assertEquals("com.example.tool", normalizedAppLabel("  ", "com.example.tool"))
        assertEquals("Maps", normalizedAppLabel(" Maps ", "com.example.maps"))
    }

    @Test
    fun userAndSystemFiltersUseApplicationClassification() {
        val user = app("User app", isSystem = false)
        val system = app("System app", isSystem = true)

        assertEquals(listOf(user), filteredAndSortedApps(listOf(system, user), AppFilter.USER, "", AppSort.NAME_ASC, now, 30))
        assertEquals(listOf(system), filteredAndSortedApps(listOf(system, user), AppFilter.SYSTEM, "", AppSort.NAME_ASC, now, 30))
    }

    @Test
    fun searchMatchesLabelOrPackageName() {
        val maps = app("Maps", packageName = "com.example.maps")
        val player = app("Player", packageName = "com.example.media")

        assertEquals(listOf(maps), filteredAndSortedApps(listOf(player, maps), AppFilter.ALL, "MAPS", AppSort.NAME_ASC, now, 30))
        assertEquals(listOf(player), filteredAndSortedApps(listOf(player, maps), AppFilter.ALL, "media", AppSort.NAME_ASC, now, 30))
    }

    @Test
    fun rarelyUsedUsesExactThresholdAndExcludesUnknown() {
        val thirtyDaysAgo = now - 30L * 24L * 60L * 60L * 1000L
        val twentyNineDaysAgo = now - 29L * 24L * 60L * 60L * 1000L
        val old = app("Old", usage = AppUsageInfo.Available(thirtyDaysAgo))
        val recent = app("Recent", usage = AppUsageInfo.Available(twentyNineDaysAgo))
        val unknown = app("Unknown", usage = AppUsageInfo.Available(null))

        val result = filteredAndSortedApps(listOf(unknown, recent, old), AppFilter.RARELY_USED, "", AppSort.NAME_ASC, now, 30)

        assertEquals(listOf(old), result)
        assertFalse(isRarelyUsed(unknown, now, 30))
        assertNull(daysSinceLastUse(null, now))
        assertNull(daysSinceLastUse(now + 1, now))
    }

    @Test
    fun storageTotalDoesNotDoubleCountCache() {
        val storage = AppStorageInfo.Available(appBytes = 10, dataBytes = 90, cacheBytes = 25)

        assertEquals(100, storage.totalBytes)
        assertTrue(storage.cacheBytes < storage.dataBytes)
    }

    private fun app(
        label: String,
        packageName: String = "com.example.${label.lowercase()}",
        isSystem: Boolean = false,
        usage: AppUsageInfo = AppUsageInfo.Unavailable,
    ) = AppItem(
        packageName = packageName,
        label = label,
        versionName = "1.0",
        versionCode = 1,
        firstInstallTime = now,
        lastUpdateTime = now,
        isSystem = isSystem,
        isEnabled = true,
        isLaunchable = true,
        isTooliva = false,
        usage = usage,
    )
}
