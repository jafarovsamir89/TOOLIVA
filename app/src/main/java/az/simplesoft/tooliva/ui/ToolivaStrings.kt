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
    val messages: Map<String, String> = emptyMap(),
) {
    fun text(key: String): String = messages[key] ?: key

    companion object {
        fun english() = CheckupStrings(
            "Check My Phone", "Review your phone", "A quick local check of device facts, memory, storage, battery, thermal status and known hardware-test results. It does not run expensive storage analysis.", "Run checkup", "Checking…", "Try again", "Checkup complete", "Last checked", "Needs attention", "No immediate issues detected", "Hardware tests", "%1\$d of %2\$d supported tests completed", "%1\$d problem reported", "Device and system", "Device", "Android", "Security patch", "CPU / ABI", "Memory", "Storage", "Battery", "Android health", "Power source", "Temperature", "Voltage", "Current", "Thermal", "Display", "Sensors", "available", "Unavailable", "Cleanup tools", "Storage review plan", "No storage scan has been run yet. Run Analyze storage in Clean to get measured review items.", "Review", "%1\$d items · %2\$s reviewable", "Expensive analyses remain user-started. Nothing is scanned or deleted automatically.", "Open hardware tests", "The check could not be completed.",
        ).copy(
            actionTitles = mapOf("large-files" to "Large files", "downloads" to "Downloads", "duplicates" to "Exact duplicates", "screenshots" to "Screenshots", "photo-analyzer" to "Photo Analyzer", "cache" to "Cache Cleaner", "optimizer" to "Phone Optimizer"),
            actionSubtitles = mapOf("large-files" to "Review the biggest accessible files", "downloads" to "Review installers, archives and documents", "duplicates" to "Analyze identical files when you choose", "screenshots" to "Review screenshots by age", "photo-analyzer" to "Review similar, blurry and large media on-device", "cache" to "Review app cache measurements", "optimizer" to "Open real memory and system cache tools"),
            language = ToolivaLanguage.ENGLISH,
            messages = checkupMessages(ToolivaLanguage.ENGLISH),
        )

        fun russian() = CheckupStrings(
            "Проверка телефона", "Проверьте телефон", "Быстрая локальная проверка данных устройства, памяти, хранилища, батареи, температуры и известных результатов аппаратных тестов. Тяжёлый анализ хранилища не запускается.", "Запустить проверку", "Проверка…", "Повторить", "Проверка завершена", "Последняя проверка", "Требует внимания", "Критичных проблем не обнаружено", "Аппаратные тесты", "%1\$d из %2\$d поддерживаемых тестов завершено", "Сообщено о проблеме: %1\$d", "Устройство и система", "Устройство", "Android", "Патч безопасности", "CPU / ABI", "Память", "Хранилище", "Батарея", "Состояние Android", "Источник питания", "Температура", "Напряжение", "Ток", "Температура системы", "Экран", "Датчики", "доступно", "Недоступно", "Инструменты очистки", "План проверки хранилища", "Сканирование хранилища ещё не выполнялось. Запустите анализ в разделе «Очистка», чтобы получить реальные результаты.", "Открыть", "%1\$d объектов · %2\$s для проверки", "Тяжёлые анализы запускаются только вручную. Ничего не сканируется и не удаляется автоматически.", "Открыть аппаратные тесты", "Не удалось завершить проверку.",
        ).copy(
            actionTitles = mapOf("large-files" to "Большие файлы", "downloads" to "Загрузки", "duplicates" to "Дубликаты", "screenshots" to "Скриншоты", "photo-analyzer" to "Анализ фото", "cache" to "Очистка кэша", "optimizer" to "Оптимизатор телефона"),
            actionSubtitles = mapOf("large-files" to "Проверьте самые большие доступные файлы", "downloads" to "Проверьте установщики, архивы и документы", "duplicates" to "Найдите одинаковые файлы вручную", "screenshots" to "Проверьте скриншоты по возрасту", "photo-analyzer" to "Проверьте похожие, размытые и большие фото локально", "cache" to "Проверьте объём кэша приложений", "optimizer" to "Откройте реальные инструменты памяти и системного кэша"),
            refreshRate = "Частота обновления", scanSummaryFormat = "%1\$d категорий · проверено файлов: %2\$d", checkedBytesFormat = "%s проверено", language = ToolivaLanguage.RUSSIAN,
            messages = checkupMessages(ToolivaLanguage.RUSSIAN),
        )

        fun azerbaijani() = english().copy(
            checkMyPhone = "Telefonumu yoxla", reviewYourPhone = "Telefonunuzu yoxlayın", intro = "Cihaz, yaddaş, batareya, temperatur və məlum aparat testlərinin sürətli lokal yoxlanışı. Ağır yaddaş analizi başladılmır.", runCheckup = "Yoxlamanı başladın", checking = "Yoxlanılır…", retry = "Yenidən cəhd edin", checkupComplete = "Yoxlama tamamlandı", lastChecked = "Son yoxlama", needsAttention = "Diqqət tələb edir", noAttention = "Təcili problem aşkar edilmədi", hardwareTests = "Aparat testləri", deviceAndSystem = "Cihaz və sistem", storage = "Yaddaş", battery = "Batareya", thermal = "Sistem temperaturu", display = "Ekran", sensors = "Sensorlar", available = "mövcuddur", unavailable = "Mövcud deyil", cleanupTools = "Təmizləmə alətləri", storagePlan = "Yaddaş yoxlama planı", noStorageScan = "Yaddaş skanı hələ aparılmayıb. Real nəticələr üçün Təmizləmə bölməsində analizi başladın.", review = "Yoxla", noAutomaticScan = "Ağır analizlər yalnız istifadəçi tərəfindən başladılır. Heç nə avtomatik skan edilmir və silinmir.", viewHardwareTests = "Aparat testlərini açın", noErrorDetails = "Yoxlamanı tamamlamaq mümkün olmadı.",
            actionTitles = mapOf("large-files" to "Böyük fayllar", "downloads" to "Yükləmələr", "duplicates" to "Dəqiq dublikatlar", "screenshots" to "Ekran görüntüləri", "photo-analyzer" to "Foto analizatoru", "cache" to "Keş təmizləyicisi", "optimizer" to "Telefon optimizatoru"),
            actionSubtitles = mapOf("large-files" to "Ən böyük əlçatan faylları yoxlayın", "downloads" to "Quraşdırıcıları, arxivləri və sənədləri yoxlayın", "duplicates" to "Eyni faylları öz seçiminizlə analiz edin", "screenshots" to "Ekran görüntülərini yaşına görə yoxlayın", "photo-analyzer" to "Oxşar, bulanıq və böyük medianı cihazda yoxlayın", "cache" to "Tətbiq keşinin ölçüsünü yoxlayın", "optimizer" to "Real yaddaş və sistem keşi alətlərini açın"),
            refreshRate = "Yenilənmə tezliyi", scanSummaryFormat = "%1\$d ölçülmüş kateqoriya · %2\$d fayl yoxlanıldı", checkedBytesFormat = "%s yoxlanıldı", language = ToolivaLanguage.AZERBAIJANI,
            messages = checkupMessages(ToolivaLanguage.AZERBAIJANI),
        )

        fun turkish() = english().copy(
            checkMyPhone = "Telefonumu kontrol et", reviewYourPhone = "Telefonunuzu kontrol edin", intro = "Cihaz, bellek, depolama, pil, sıcaklık ve bilinen donanım testi sonuçlarının hızlı yerel kontrolü. Ağır depolama analizi başlatılmaz.", runCheckup = "Kontrolü başlat", checking = "Kontrol ediliyor…", retry = "Tekrar dene", checkupComplete = "Kontrol tamamlandı", lastChecked = "Son kontrol", needsAttention = "Dikkat gerekiyor", noAttention = "Acil bir sorun tespit edilmedi", hardwareTests = "Donanım testleri", deviceAndSystem = "Cihaz ve sistem", storage = "Depolama", battery = "Pil", thermal = "Sistem sıcaklığı", display = "Ekran", sensors = "Sensörler", available = "kullanılabilir", unavailable = "Kullanılamıyor", cleanupTools = "Temizleme araçları", storagePlan = "Depolama inceleme planı", noStorageScan = "Henüz depolama taraması yapılmadı. Gerçek sonuçlar için Temizle bölümünde analizi başlatın.", review = "İncele", noAutomaticScan = "Ağır analizler yalnızca kullanıcı tarafından başlatılır. Hiçbir şey otomatik olarak taranmaz veya silinmez.", viewHardwareTests = "Donanım testlerini aç", noErrorDetails = "Kontrol tamamlanamadı.",
            actionTitles = mapOf("large-files" to "Büyük dosyalar", "downloads" to "İndirilenler", "duplicates" to "Aynı dosyalar", "screenshots" to "Ekran görüntüleri", "photo-analyzer" to "Fotoğraf analizörü", "cache" to "Önbellek temizleyici", "optimizer" to "Telefon optimize edici"),
            actionSubtitles = mapOf("large-files" to "En büyük erişilebilir dosyaları inceleyin", "downloads" to "Kurulum dosyalarını, arşivleri ve belgeleri inceleyin", "duplicates" to "Aynı dosyaları istediğinizde analiz edin", "screenshots" to "Ekran görüntülerini yaşına göre inceleyin", "photo-analyzer" to "Benzer, bulanık ve büyük medyayı cihazda inceleyin", "cache" to "Uygulama önbelleği ölçümlerini inceleyin", "optimizer" to "Gerçek bellek ve sistem önbelleği araçlarını açın"),
            refreshRate = "Yenileme hızı", scanSummaryFormat = "%1\$d ölçülen kategori · %2\$d dosya kontrol edildi", checkedBytesFormat = "%s kontrol edildi", language = ToolivaLanguage.TURKISH,
            messages = checkupMessages(ToolivaLanguage.TURKISH),
        )
    }
}

