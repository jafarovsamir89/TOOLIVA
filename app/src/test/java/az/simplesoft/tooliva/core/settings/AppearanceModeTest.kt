package az.simplesoft.tooliva.core.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class AppearanceModeTest {
    @Test
    fun allSupportedAppearanceModesAreExplicit() {
        assertEquals(listOf("SYSTEM", "DARK", "LIGHT"), AppearanceMode.entries.map { it.name })
    }
}
