package az.simplesoft.tooliva.core.cache

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CacheSelectionRulesTest {
    private val chrome = CacheAppEntry("chrome", "Chrome", CacheAppCategory.BROWSER, 500L, CacheMeasurementState.MEASURED)
    private val youtube = CacheAppEntry("youtube", "YouTube", CacheAppCategory.VIDEO, 100L, CacheMeasurementState.MEASURED)
    private val unavailable = CacheAppEntry("browser", "Browser", CacheAppCategory.BROWSER, null, CacheMeasurementState.UNAVAILABLE)

    @Test
    fun initialSelectionIsEmptyAndSelectedBytesAreExact() {
        assertTrue(CacheSelectionRules.toggle(emptySet(), "chrome").contains("chrome"))
        assertEquals(600L, CacheSelectionRules.selectedBytes(listOf(chrome, youtube), setOf("chrome", "youtube")))
    }

    @Test
    fun selectAllExcludesUnavailableAndZeroEntries() {
        assertEquals(setOf("chrome", "youtube"), CacheSelectionRules.selectAll(listOf(chrome, youtube, unavailable)))
    }
}
