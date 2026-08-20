package az.simplesoft.tooliva.feature.clean.oldfiles

import az.simplesoft.tooliva.core.storage.StorageCategory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OldFilesRulesTest {
    @Test
    fun scopesStayNarrowAndScreenshotScopeUsesNameOrPath() {
        val base = "/storage/emulated/0/Download"
        assertTrue(OldFilesRules.inScope("$base/report.pdf", "report.pdf", StorageCategory.DOCUMENT, OldFilesScope.DOWNLOADS))
        assertFalse(OldFilesRules.inScope("$base/report.pdf", "report.pdf", StorageCategory.DOCUMENT, OldFilesScope.APK))
        assertTrue(OldFilesRules.inScope("/storage/emulated/0/Install/app.apk", "app.apk", StorageCategory.APK, OldFilesScope.APK))
        assertTrue(OldFilesRules.inScope("/storage/emulated/0/Pictures/Screenshots/screen.png", "screen.png", StorageCategory.IMAGE, OldFilesScope.SCREENSHOTS))
    }
}
