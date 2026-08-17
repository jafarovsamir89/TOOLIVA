package az.simplesoft.tooliva.core.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotAgeFilterTest {

    @Test
    fun matchesOnlyItemsOlderThanSelectedAge() {
        val now = 1_000_000_000L
        val thirtyDays = 30L * 86_400_000L

        assertTrue(ScreenshotAgeFilter.isOlderThan(now - thirtyDays - 1L, now, 30))
        assertFalse(ScreenshotAgeFilter.isOlderThan(now - thirtyDays + 1L, now, 30))
    }
}
