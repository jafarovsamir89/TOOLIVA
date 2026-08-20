package az.simplesoft.tooliva.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TopLevelRouteTest {
    @Test
    fun nestedCleanerRouteBelongsToCleanTab() {
        assertEquals("clean", topLevelRoute("clean/screenshots"))
    }

    @Test
    fun topLevelRouteIsKeptUnchanged() {
        assertEquals("home", topLevelRoute("home"))
    }

    @Test
    fun missingRouteHasNoSelectedTab() {
        assertNull(topLevelRoute(null))
    }

    @Test
    fun fileManagerQueryBelongsToFilesAndSettingsBelongsToMore() {
        assertEquals("files", topLevelRoute("files?path=%2Fstorage%2Femulated%2F0"))
        assertEquals("more", topLevelRoute("settings"))
    }
}
