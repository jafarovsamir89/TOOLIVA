package az.simplesoft.tooliva.ui

import androidx.compose.runtime.staticCompositionLocalOf
import az.simplesoft.tooliva.core.settings.ToolivaLanguage

data class ToolivaStrings(
    val language: ToolivaLanguage,
    val subtitle: String,
    val actionPlan: String,
    val reviewRealFindings: String,
    val runFirstPlan: String,
    val review: String,
    val history: String,
    val storage: String,
    val used: String,
    val available: String,
    val settings: String,
    val files: String,
    val clean: String,
    val tools: String,
    val more: String,
    val languageLabel: String,
) {
    companion object {
        fun forLanguage(language: ToolivaLanguage): ToolivaStrings = when (language) {
            ToolivaLanguage.RUSSIAN -> ToolivaStrings(language, "Ваш набор инструментов Android", "План очистки", "Проверьте реальные результаты последнего сканирования", "Запустите анализ в разделе «Очистка», чтобы создать первый план.", "Проверить", "История", "Хранилище", "занято", "доступно", "Настройки", "Файлы", "Очистка", "Инструменты", "Ещё", "Язык")
            ToolivaLanguage.AZERBAIJANI -> ToolivaStrings(language, "Android alətləriniz", "Təmizləmə planı", "Son yaddaş skanının real nəticələrini yoxlayın", "İlk planı yaratmaq üçün Təmizləmə bölməsində analizi başladın.", "Yoxla", "Tarixçə", "Yaddaş", "istifadə olunur", "boşdur", "Ayarlar", "Fayllar", "Təmizlə", "Alətlər", "Daha çox", "Dil")
            ToolivaLanguage.TURKISH -> ToolivaStrings(language, "Android araç kutunuz", "Temizlik planı", "Son depolama taramasındaki gerçek sonuçları inceleyin", "İlk planı oluşturmak için Temizle bölümünden analiz başlatın.", "İncele", "Geçmiş", "Depolama", "kullanılıyor", "boş", "Ayarlar", "Dosyalar", "Temizle", "Araçlar", "Daha fazla", "Dil")
            ToolivaLanguage.ENGLISH -> ToolivaStrings(language, "Your Android toolbox", "Action plan", "Review real findings from your last storage scan", "Run Analyze storage in Clean to build your first plan.", "Review", "History", "Storage", "used", "available", "Settings", "Files", "Clean", "Tools", "More", "Language")
        }
    }
}

val LocalToolivaStrings = staticCompositionLocalOf { ToolivaStrings.forLanguage(ToolivaLanguage.ENGLISH) }