private fun checkupMessages(language: ToolivaLanguage): Map<String, String> = when (language) {
    ToolivaLanguage.ENGLISH -> mapOf(
        "status_good" to "Your phone looks good",
        "status_attention" to "A few things need attention",
        "status_critical" to "Important issues found",
        "status_recommended" to "Recommended checks",
        "findings" to "What we found",
        "findings_count" to "%d findings · %d total",
        "recommended" to "Recommended next steps",
        "storage_low_title" to "Storage is running low",
        "storage_low_detail" to "%1\$s available of %2\$s",
        "low_memory_title" to "Memory pressure is high",
        "low_memory_detail" to "Android reports low-memory pressure",
        "thermal_title" to "Phone is warmer than normal",
        "thermal_detail" to "Thermal status: %s",
        "low_battery_title" to "Battery is low",
        "low_battery_detail" to "%d%% battery remaining",
        "battery_health_title" to "Battery health warning",
        "battery_health_detail" to "Android reports: %s",
        "old_patch_title" to "Security patch may be outdated",
        "old_patch_detail" to "The installed patch is about %d days old",
        "hardware_failed_title" to "Hardware test reported a problem",
        "hardware_failed_detail" to "%d test(s) reported a problem",
        "hardware_pending_title" to "Hardware tests are not complete",
        "hardware_pending_detail" to "%d supported test(s) still need your confirmation",
        "large_files_title" to "Large files found",
        "downloads_title" to "Downloads to review",
        "screenshots_title" to "Screenshots to review",
        "reviewable_detail" to "%d items · %s reviewable",
        "storage_scan_title" to "Create your storage plan",
        "storage_scan_detail" to "Run one storage analysis to get measured review items",
        "review" to "Review",
        "details" to "Device details",
        "show_details" to "Show details",
        "hide_details" to "Hide details",
        "no_scan" to "No storage scan has been run yet",
    )
    ToolivaLanguage.RUSSIAN -> mapOf(
        "status_good" to "Телефон работает нормально",
        "status_attention" to "Есть пункты для внимания",
        "status_critical" to "Найдены важные проблемы",
        "status_recommended" to "Рекомендуемые проверки",
        "findings" to "Что найдено",
        "findings_count" to "%d пунктов · всего: %d",
        "recommended" to "Что сделать дальше",
        "storage_low_title" to "Мало свободного места",
        "storage_low_detail" to "Доступно %1\$s из %2\$s",
        "low_memory_title" to "Высокая нагрузка на память",
        "low_memory_detail" to "Android сообщает о нехватке памяти",
        "thermal_title" to "Телефон теплее обычного",
        "thermal_detail" to "Состояние температуры: %s",
        "low_battery_title" to "Низкий заряд батареи",
        "low_battery_detail" to "Осталось заряда: %d%%",
        "battery_health_title" to "Предупреждение о батарее",
        "battery_health_detail" to "Android сообщает: %s",
        "old_patch_title" to "Патч безопасности может быть устаревшим",
        "old_patch_detail" to "Установленному патчу примерно %d дней",
        "hardware_failed_title" to "Аппаратный тест сообщил о проблеме",
        "hardware_failed_detail" to "Проблема отмечена в тестах: %d",
        "hardware_pending_title" to "Аппаратные тесты не завершены",
        "hardware_pending_detail" to "Ожидают подтверждения: %d поддерживаемых теста",
        "large_files_title" to "Найдены большие файлы",
        "downloads_title" to "Загрузки для проверки",
        "screenshots_title" to "Скриншоты для проверки",
        "reviewable_detail" to "%d объектов · %s для проверки",
        "storage_scan_title" to "Создайте план проверки хранилища",
        "storage_scan_detail" to "Запустите анализ, чтобы получить реальные результаты",
        "review" to "Проверить",
        "details" to "Данные устройства",
        "show_details" to "Показать подробности",
        "hide_details" to "Скрыть подробности",
        "no_scan" to "Сканирование хранилища ещё не выполнялось",
    )
    ToolivaLanguage.AZERBAIJANI -> mapOf(
        "status_good" to "Telefonunuz normal işləyir", "status_attention" to "Diqqət tələb edən məqamlar var", "status_critical" to "Vacib problemlər aşkarlandı", "status_recommended" to "Tövsiyə olunan yoxlamalar", "findings" to "Aşkar edilənlər", "findings_count" to "%d nəticə · cəmi: %d", "recommended" to "Növbəti addımlar", "storage_low_title" to "Yaddaşda boş yer azalır", "storage_low_detail" to "%1\$s / %2\$s boşdur", "low_memory_title" to "Yaddaş yükü yüksəkdir", "low_memory_detail" to "Android yaddaş təzyiqinin yüksək olduğunu bildirir", "thermal_title" to "Telefon normaldan istidir", "thermal_detail" to "Temperatur vəziyyəti: %s", "low_battery_title" to "Batareya zəifdir", "low_battery_detail" to "Qalan enerji: %d%%", "battery_health_title" to "Batareya xəbərdarlığı", "battery_health_detail" to "Android bildirir: %s", "old_patch_title" to "Təhlükəsizlik yaması köhnə ola bilər", "old_patch_detail" to "Yamağın yaşı təxminən %d gündür", "hardware_failed_title" to "Aparat testi problem bildirdi", "hardware_failed_detail" to "Problem bildirən testlər: %d", "hardware_pending_title" to "Aparat testləri tamamlanmayıb", "hardware_pending_detail" to "Təsdiq gözləyən testlər: %d", "large_files_title" to "Böyük fayllar tapıldı", "downloads_title" to "Yükləmələri yoxlayın", "screenshots_title" to "Ekran görüntülərini yoxlayın", "reviewable_detail" to "%d obyekt · %s yoxlanıla bilər", "storage_scan_title" to "Yaddaş planınızı yaradın", "storage_scan_detail" to "Ölçülmüş nəticələr üçün yaddaş analizini başladın", "review" to "Yoxla", "details" to "Cihaz məlumatları", "show_details" to "Ətraflı göstər", "hide_details" to "Ətraflı gizlət", "no_scan" to "Yaddaş skanı hələ aparılmayıb",
    )
    ToolivaLanguage.TURKISH -> mapOf(
        "status_good" to "Telefonunuz normal çalışıyor", "status_attention" to "Dikkat edilmesi gereken noktalar var", "status_critical" to "Önemli sorunlar bulundu", "status_recommended" to "Önerilen kontroller", "findings" to "Bulduklarımız", "findings_count" to "%d bulgu · toplam: %d", "recommended" to "Sonraki adımlar", "storage_low_title" to "Depolama alanı azalıyor", "storage_low_detail" to "%1\$s / %2\$s kullanılabilir", "low_memory_title" to "Bellek kullanımı yüksek", "low_memory_detail" to "Android düşük bellek baskısı bildiriyor", "thermal_title" to "Telefon normalden sıcak", "thermal_detail" to "Sıcaklık durumu: %s", "low_battery_title" to "Pil seviyesi düşük", "low_battery_detail" to "Kalan pil: %d%%", "battery_health_title" to "Pil sağlığı uyarısı", "battery_health_detail" to "Android bildiriyor: %s", "old_patch_title" to "Güvenlik yaması eski olabilir", "old_patch_detail" to "Yama yaklaşık %d günlük", "hardware_failed_title" to "Donanım testi sorun bildirdi", "hardware_failed_detail" to "Sorun bildiren test: %d", "hardware_pending_title" to "Donanım testleri tamamlanmadı", "hardware_pending_detail" to "Onayınızı bekleyen test: %d", "large_files_title" to "Büyük dosyalar bulundu", "downloads_title" to "İndirilenleri inceleyin", "screenshots_title" to "Ekran görüntülerini inceleyin", "reviewable_detail" to "%d öğe · %s incelenebilir", "storage_scan_title" to "Depolama planınızı oluşturun", "storage_scan_detail" to "Ölçülen sonuçlar için depolama analizini başlatın", "review" to "İncele", "details" to "Cihaz ayrıntıları", "show_details" to "Ayrıntıları göster", "hide_details" to "Ayrıntıları gizle", "no_scan" to "Henüz depolama taraması yapılmadı",
    )
}

val LocalToolivaStrings = staticCompositionLocalOf { ToolivaStrings.forLanguage(ToolivaLanguage.ENGLISH) }
