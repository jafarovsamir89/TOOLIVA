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
    val checkup: CheckupStrings,
) {
    companion object {
        fun forLanguage(language: ToolivaLanguage): ToolivaStrings = when (language) {
            ToolivaLanguage.RUSSIAN -> ToolivaStrings(language, "Ваш набор инструментов Android", "План очистки", "Проверьте реальные результаты последнего сканирования", "Запустите анализ в разделе «Очистка», чтобы создать первый план.", "Проверить", "История", "Хранилище", "занято", "доступно", "Настройки", "Файлы", "Очистка", "Инструменты", "Ещё", "Язык", CheckupStrings.russian())
            ToolivaLanguage.AZERBAIJANI -> ToolivaStrings(language, "Android alətləriniz", "Təmizləmə planı", "Son yaddaş skanının real nəticələrini yoxlayın", "İlk planı yaratmaq üçün Təmizləmə bölməsində analizi başladın.", "Yoxla", "Tarixçə", "Yaddaş", "istifadə olunur", "boşdur", "Ayarlar", "Fayllar", "Təmizlə", "Alətlər", "Daha çox", "Dil", CheckupStrings.azerbaijani())
            ToolivaLanguage.TURKISH -> ToolivaStrings(language, "Android araç kutunuz", "Temizlik planı", "Son depolama taramasındaki gerçek sonuçları inceleyin", "İlk planı oluşturmak için Temizle bölümünden analiz başlatın.", "İncele", "Geçmiş", "Depolama", "kullanılıyor", "boş", "Ayarlar", "Dosyalar", "Temizle", "Araçlar", "Daha fazla", "Dil", CheckupStrings.turkish())
            ToolivaLanguage.ENGLISH -> ToolivaStrings(language, "Your Android toolbox", "Action plan", "Review real findings from your last storage scan", "Run Analyze storage in Clean to build your first plan.", "Review", "History", "Storage", "used", "available", "Settings", "Files", "Clean", "Tools", "More", "Language", CheckupStrings.english())
        }
    }
}

