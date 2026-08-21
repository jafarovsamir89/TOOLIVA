package az.simplesoft.tooliva.ui

import az.simplesoft.tooliva.core.settings.ToolivaLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class LocalizationTest {
    @Test
    fun commonActionsAreTranslatedForEverySupportedLanguage() {
        val languages = listOf(
            ToolivaLanguage.RUSSIAN,
            ToolivaLanguage.AZERBAIJANI,
            ToolivaLanguage.TURKISH,
        )

        languages.forEach { language ->
            assertNotEquals("Back remains English for $language", "Back", localizeToolivaText("Back", language))
            assertNotEquals("Large files remain English for $language", "Large files", localizeToolivaText("Large files", language))
            assertNotEquals("Settings remain English for $language", "Settings", localizeToolivaText("Settings", language))
        }
    }

    @Test
    fun dynamicCountsAndSortLabelsKeepTheirDataAndTranslateTheLabel() {
        assertEquals("12 файлов проверено", localizeToolivaText("12 files checked", ToolivaLanguage.RUSSIAN))
        assertEquals("12 fayl yoxlanıldı", localizeToolivaText("12 files checked", ToolivaLanguage.AZERBAIJANI))
        assertEquals("12 dosya kontrol edildi", localizeToolivaText("12 files checked", ToolivaLanguage.TURKISH))
        assertEquals("Sıralama: önce büyükler", localizeToolivaText("Sort: Largest first", ToolivaLanguage.TURKISH))
        assertEquals("7 gün", localizeToolivaText("7 days", ToolivaLanguage.AZERBAIJANI))
    }

    @Test
    fun englishKeepsSourceCopyAndUserDataUntouched() {
        assertEquals("Back", localizeToolivaText("Back", ToolivaLanguage.ENGLISH))
        assertEquals("photo_001.jpg", localizeToolivaText("photo_001.jpg", ToolivaLanguage.RUSSIAN))
        assertEquals("/storage/emulated/0/Download/archive.zip", localizeToolivaText("/storage/emulated/0/Download/archive.zip", ToolivaLanguage.TURKISH))
    }
}