data class CheckupStrings(
    val checkMyPhone: String,
    val reviewYourPhone: String,
    val intro: String,
    val runCheckup: String,
    val checking: String,
    val retry: String,
    val checkupComplete: String,
    val lastChecked: String,
    val needsAttention: String,
    val noAttention: String,
    val hardwareTests: String,
    val hardwareTestsCompleted: String,
    val problemReported: String,
    val deviceAndSystem: String,
    val device: String,
    val android: String,
    val securityPatch: String,
    val supportedAbis: String,
    val memory: String,
    val storage: String,
    val battery: String,
    val batteryHealth: String,
    val powerSource: String,
    val temperature: String,
    val voltage: String,
    val current: String,
    val thermal: String,
    val display: String,
    val sensors: String,
    val available: String,
    val unavailable: String,
    val cleanupTools: String,
    val storagePlan: String,
    val noStorageScan: String,
    val review: String,
    val reviewableFormat: String,
    val noAutomaticScan: String,
    val viewHardwareTests: String,
    val noErrorDetails: String,
    val actionTitles: Map<String, String> = emptyMap(),
    val actionSubtitles: Map<String, String> = emptyMap(),
    val refreshRate: String = "Refresh rate",
    val scanSummaryFormat: String = "%1\$d measured categories · %2\$d files checked",
    val checkedBytesFormat: String = "%s checked",
    val language: ToolivaLanguage = ToolivaLanguage.ENGLISH,
) {
    companion object {
        fun english() = CheckupStrings(
            "Check My Phone", "Review your phone", "A quick local check of device facts, memory, storage, battery, thermal status and known hardware-test results. It does not run expensive storage analysis.", "Run checkup", "Checking…", "Try again", "Checkup complete", "Last checked", "Needs attention", "No immediate issues detected", "Hardware tests", "%1\$d of %2\$d supported tests completed", "%1\$d problem reported", "Device and system", "Device", "Android", "Security patch", "CPU / ABI", "Memory", "Storage", "Battery", "Android health", "Power source", "Temperature", "Voltage", "Current", "Thermal", "Display", "Sensors", "available", "Unavailable", "Cleanup tools", "Storage review plan", "No storage scan has been run yet. Run Analyze storage in Clean to get measured review items.", "Review", "%1\$d items · %2\$s reviewable", "Expensive analyses remain user-started. Nothing is scanned or deleted automatically.", "Open hardware tests", "The check could not be completed.",
        ).copy(
            actionTitles = mapOf("large-files" to "Large files", "downloads" to "Downloads", "duplicates" to "Exact duplicates", "screenshots" to "Screenshots", "photo-analyzer" to "Photo Analyzer", "cache" to "Cache Cleaner", "optimizer" to "Phone Optimizer"),
            actionSubtitles = mapOf("large-files" to "Review the biggest accessible files", "downloads" to "Review installers, archives and documents", "duplicates" to "Analyze identical files when you choose", "screenshots" to "Review screenshots by age", "photo-analyzer" to "Review similar, blurry and large media on-device", "cache" to "Review app cache measurements", "optimizer" to "Open real memory and system cache tools"),
            language = ToolivaLanguage.ENGLISH,
        )

        fun russian() = CheckupStrings(
            "Проверка телефона", "Проверьте телефон", "Быстрая локальная проверка данных устройства, памяти, хранилища, батареи, температуры и известных результатов аппаратных тестов. Тяжёлый анализ хранилища не запускается.", "Запустить проверку", "Проверка…", "Повторить", "Проверка завершена", "Последняя проверка", "Требует внимания", "Критичных проблем не обнаружено", "Аппаратные тесты", "%1\$d из %2\$d поддерживаемых тестов завершено", "Сообщено о проблеме: %1\$d", "Устройство и система", "Устройство", "Android", "Патч безопасности", "CPU / ABI", "Память", "Хранилище", "Батарея", "Состояние Android", "Источник питания", "Температура", "Напряжение", "Ток", "Температура системы", "Экран", "Датчики", "доступно", "Недоступно", "Инструменты очистки", "План проверки хранилища", "Сканирование хранилища ещё не выполнялось. Запустите анализ в разделе «Очистка», чтобы получить реальные результаты.", "Открыть", "%1\$d объектов · %2\$s для проверки", "Тяжёлые анализы запускаются только вручную. Ничего не сканируется и не удаляется автоматически.", "Открыть аппаратные тесты", "Не удалось завершить проверку.",
        ).copy(
            actionTitles = mapOf("large-files" to "Большие файлы", "downloads" to "Загрузки", "duplicates" to "Дубликаты", "screenshots" to "Скриншоты", "photo-analyzer" to "Анализ фото", "cache" to "Очистка кэша", "optimizer" to "Оптимизатор телефона"),
            actionSubtitles = mapOf("large-files" to "Проверьте самые большие доступные файлы", "downloads" to "Проверьте установщики, архивы и документы", "duplicates" to "Найдите одинаковые файлы вручную", "screenshots" to "Проверьте скриншоты по возрасту", "photo-analyzer" to "Проверьте похожие, размытые и большие фото локально", "cache" to "Проверьте объём кэша приложений", "optimizer" to "Откройте реальные инструменты памяти и системного кэша"),
            refreshRate = "Частота обновления", scanSummaryFormat = "%1\$d категорий · проверено файлов: %2\$d", checkedBytesFormat = "%s проверено", language = ToolivaLanguage.RUSSIAN,
        )

        fun azerbaijani() = english().copy(
            checkMyPhone = "Telefonumu yoxla", reviewYourPhone = "Telefonunuzu yoxlayın", intro = "Cihaz, yaddaş, batareya, temperatur və məlum aparat testlərinin sürətli lokal yoxlanışı. Ağır yaddaş analizi başladılmır.", runCheckup = "Yoxlamanı başladın", checking = "Yoxlanılır…", retry = "Yenidən cəhd edin", checkupComplete = "Yoxlama tamamlandı", lastChecked = "Son yoxlama", needsAttention = "Diqqət tələb edir", noAttention = "Təcili problem aşkar edilmədi", hardwareTests = "Aparat testləri", deviceAndSystem = "Cihaz və sistem", storage = "Yaddaş", battery = "Batareya", thermal = "Sistem temperaturu", display = "Ekran", sensors = "Sensorlar", available = "mövcuddur", unavailable = "Mövcud deyil", cleanupTools = "Təmizləmə alətləri", storagePlan = "Yaddaş yoxlama planı", noStorageScan = "Yaddaş skanı hələ aparılmayıb. Real nəticələr üçün Təmizləmə bölməsində analizi başladın.", review = "Yoxla", noAutomaticScan = "Ağır analizlər yalnız istifadəçi tərəfindən başladılır. Heç nə avtomatik skan edilmir və silinmir.", viewHardwareTests = "Aparat testlərini açın", noErrorDetails = "Yoxlamanı tamamlamaq mümkün olmadı.",
            actionTitles = mapOf("large-files" to "Böyük fayllar", "downloads" to "Yükləmələr", "duplicates" to "Dəqiq dublikatlar", "screenshots" to "Ekran görüntüləri", "photo-analyzer" to "Foto analizatoru", "cache" to "Keş təmizləyicisi", "optimizer" to "Telefon optimizatoru"),
            actionSubtitles = mapOf("large-files" to "Ən böyük əlçatan faylları yoxlayın", "downloads" to "Quraşdırıcıları, arxivləri və sənədləri yoxlayın", "duplicates" to "Eyni faylları öz seçiminizlə analiz edin", "screenshots" to "Ekran görüntülərini yaşına görə yoxlayın", "photo-analyzer" to "Oxşar, bulanıq və böyük medianı cihazda yoxlayın", "cache" to "Tətbiq keşinin ölçüsünü yoxlayın", "optimizer" to "Real yaddaş və sistem keşi alətlərini açın"),
            refreshRate = "Yenilənmə tezliyi", scanSummaryFormat = "%1\$d ölçülmüş kateqoriya · %2\$d fayl yoxlanıldı", checkedBytesFormat = "%s yoxlanıldı", language = ToolivaLanguage.AZERBAIJANI,
        )

        fun turkish() = english().copy(
            checkMyPhone = "Telefonumu kontrol et", reviewYourPhone = "Telefonunuzu kontrol edin", intro = "Cihaz, bellek, depolama, pil, sıcaklık ve bilinen donanım testi sonuçlarının hızlı yerel kontrolü. Ağır depolama analizi başlatılmaz.", runCheckup = "Kontrolü başlat", checking = "Kontrol ediliyor…", retry = "Tekrar dene", checkupComplete = "Kontrol tamamlandı", lastChecked = "Son kontrol", needsAttention = "Dikkat gerekiyor", noAttention = "Acil bir sorun tespit edilmedi", hardwareTests = "Donanım testleri", deviceAndSystem = "Cihaz ve sistem", storage = "Depolama", battery = "Pil", thermal = "Sistem sıcaklığı", display = "Ekran", sensors = "Sensörler", available = "kullanılabilir", unavailable = "Kullanılamıyor", cleanupTools = "Temizleme araçları", storagePlan = "Depolama inceleme planı", noStorageScan = "Henüz depolama taraması yapılmadı. Gerçek sonuçlar için Temizle bölümünde analizi başlatın.", review = "İncele", noAutomaticScan = "Ağır analizler yalnızca kullanıcı tarafından başlatılır. Hiçbir şey otomatik olarak taranmaz veya silinmez.", viewHardwareTests = "Donanım testlerini aç", noErrorDetails = "Kontrol tamamlanamadı.",
            actionTitles = mapOf("large-files" to "Büyük dosyalar", "downloads" to "İndirilenler", "duplicates" to "Aynı dosyalar", "screenshots" to "Ekran görüntüleri", "photo-analyzer" to "Fotoğraf analizörü", "cache" to "Önbellek temizleyici", "optimizer" to "Telefon optimize edici"),
            actionSubtitles = mapOf("large-files" to "En büyük erişilebilir dosyaları inceleyin", "downloads" to "Kurulum dosyalarını, arşivleri ve belgeleri inceleyin", "duplicates" to "Aynı dosyaları istediğinizde analiz edin", "screenshots" to "Ekran görüntülerini yaşına göre inceleyin", "photo-analyzer" to "Benzer, bulanık ve büyük medyayı cihazda inceleyin", "cache" to "Uygulama önbelleği ölçümlerini inceleyin", "optimizer" to "Gerçek bellek ve sistem önbelleği araçlarını açın"),
            refreshRate = "Yenileme hızı", scanSummaryFormat = "%1\$d ölçülen kategori · %2\$d dosya kontrol edildi", checkedBytesFormat = "%s kontrol edildi", language = ToolivaLanguage.TURKISH,
        )
    }
}

val LocalToolivaStrings = staticCompositionLocalOf { ToolivaStrings.forLanguage(ToolivaLanguage.ENGLISH) }
