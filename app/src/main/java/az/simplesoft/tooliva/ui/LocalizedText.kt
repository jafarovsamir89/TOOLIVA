package az.simplesoft.tooliva.ui

import androidx.compose.material3.Icon as MaterialIcon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text as MaterialText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import az.simplesoft.tooliva.core.settings.ToolivaLanguage

/**
 * The product currently keeps copy in Kotlin because screens are Compose-first.
 * This wrapper makes every imported Material Text go through the same language
 * table while leaving filenames, package names and notification content intact.
 */
@Composable
fun LocalizedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = androidx.compose.material3.LocalTextStyle.current,
) {
    MaterialText(
        text = localizeToolivaText(text, LocalToolivaStrings.current.language),
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        onTextLayout = onTextLayout,
        style = style,
    )
}

@Composable
fun LocalizedIcon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    MaterialIcon(
        imageVector = imageVector,
        contentDescription = contentDescription?.let { localizeToolivaText(it, LocalToolivaStrings.current.language) },
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun LocalizedIcon(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    MaterialIcon(
        painter = painter,
        contentDescription = contentDescription?.let { localizeToolivaText(it, LocalToolivaStrings.current.language) },
        modifier = modifier,
        tint = tint,
    )
}

@Composable
fun LocalizedText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, androidx.compose.foundation.text.InlineTextContent> = mapOf(),
    onTextLayout: (TextLayoutResult) -> Unit = {},
    style: TextStyle = androidx.compose.material3.LocalTextStyle.current,
) {
    MaterialText(
        text = text,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign,
        lineHeight = lineHeight,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        minLines = minLines,
        inlineContent = inlineContent,
        onTextLayout = onTextLayout,
        style = style,
    )
}

private data class Translation(val source: String, val russian: String, val azerbaijani: String, val turkish: String)

private val translations = listOf(
    Translation("Back", "Назад", "Geri", "Geri"),
    Translation("Cancel", "Отмена", "Ləğv et", "İptal"),
    Translation("Close", "Закрыть", "Bağla", "Kapat"),
    Translation("Done", "Готово", "Hazırdır", "Bitti"),
    Translation("Save", "Сохранить", "Yadda saxla", "Kaydet"),
    Translation("OK", "ОК", "OK", "Tamam"),
    Translation("Open", "Открыть", "Aç", "Aç"),
    Translation("Share", "Поделиться", "Paylaş", "Paylaş"),
    Translation("Delete", "Удалить", "Sil", "Sil"),
    Translation("Clear", "Очистить", "Təmizlə", "Temizle"),
    Translation("Select all", "Выбрать всё", "Hamısını seç", "Tümünü seç"),
    Translation("Clear all", "Очистить всё", "Hamısını təmizlə", "Tümünü temizle"),
    Translation("Search", "Поиск", "Axtarış", "Ara"),
    Translation("Details", "Подробности", "Təfərrüatlar", "Ayrıntılar"),
    Translation("Review", "Проверить", "Yoxla", "İncele"),
    Translation("Refresh", "Обновить", "Yenilə", "Yenile"),
    Translation("Try again", "Повторить", "Yenidən cəhd et", "Tekrar dene"),
    Translation("Analyze", "Анализировать", "Analiz et", "Analiz et"),
    Translation("Analyze storage", "Анализировать хранилище", "Yaddaşı analiz et", "Depolamayı analiz et"),
    Translation("Analyze again", "Повторить анализ", "Analizi yenidən başlat", "Tekrar analiz et"),
    Translation("Loading…", "Загрузка…", "Yüklənir…", "Yükleniyor…"),
    Translation("Analyzing…", "Анализ…", "Analiz edilir…", "Analiz ediliyor…"),
    Translation("Scanning…", "Сканирование…", "Skan edilir…", "Taranıyor…"),
    Translation("Cancel scan", "Отменить сканирование", "Skanı ləğv et", "Taramayı iptal et"),
    Translation("Settings", "Настройки", "Ayarlar", "Ayarlar"),
    Translation("Language", "Язык", "Dil", "Dil"),
    Translation("Appearance", "Внешний вид", "Görünüş", "Görünüm"),
    Translation("Access", "Доступ", "Giriş", "Erişim"),
    Translation("Enabled", "Включён", "Aktivdir", "Etkin"),
    Translation("Disabled", "Выключен", "Deaktivdir", "Devre dışı"),
    Translation("Unavailable", "Недоступно", "Mövcud deyil", "Kullanılamıyor"),
    Translation("Storage", "Хранилище", "Yaddaş", "Depolama"),
    Translation("Memory", "Память", "Yaddaş", "Bellek"),
    Translation("Battery", "Батарея", "Batareya", "Pil"),
    Translation("Files", "Файлы", "Fayllar", "Dosyalar"),
    Translation("Clean", "Очистка", "Təmizlə", "Temizle"),
    Translation("Tools", "Инструменты", "Alətlər", "Araçlar"),
    Translation("More", "Ещё", "Daha çox", "Daha fazla"),
    Translation("Home", "Главная", "Əsas", "Ana sayfa"),
    Translation("Phone Doctor", "Диагностика телефона", "Telefon diaqnostikası", "Telefon Doktoru"),
    Translation("Hardware Tests", "Аппаратные тесты", "Aparat testləri", "Donanım testleri"),
    Translation("Phone Optimizer", "Оптимизатор телефона", "Telefon optimizatoru", "Telefon optimize edici"),
    Translation("App Manager", "Менеджер приложений", "Tətbiq meneceri", "Uygulama yöneticisi"),
    Translation("Notification History", "История уведомлений", "Bildiriş tarixçəsi", "Bildirim geçmişi"),
    Translation("Storage Map", "Карта хранилища", "Yaddaş xəritəsi", "Depolama haritası"),
    Translation("Scan history", "История сканирований", "Skan tarixçəsi", "Tarama geçmişi"),
    Translation("Large files", "Большие файлы", "Böyük fayllar", "Büyük dosyalar"),
    Translation("Downloads", "Загрузки", "Yükləmələr", "İndirilenler"),
    Translation("Screenshots", "Скриншоты", "Ekran görüntüləri", "Ekran görüntüleri"),
    Translation("Exact duplicates", "Точные дубликаты", "Dəqiq dublikatlar", "Aynı dosyalar"),
    Translation("Cleanup Swipe", "Очистка свайпом", "Sürüşdürərək təmizləmə", "Kaydırarak temizleme"),
    Translation("Cache Cleaner", "Очистка кэша", "Keş təmizləyicisi", "Önbellek temizleyici"),
    Translation("Photo Analyzer", "Анализ фото", "Foto analizatoru", "Fotoğraf analizörü"),
    Translation("Empty folders", "Пустые папки", "Boş qovluqlar", "Boş klasörler"),
    Translation("Old files", "Старые файлы", "Köhnə fayllar", "Eski dosyalar"),
    Translation("Recycle Bin", "Корзина", "Zibil qutusu", "Geri dönüşüm kutusu"),
    Translation("External sources", "Внешние источники", "Xarici mənbələr", "Harici kaynaklar"),
    Translation("Check My Phone", "Проверка телефона", "Telefonumu yoxla", "Telefonumu kontrol et"),
    Translation("Start checkup", "Запустить проверку", "Yoxlamanı başlat", "Kontrolü başlat"),
    Translation("Checkup complete", "Проверка завершена", "Yoxlama tamamlandı", "Kontrol tamamlandı"),
    Translation("What we found", "Что найдено", "Aşkar edilənlər", "Bulduklarımız"),
    Translation("Recommended next steps", "Что сделать дальше", "Növbəti addımlar", "Önerilen sonraki adımlar"),
    Translation("Needs attention", "Требует внимания", "Diqqət tələb edir", "Dikkat gerekiyor"),
    Translation("No immediate issues detected", "Срочных проблем не обнаружено", "Təcili problem aşkarlanmadı", "Acil bir sorun tespit edilmedi"),
    Translation("Security patch may be outdated", "Патч безопасности может быть устаревшим", "Təhlükəsizlik yeniləməsi köhnəlmiş ola bilər", "Güvenlik yaması eski olabilir"),
    Translation("Device and system", "Устройство и система", "Cihaz və sistem", "Cihaz ve sistem"),
    Translation("Display", "Экран", "Ekran", "Ekran"),
    Translation("Sensors", "Датчики", "Sensorlar", "Sensörler"),
    Translation("Thermal", "Температура системы", "Sistem temperaturu", "Sistem sıcaklığı"),
    Translation("Security patch", "Патч безопасности", "Təhlükəsizlik yeniləməsi", "Güvenlik yaması"),
    Translation("Storage review plan", "План проверки хранилища", "Yaddaş yoxlama planı", "Depolama inceleme planı"),
    Translation("Cleanup tools", "Инструменты очистки", "Təmizləmə alətləri", "Temizleme araçları"),
    Translation("Find identical files and safely keep one copy", "Найдите одинаковые файлы и сохраните одну копию", "Eyni faylları tapın və bir nüsxəni saxlayın", "Aynı dosyaları bulun ve bir kopyayı güvenle saklayın"),
    Translation("Find the biggest files worth reviewing", "Найдите самые большие файлы для проверки", "Yoxlanmalı ən böyük faylları tapın", "İncelenmeye değer en büyük dosyaları bulun"),
    Translation("Review installers, archives, documents and old downloads", "Проверьте установщики, архивы, документы и старые загрузки", "Quraşdırıcıları, arxivləri, sənədləri və köhnə yükləmələri yoxlayın", "Kurulum dosyalarını, arşivleri, belgeleri ve eski indirmeleri inceleyin"),
    Translation("Review old screenshots by age", "Проверьте старые скриншоты по возрасту", "Köhnə ekran görüntülərini yaşına görə yoxlayın", "Eski ekran görüntülerini yaşına göre inceleyin"),
    Translation("Review real files. Nothing is deleted automatically.", "Проверяйте реальные файлы. Ничего не удаляется автоматически.", "Real faylları yoxlayın. Heç nə avtomatik silinmir.", "Gerçek dosyaları inceleyin. Hiçbir şey otomatik silinmez."),
    Translation("One scan builds a live review plan. Nothing is selected or deleted automatically.", "Один анализ создаёт актуальный план проверки. Ничего не выбирается и не удаляется автоматически.", "Bir skan canlı yoxlama planı yaradır. Heç nə avtomatik seçilmir və silinmir.", "Tek tarama canlı bir inceleme planı oluşturur. Hiçbir şey otomatik seçilmez veya silinmez."),
    Translation("Full Storage Access is needed", "Нужен полный доступ к хранилищу", "Tam yaddaş girişi tələb olunur", "Tam Depolama Erişimi gerekli"),
    Translation("Enable Full Storage Access", "Включить полный доступ к хранилищу", "Tam yaddaş girişini aktivləşdir", "Tam Depolama Erişimini etkinleştir"),
    Translation("Full Storage Mode", "Полный режим хранилища", "Tam yaddaş rejimi", "Tam Depolama Modu"),
    Translation("Limited Mode", "Ограниченный режим", "Məhdud rejim", "Sınırlı Mod"),
    Translation("Search name or path", "Поиск по имени или пути", "Ad və ya yola görə axtar", "Ad veya yolda ara"),
    Translation("Sort: Largest first", "Сортировка: сначала большие", "Sıralama: əvvəlcə böyüklər", "Sıralama: önce büyükler"),
    Translation("Sort: Newest", "Сортировка: сначала новые", "Sıralama: əvvəlcə yenilər", "Sıralama: önce yeniler"),
    Translation("Sort: Oldest", "Сортировка: сначала старые", "Sıralama: əvvəlcə köhnələr", "Sıralama: önce eskiler"),
    Translation("Sort: Name", "Сортировка: по имени", "Sıralama: ada görə", "Sıralama: ada göre"),
    Translation("All", "Все", "Hamısı", "Tümü"),
    Translation("Video", "Видео", "Video", "Video"),
    Translation("Image", "Изображения", "Şəkil", "Görüntü"),
    Translation("Audio", "Аудио", "Audio", "Ses"),
    Translation("Archives", "Архивы", "Arxivlər", "Arşivler"),
    Translation("Documents", "Документы", "Sənədlər", "Belgeler"),
    Translation("Other", "Другое", "Digər", "Diğer"),
    Translation("Name", "Имя", "Ad", "Ad"),
    Translation("Newest", "Новые", "Ən yeni", "Yeni"),
    Translation("Oldest", "Старые", "Ən köhnə", "Eski"),
    Translation("Size", "Размер", "Ölçü", "Boyut"),
    Translation("Age", "Возраст", "Yaş", "Yaş"),
    Translation("Any age", "Любой возраст", "İstənilən yaş", "Her yaş"),
    Translation("Any size", "Любой размер", "İstənilən ölçü", "Her boyut"),
    Translation("Analyze Downloads", "Анализировать загрузки", "Yükləmələri analiz et", "İndirilenleri analiz et"),
    Translation("Downloads analyzed", "Загрузки проанализированы", "Yükləmələr analiz edildi", "İndirilenler analiz edildi"),
    Translation("No matching Downloads", "Подходящих загрузок нет", "Uyğun yükləmə yoxdur", "Eşleşen indirme yok"),
    Translation("File details", "Сведения о файле", "Fayl məlumatları", "Dosya ayrıntıları"),
    Translation("Type", "Тип", "Növ", "Tür"),
    Translation("Path", "Путь", "Yol", "Yol"),
    Translation("Modified", "Изменён", "Dəyişdirilib", "Değiştirilme"),
    Translation("Open in Files", "Открыть в Файлах", "Fayllarda aç", "Dosyalarda aç"),
    Translation("No matching apps", "Подходящих приложений нет", "Uyğun tətbiq yoxdur", "Eşleşen uygulama yok"),
    Translation("Installed apps", "Установленные приложения", "Quraşdırılmış tətbiqlər", "Yüklü uygulamalar"),
    Translation("System app", "Системное приложение", "Sistem tətbiqi", "Sistem uygulaması"),
    Translation("User app", "Пользовательское приложение", "İstifadəçi tətbiqi", "Kullanıcı uygulaması"),
    Translation("Usage Access is optional", "Доступ к данным об использовании необязателен", "İstifadə məlumatlarına giriş məcburi deyil", "Kullanım Erişimi isteğe bağlıdır"),
    Translation("Open Usage Access", "Открыть доступ к данным использования", "İstifadə girişini aç", "Kullanım Erişimini aç"),
    Translation("Uninstall", "Удалить приложение", "Tətbiqi sil", "Kaldır"),
    Translation("No saved notifications yet", "Сохранённых уведомлений пока нет", "Hələ saxlanılmış bildiriş yoxdur", "Henüz kaydedilmiş bildirim yok"),
    Translation("No saved history", "Сохранённой истории нет", "Saxlanılmış tarixçə yoxdur", "Kaydedilmiş geçmiş yok"),
    Translation("Notification Access is off", "Доступ к уведомлениям выключен", "Bildiriş girişi bağlıdır", "Bildirim Erişimi kapalı"),
    Translation("Enable notification history", "Включить историю уведомлений", "Bildiriş tarixçəsini aktivləşdir", "Bildirim geçmişini etkinleştir"),
    Translation("History settings", "Настройки истории", "Tarixçə ayarları", "Geçmiş ayarları"),
    Translation("Clear all history", "Очистить всю историю", "Bütün tarixçəni təmizlə", "Tüm geçmişi temizle"),
    Translation("Privacy & Security", "Конфиденциальность и безопасность", "Məxfilik və təhlükəsizlik", "Gizlilik ve güvenlik"),
    Translation("About Tooliva", "О Tooliva", "Tooliva haqqında", "Tooliva hakkında"),
    Translation("Read summary", "Прочитать описание", "Xülasəni oxu", "Özeti oku"),
    Translation("Appearance, access and preferences", "Внешний вид, доступ и настройки", "Görünüş, giriş və seçimlər", "Görünüm, erişim ve tercihler"),
    Translation("Map", "Карта", "Xəritə", "Harita"),
    Translation("Root", "Корень", "Kök", "Kök dizin"),
    Translation("Folder", "Папка", "Qovluq", "Klasör"),
    Translation("File", "Файл", "Fayl", "Dosya"),
    Translation("Unknown", "Неизвестно", "Naməlum", "Bilinmiyor"),
    Translation("Up", "Вверх", "Yuxarı", "Yukarı"),
    Translation("List", "Список", "Siyahı", "Liste"),
    Translation("Sunburst", "Солнечная диаграмма", "Günəş diaqramı", "Güneş ışını grafiği"),
    Translation("Pass", "Пройдено", "Keçdi", "Geçti"),
    Translation("Problem", "Проблема", "Problem", "Sorun"),
    Translation("Why is this shown?", "Почему это показано?", "Bu niyə göstərilir?", "Bu neden gösteriliyor?"),
    Translation("Final review", "Финальная проверка", "Yekun yoxlama", "Son inceleme"),
    Translation("Open final review", "Открыть финальную проверку", "Yekun yoxlamanı aç", "Son incelemeyi aç"),
    Translation("Files to review", "Файлы для проверки", "Yoxlanılacaq fayllar", "İncelenecek dosyalar"),
    Translation("No files to review", "Нет файлов для проверки", "Yoxlanılacaq fayl yoxdur", "İncelenecek dosya yok"),
    Translation("No review candidates yet", "Кандидатов для проверки пока нет", "Hələ yoxlama namizədi yoxdur", "Henüz inceleme adayı yok"),
    Translation("Review files one by one", "Проверьте файлы по одному", "Faylları bir-bir yoxlayın", "Dosyaları tek tek inceleyin"),
    Translation("Nothing is selected automatically.", "Ничего не выбирается автоматически.", "Heç nə avtomatik seçilmir.", "Hiçbir şey otomatik seçilmez."),
    Translation("No preview text", "Нет текста для предпросмотра", "Önizləmə mətni yoxdur", "Önizleme metni yok"),
    Translation("Delete permanently", "Удалить навсегда", "Həmişəlik sil", "Kalıcı olarak sil"),
    Translation("Delete permanently?", "Удалить навсегда?", "Həmişəlik silinsin?", "Kalıcı olarak silinsin mi?"),
    Translation("Trash", "Корзина", "Zibil qutusu", "Çöp kutusu"),
    Translation("Move to", "Переместить в", "Buraya köçür", "Şuraya taşı"),
    Translation("Copy to", "Копировать в", "Buraya kopyala", "Şuraya kopyala"),
    Translation("Manage", "Управление", "İdarə et", "Yönet"),
    Translation("Got it", "Понятно", "Başa düşdüm", "Anladım"),
    Translation("Turn on", "Включить", "Aktiv et", "Aç"),
    Translation("Turn off", "Выключить", "Deaktiv et", "Kapat"),
    Translation("Optimize", "Оптимизировать", "Optimallaşdır", "Optimize et"),
    Translation("Reset", "Сбросить", "Sıfırla", "Sıfırla"),
    Translation("Reset coverage", "Сбросить покрытие", "Əhatəni sıfırla", "Kapsamı sıfırla"),
    Translation("Reset test results", "Сбросить результаты тестов", "Test nəticələrini sıfırla", "Test sonuçlarını sıfırla"),
    Translation("Reset test results?", "Сбросить результаты тестов?", "Test nəticələri sıfırlansın?", "Test sonuçları sıfırlansın mı?"),
    Translation("Test vibration", "Проверить вибрацию", "Vibrasiyanı yoxla", "Titreşimi test et"),
    Translation("Play test sound", "Воспроизвести тестовый звук", "Test səsini səsləndir", "Test sesini çal"),
    Translation("Start microphone test", "Запустить тест микрофона", "Mikrofon testini başlat", "Mikrofon testini başlat"),
    Translation("Start checkup", "Запустить проверку", "Yoxlamanı başlat", "Kontrolü başlat"),
    Translation("No immediate issues detected", "Срочных проблем не обнаружено", "Təcili problem aşkarlanmadı", "Acil bir sorun tespit edilmedi"),
    Translation("Waiting for Android…", "Ожидание ответа Android…", "Android cavabı gözlənilir…", "Android yanıtı bekleniyor…"),
    Translation("Waiting for sensor events…", "Ожидание событий датчиков…", "Sensor hadisələri gözlənilir…", "Sensör olayları bekleniyor…"),
    Translation("Listening…", "Слушаем…", "Dinlənilir…", "Dinleniyor…"),
    Translation("Signal detected", "Сигнал обнаружен", "Siqnal aşkarlandı", "Sinyal algılandı"),
    Translation("Input level", "Уровень сигнала", "Giriş səviyyəsi", "Giriş seviyesi"),
    Translation("Torch is on", "Фонарик включён", "Fənər aktivdir", "El feneri açık"),
    Translation("Torch is off", "Фонарик выключен", "Fənər söndürülüb", "El feneri kapalı"),
    Translation("Close live values", "Закрыть текущие значения", "Cari göstəriciləri bağla", "Canlı değerleri kapat"),
    Translation("Browse shared storage", "Просмотр общего хранилища", "Paylaşılan yaddaşa baxış", "Paylaşılan depolamaya göz at"),
    Translation("Allow access", "Разрешить доступ", "Girişə icazə ver", "Erişime izin ver"),
    Translation("Use SD / USB / cloud sources", "Использовать SD / USB / облачные источники", "SD / USB / bulud mənbələrindən istifadə et", "SD / USB / bulut kaynaklarını kullan"),
    Translation("SD / USB / cloud sources", "Источники SD / USB / облако", "SD / USB / bulud mənbələri", "SD / USB / bulut kaynakları"),
    Translation("Add storage or cloud folder", "Добавить папку хранилища или облака", "Yaddaş və ya bulud qovluğu əlavə et", "Depolama veya bulut klasörü ekle"),
    Translation("User-selected access", "Доступ, выбранный пользователем", "İstifadəçinin seçdiyi giriş", "Kullanıcının seçtiği erişim"),
    Translation("Open a folder to browse. Searches and category scans start only when you ask.", "Откройте папку для просмотра. Поиск и анализ категорий запускаются только по вашему запросу.", "Baxmaq üçün qovluq açın. Axtarış və kateqoriya skanı yalnız istəyinizlə başlayır.", "Göz atmak için bir klasör açın. Aramalar ve kategori taramaları yalnızca siz istediğinizde başlar."),
    Translation("No files here", "Здесь нет файлов", "Burada fayl yoxdur", "Burada dosya yok"),
    Translation("Try another folder or search.", "Попробуйте другую папку или поиск.", "Başqa qovluq və ya axtarış sınayın.", "Başka bir klasör veya arama deneyin."),
    Translation("Search this folder", "Искать в этой папке", "Bu qovluqda axtar", "Bu klasörde ara"),
    Translation("Search in this folder", "Поиск в этой папке", "Bu qovluqda axtarış", "Bu klasörde ara"),
    Translation("Clear selection", "Снять выделение", "Seçimi təmizlə", "Seçimi temizle"),
    Translation("More actions", "Другие действия", "Digər əməliyyatlar", "Diğer işlemler"),
    Translation("Choose an action for the selected items.", "Выберите действие для выбранных объектов.", "Seçilmiş obyekt üçün əməliyyat seçin.", "Seçili öğeler için bir işlem seçin."),
    Translation("Rename first selected", "Переименовать первый выбранный", "İlk seçiləni adlandır", "İlk seçileni yeniden adlandır"),
    Translation("New folder", "Новая папка", "Yeni qovluq", "Yeni klasör"),
    Translation("Folder name", "Имя папки", "Qovluq adı", "Klasör adı"),
    Translation("Create", "Создать", "Yarat", "Oluştur"),
    Translation("Preview", "Предпросмотр", "Önizləmə", "Önizleme"),
    Translation("Extract here", "Распаковать здесь", "Buraya çıxar", "Buraya çıkar"),
    Translation("Close", "Закрыть", "Bağla", "Kapat"),
    Translation("Readable", "Доступен для чтения", "Oxuna bilən", "Okunabilir"),
    Translation("Writable", "Доступен для записи", "Yazıla bilən", "Yazılabilir"),
    Translation("No matching apps", "Подходящих приложений нет", "Uyğun tətbiq yoxdur", "Eşleşen uygulama yok"),
    Translation("No apps in this view", "В этом представлении нет приложений", "Bu görünüşdə tətbiq yoxdur", "Bu görünümde uygulama yok"),
    Translation("Loading visible apps…", "Загрузка доступных приложений…", "Görünən tətbiqlər yüklənir…", "Görünen uygulamalar yükleniyor…"),
    Translation("Reading visible apps…", "Чтение доступных приложений…", "Görünən tətbiqlər oxunur…", "Görünen uygulamalar okunuyor…"),
    Translation("Older than", "Старше чем", "Bundan köhnə", "Şundan eski"),
    Translation("Select all removable", "Выбрать все удаляемые", "Silinə bilənlərin hamısını seç", "Kaldırılabilir olanların tümünü seç"),
    Translation("App details", "Сведения о приложении", "Tətbiq məlumatları", "Uygulama ayrıntıları"),
    Translation("Android App info", "Сведения о приложении Android", "Android tətbiq məlumatları", "Android uygulama bilgisi"),
    Translation("Calculating…", "Вычисление…", "Hesablanır…", "Hesaplanıyor…"),
    Translation("Loading Android storage and usage details…", "Загрузка данных о хранилище и использовании…", "Android yaddaş və istifadə məlumatları yüklənir…", "Android depolama ve kullanım ayrıntıları yükleniyor…"),
    Translation("Usage Access required for last-used information.", "Для данных о последнем использовании нужен доступ к статистике использования.", "Son istifadə məlumatları üçün İstifadə girişinə icazə lazımdır.", "Son kullanım bilgileri için Kullanım Erişimi gerekir."),
    Translation("Usage information unavailable.", "Данные об использовании недоступны.", "İstifadə məlumatları mövcud deyil.", "Kullanım bilgileri kullanılamıyor."),
    Translation("Search app or package", "Поиск приложения или пакета", "Tətbiq və ya paket axtarışı", "Uygulama veya paket ara"),
    Translation("Notification details", "Сведения об уведомлении", "Bildiriş məlumatları", "Bildirim ayrıntıları"),
    Translation("Continue to Android settings", "Перейти в настройки Android", "Android ayarlarına keç", "Android ayarlarına git"),
    Translation("Clear notification history?", "Очистить историю уведомлений?", "Bildiriş tarixçəsi təmizlənsin?", "Bildirim geçmişi temizlensin mi?"),
    Translation("Delete all notification history stored on this device? Pinned notifications are included.", "Удалить всю историю уведомлений на этом устройстве? Закреплённые уведомления тоже будут удалены.", "Bu cihazda saxlanılan bütün bildiriş tarixçəsi silinsin? Bərkidilmiş bildirişlər də daxildir.", "Bu cihazda saklanan tüm bildirim geçmişi silinsin mi? Sabitlenen bildirimler de dahildir."),
    Translation("Exclude this app?", "Исключить это приложение?", "Bu tətbiq istisna edilsin?", "Bu uygulama hariç tutulsun mu?"),
    Translation("Delete existing", "Удалить существующие", "Mövcud olanları sil", "Mevcut olanları sil"),
    Translation("Keep existing", "Оставить существующие", "Mövcud olanları saxla", "Mevcut olanları tut"),
    Translation("History paused — new notifications are not being saved.", "История приостановлена — новые уведомления не сохраняются.", "Tarixçə dayandırılıb — yeni bildirişlər saxlanılmır.", "Geçmiş duraklatıldı — yeni bildirimler kaydedilmiyor."),
    Translation("Search app, title or text", "Поиск приложения, заголовка или текста", "Tətbiq, başlıq və ya mətn axtarışı", "Uygulama, başlık veya metin ara"),
    Translation("All apps", "Все приложения", "Bütün tətbiqlər", "Tüm uygulamalar"),
    Translation("Exclude an app", "Исключить приложение", "Tətbiqi istisna et", "Bir uygulamayı hariç tut"),
    Translation("Include", "Включить", "Daxil et", "Dahil et"),
    Translation("Exclude", "Исключить", "İstisna et", "Hariç tut"),
    Translation("None", "Нет", "Heç biri", "Yok"),
    Translation("Pin", "Закрепить", "Bərkid", "Sabitle"),
    Translation("Unpin", "Открепить", "Bərkitməni götür", "Sabitlemeyi kaldır"),
    Translation("Scan history", "История сканирований", "Skan tarixçəsi", "Tarama geçmişi"),
    Translation("Local snapshots from completed storage scans", "Локальные снимки завершённых сканирований хранилища", "Tamamlanmış yaddaş skanlarının lokal görüntüləri", "Tamamlanan depolama taramalarının yerel kayıtları"),
    Translation("No completed scans yet", "Завершённых сканирований пока нет", "Hələ tamamlanmış skan yoxdur", "Henüz tamamlanan tarama yok"),
    Translation("Run Analyze storage from Clean to start building a local history.", "Запустите «Анализировать хранилище» в разделе «Очистка», чтобы начать локальную историю.", "Lokal tarixçəni yaratmaq üçün «Təmizlə» bölməsindən «Yaddaşı analiz et» seçin.", "Yerel geçmişi oluşturmaya başlamak için Temizle bölümünden Depolamayı analiz et seçeneğini çalıştırın."),
    Translation("Storage Map", "Карта хранилища", "Yaddaş xəritəsi", "Depolama haritası"),
    Translation("See which folders use the most storage.", "Посмотрите, какие папки занимают больше всего места.", "Ən çox yaddaş tutan qovluqları görün.", "En çok depolama alanını hangi klasörlerin kullandığını görün."),
    Translation("Ready to analyze", "Готово к анализу", "Analizə hazırdır", "Analiz için hazır"),
    Translation("Analyzing storage…", "Анализ хранилища…", "Yaddaş analiz edilir…", "Depolama analiz ediliyor…"),
    Translation("Storage roots", "Корни хранилища", "Yaddaş kökləri", "Depolama kökleri"),
    Translation("No accessible child folders were found.", "Доступные вложенные папки не найдены.", "Əlçatan alt qovluq tapılmadı.", "Erişilebilir alt klasör bulunamadı."),
    Translation("Tap a folder to drill down. Use details or Open in Files for actions.", "Нажмите на папку, чтобы открыть вложенный уровень. Для действий используйте сведения или «Открыть в Файлах».", "Daha dərinə keçmək üçün qovluğa toxunun. Əməliyyatlar üçün məlumatlardan və ya «Fayllarda aç» seçimindən istifadə edin.", "Alt klasörlere inmek için bir klasöre dokunun. İşlemler için ayrıntıları veya Dosyalarda aç seçeneğini kullanın."),
    Translation("Tap a segment", "Нажмите на сегмент", "Seqmentə toxunun", "Bir bölüme dokunun"),
    Translation("Cleanup result", "Результат очистки", "Təmizləmə nəticəsi", "Temizleme sonucu"),
    Translation("Operation complete", "Операция завершена", "Əməliyyat tamamlandı", "İşlem tamamlandı"),
    Translation("Operation canceled", "Операция отменена", "Əməliyyat ləğv edildi", "İşlem iptal edildi"),
    Translation("Cleanup Swipe", "Очистка свайпом", "Sürüşdürərək təmizləmə", "Kaydırarak temizleme"),
    Translation("Review media cleanup", "Проверка очистки медиафайлов", "Media təmizləməsini yoxla", "Medya temizliğini incele"),
    Translation("Scanning screenshot buckets…", "Сканирование групп скриншотов…", "Ekran görüntüsü qrupları skan edilir…", "Ekran görüntüsü grupları taranıyor…"),
    Translation("Screenshot scan needs attention", "Сканирование скриншотов требует внимания", "Ekran görüntüsü skanı diqqət tələb edir", "Ekran görüntüsü taraması dikkat gerektiriyor"),
    Translation("Choose a category", "Выберите категорию", "Kateqoriya seçin", "Bir kategori seçin"),
    Translation("Choose a scope and age, then scan to build the list.", "Выберите область и возраст, затем запустите сканирование для создания списка.", "Əhatə dairəsini və yaşı seçin, sonra siyahını yaratmaq üçün skan edin.", "Kapsamı ve yaşı seçin, ardından listeyi oluşturmak için tarayın."),
    Translation("Analyze media", "Анализировать медиа", "Medianı analiz et", "Medyayı analiz et"),
    Translation("Photo Analyzer", "Анализ фото", "Foto analizatoru", "Fotoğraf analizörü"),
    Translation("No exact duplicates found", "Точных дубликатов не найдено", "Dəqiq dublikat tapılmadı", "Aynı dosya bulunamadı"),
    Translation("Find large files", "Найти большие файлы", "Böyük faylları tap", "Büyük dosyaları bul"),
    Translation("Scan large files", "Сканировать большие файлы", "Böyük faylları skan et", "Büyük dosyaları tara"),
    Translation("Old Downloads", "Старые загрузки", "Köhnə yükləmələr", "Eski indirilenler"),
    Translation("Scan old files", "Сканировать старые файлы", "Köhnə faylları skan et", "Eski dosyaları tara"),
    Translation("Analyze duplicates", "Анализировать дубликаты", "Dublikatları analiz et", "Aynı dosyaları analiz et"),
    Translation("Analyze cache", "Анализировать кэш", "Keşi analiz et", "Önbelleği analiz et"),
    Translation("Review and clear app caches", "Проверьте и очистите кэш приложений", "Tətbiq keşlərini yoxlayın və təmizləyin", "Uygulama önbelleklerini inceleyin ve temizleyin"),
    Translation("What will be cleaned?", "Что будет очищено?", "Nə təmizlənəcək?", "Ne temizlenecek?"),
    Translation("App caches", "Кэш приложений", "Tətbiq keşləri", "Uygulama önbellekleri"),
    Translation("Measure app cache sizes", "Измерить размер кэша приложений", "Tətbiq keşlərinin ölçüsünü ölç", "Uygulama önbelleği boyutlarını ölç"),
    Translation("Temporary app cache cleanup", "Очистка временного кэша приложений", "Müvəqqəti tətbiq keşinin təmizlənməsi", "Geçici uygulama önbelleği temizliği"),
    Translation("System cache cleanup isn't available on this device.", "Очистка системного кэша недоступна на этом устройстве.", "Sistem keşinin təmizlənməsi bu cihazda mövcud deyil.", "Sistem önbelleği temizliği bu cihazda kullanılamıyor."),
    Translation("Optimization complete", "Оптимизация завершена", "Optimallaşdırma tamamlandı", "Optimizasyon tamamlandı"),
    Translation("Optimization canceled", "Оптимизация отменена", "Optimallaşdırma ləğv edildi", "Optimizasyon iptal edildi"),
    Translation("Memory information unavailable", "Сведения о памяти недоступны", "Yaddaş məlumatları mövcud deyil", "Bellek bilgileri kullanılamıyor"),
    Translation("Storage information unavailable", "Сведения о хранилище недоступны", "Yaddaş məlumatları mövcud deyil", "Depolama bilgileri kullanılamıyor"),
    Translation("Memory and temporary system cache", "Память и временный системный кэш", "Yaddaş və müvəqqəti sistem keşi", "Bellek ve geçici sistem önbelleği"),
    Translation("Open Files", "Открыть Файлы", "Faylları aç", "Dosyaları aç"),
    Translation("Large Files", "Большие файлы", "Böyük fayllar", "Büyük dosyalar"),
    Translation("Duplicates", "Дубликаты", "Dublikatlar", "Aynı dosyalar"),
    Translation("Delete selected", "Удалить выбранное", "Seçilənləri sil", "Seçilenleri sil"),
    Translation("Delete selected files?", "Удалить выбранные файлы?", "Seçilmiş fayllar silinsin?", "Seçili dosyalar silinsin mi?"),
    Translation("Delete selected items?", "Удалить выбранные объекты?", "Seçilmiş obyektlər silinsin?", "Seçili öğeler silinsin mi?"),
    Translation("Delete selected Downloads?", "Удалить выбранные загрузки?", "Seçilmiş yükləmələr silinsin?", "Seçili indirilenler silinsin mi?"),
    Translation("Delete selected old files?", "Удалить выбранные старые файлы?", "Seçilmiş köhnə fayllar silinsin?", "Seçili eski dosyalar silinsin mi?"),
    Translation("Delete selected duplicates?", "Удалить выбранные дубликаты?", "Seçilmiş dublikatlar silinsin?", "Seçili aynı dosyalar silinsin mi?"),
    Translation("Delete selected screenshots?", "Удалить выбранные скриншоты?", "Seçilmiş ekran görüntüləri silinsin?", "Seçili ekran görüntüleri silinsin mi?"),
    Translation("Delete selected review files?", "Удалить выбранные файлы для проверки?", "Seçilmiş yoxlama faylları silinsin?", "Seçili inceleme dosyaları silinsin mi?"),
    Translation("Move selected files to Trash?", "Переместить выбранные файлы в корзину?", "Seçilmiş fayllar zibil qutusuna köçürülsün?", "Seçili dosyalar çöp kutusuna taşınsın mı?"),
    Translation("Move selected screenshots to Trash?", "Переместить выбранные скриншоты в корзину?", "Seçilmiş ekran görüntüləri zibil qutusuna köçürülsün?", "Seçili ekran görüntüleri çöp kutusuna taşınsın mı?"),
    Translation("No safe empty folders found.", "Безопасных пустых папок не найдено.", "Təhlükəsiz boş qovluq tapılmadı.", "Güvenli boş klasör bulunamadı."),
    Translation("Scan empty folders", "Сканировать пустые папки", "Boş qovluqları skan et", "Boş klasörleri tara"),
    Translation("Delete empty folders?", "Удалить пустые папки?", "Boş qovluqlar silinsin?", "Boş klasörler silinsin mi?"),
    Translation("If a name already exists", "Если такое имя уже существует", "Belə ad artıq varsa", "Aynı ad zaten varsa"),
    Translation("No external source added yet. Android will show available SD, USB and cloud providers in the picker.", "Внешние источники пока не добавлены. Android покажет доступные SD, USB и облачные источники в окне выбора.", "Hələ xarici mənbə əlavə edilməyib. Android seçim pəncərəsində mövcud SD, USB və bulud mənbələrini göstərəcək.", "Henüz harici kaynak eklenmedi. Android, seçicide kullanılabilir SD, USB ve bulut sağlayıcılarını gösterir."),
    Translation("Android controls which folders and actions this provider exposes.", "Android определяет, какие папки и действия предоставляет этот источник.", "Bu mənbənin hansı qovluq və əməliyyatları təqdim etdiyini Android idarə edir.", "Bu sağlayıcının hangi klasörleri ve işlemleri sunduğunu Android belirler."),
    Translation("No saved notifications yet", "Сохранённых уведомлений пока нет", "Hələ saxlanılmış bildiriş yoxdur", "Henüz kaydedilmiş bildirim yok"),
    Translation("No saved history", "Сохранённой истории нет", "Saxlanılmış tarixçə yoxdur", "Kaydedilmiş geçmiş yok"),
    Translation("Trash is empty", "Корзина пуста", "Zibil qutusu boşdur", "Çöp kutusu boş"),
    Translation("No files to review", "Нет файлов для проверки", "Yoxlanılacaq fayl yoxdur", "İncelenecek dosya yok"),
    Translation("Action plan", "План действий", "Fəaliyyət planı", "Eylem planı"),
    Translation("Cleaner analysis", "Анализ очистки", "Təmizləmə analizi", "Temizleyici analizi"),
    Translation("Categories", "Категории", "Kateqoriyalar", "Kategoriler"),
    Translation("Favorites", "Избранное", "Seçilmişlər", "Favoriler"),
    Translation("Recently opened", "Недавно открытые", "Son açılanlar", "Son açılanlar"),
    Translation("Internal storage", "Внутренняя память", "Daxili yaddaş", "Dahili depolama"),
    Translation("Choose a folder", "Выберите папку", "Qovluq seçin", "Bir klasör seçin"),
    Translation("Select this folder", "Выбрать эту папку", "Bu qovluğu seç", "Bu klasörü seç"),
    Translation("Parent folder", "Родительская папка", "Üst qovluq", "Üst klasör"),
    Translation("File tools", "Инструменты работы с файлами", "Fayl alətləri", "Dosya araçları"),
    Translation("Remove", "Удалить", "Sil", "Kaldır"),
    Translation("Rename", "Переименовать", "Adını dəyiş", "Yeniden adlandır"),
    Translation("Restore", "Восстановить", "Bərpa et", "Geri yükle"),
    Translation("Keep", "Оставить", "Saxla", "Tut"),
    Translation("Skip", "Пропустить", "Keç", "Atla"),
    Translation("Select", "Выбрать", "Seç", "Seç"),
    Translation("Continue", "Продолжить", "Davam et", "Devam et"),
    Translation("Stop", "Остановить", "Dayandır", "Durdur"),
    Translation("Delete folder?", "Удалить папку?", "Qovluq silinsin?", "Klasör silinsin mi?"),
    Translation("Delete selected files permanently?", "Удалить выбранные файлы навсегда?", "Seçilmiş fayllar həmişəlik silinsin?", "Seçili dosyalar kalıcı olarak silinsin mi?"),
    Translation("Deleting…", "Удаление…", "Silinir…", "Siliniyor…"),
    Translation("Preparing…", "Подготовка…", "Hazırlanır…", "Hazırlanıyor…"),
    Translation("Preparing cleanup…", "Подготовка очистки…", "Təmizləmə hazırlanır…", "Temizleme hazırlanıyor…"),
    Translation("Full Storage Access required", "Требуется полный доступ к хранилищу", "Tam yaddaş girişi tələb olunur", "Tam Depolama Erişimi gerekli"),
    Translation("Full Storage Access is required", "Требуется полный доступ к хранилищу", "Tam yaddaş girişi tələb olunur", "Tam Depolama Erişimi gerekli"),
    Translation("Limited media access", "Ограниченный доступ к медиафайлам", "Mediaya məhdud giriş", "Sınırlı medya erişimi"),
    Translation("Allow media access", "Разрешить доступ к медиафайлам", "Mediaya girişə icazə ver", "Medya erişimine izin ver"),
    Translation("Allow photo access", "Разрешить доступ к фото", "Fotolara girişə icazə ver", "Fotoğraf erişimine izin ver"),
    Translation("Grant media access", "Предоставить доступ к медиафайлам", "Media girişini ver", "Medya erişimi ver"),
    Translation("Scan needs attention", "Сканирование требует внимания", "Skan diqqət tələb edir", "Tarama dikkat gerektiriyor"),
    Translation("Storage analyzed", "Хранилище проанализировано", "Yaddaş analiz edildi", "Depolama analiz edildi"),
    Translation("Reviewable bytes are not a promise of reclaimable space. Each category opens its review flow.", "Объём для проверки не означает гарантированно освобождаемое место. Каждая категория открывает отдельную проверку.", "Yoxlanıla bilən həcm azad ediləcək yerə zəmanət vermir. Hər kateqoriya ayrıca yoxlama axını açır.", "İncelenebilir boyut, geri kazanılacak alan sözü değildir. Her kategori kendi inceleme akışını açar."),
    Translation("Find identical files and safely keep one copy.", "Найдите одинаковые файлы и безопасно оставьте одну копию.", "Eyni faylları tapın və bir nüsxəni təhlükəsiz saxlayın.", "Aynı dosyaları bulun ve bir kopyayı güvenle saklayın."),
    Translation("Tooliva compared matching-size files and verified their contents.", "Tooliva сравнил файлы одинакового размера и проверил их содержимое.", "Tooliva eyni ölçülü faylları müqayisə etdi və məzmununu yoxladı.", "Tooliva aynı boyuttaki dosyaları karşılaştırıp içeriklerini doğruladı."),
    Translation("No verified groups in this session", "В этой сессии нет подтверждённых групп", "Bu sessiyada təsdiqlənmiş qrup yoxdur", "Bu oturumda doğrulanmış grup yok"),
    Translation("Search filename or path", "Поиск по имени файла или пути", "Fayl adı və ya yol üzrə axtar", "Dosya adı veya yolda ara"),
    Translation("Keep this copy", "Оставить эту копию", "Bu nüsxəni saxla", "Bu kopyayı tut"),
    Translation("Find identical files and safely keep one copy", "Найдите одинаковые файлы и безопасно оставьте одну копию", "Eyni faylları tapın və bir nüsxəni təhlükəsiz saxlayın", "Aynı dosyaları bulun ve bir kopyayı güvenle saklayın"),
    Translation("Old installers and downloads you may no longer need", "Старые установщики и загрузки, которые могут быть вам больше не нужны", "Artıq lazım olmayan köhnə quraşdırıcılar və yükləmələr", "Artık ihtiyacınız olmayabilecek eski kurulum dosyaları ve indirilenler"),
    Translation("APK installers", "APK-установщики", "APK quraşdırıcıları", "APK kurulum dosyaları"),
    Translation("Why", "Почему", "Niyə", "Neden"),
    Translation("Unnamed", "Без названия", "Adsız", "Adsız"),
    Translation("Unnamed file", "Файл без названия", "Adsız fayl", "Adsız dosya"),
    Translation("Conservative age and scope filters. Nothing is selected automatically.", "Осторожные фильтры по возрасту и области. Ничего не выбирается автоматически.", "Yaş və əhatə üzrə ehtiyatlı filtrlər. Heç nə avtomatik seçilmir.", "Yaş ve kapsam filtreleri temkinlidir. Hiçbir şey otomatik seçilmez."),
    Translation("Limited Mode cannot safely enumerate shared-storage folders.", "Ограниченный режим не может безопасно перечислить папки общего хранилища.", "Məhdud rejim paylaşılan yaddaş qovluqlarını təhlükəsiz sadalaya bilmir.", "Sınırlı Mod, paylaşılan depolama klasörlerini güvenle listeleyemez."),
    Translation("Full Storage Access is needed", "Нужен полный доступ к хранилищу", "Tam yaddaş girişi tələb olunur", "Tam Depolama Erişimi gerekli"),
    Translation("Automatic recommendations need the Downloads folders. Limited Mode does not pretend to cover the whole folder.", "Для автоматических рекомендаций нужны папки загрузок. Ограниченный режим не выдаёт себя за полный анализ папки.", "Avtomatik tövsiyələr üçün Yükləmələr qovluqları lazımdır. Məhdud rejim bütün qovluğu əhatə etdiyini iddia etmir.", "Otomatik öneriler için İndirilenler klasörleri gerekir. Sınırlı Mod, klasörün tamamını kapsıyormuş gibi davranmaz."),
    Translation("Only files visible to Android and matching this category are shown.", "Показаны только файлы, доступные Android и подходящие под эту категорию.", "Yalnız Android üçün əlçatan və bu kateqoriyaya uyğun fayllar göstərilir.", "Yalnızca Android tarafından görülebilen ve bu kategoriye uyan dosyalar gösterilir."),
    Translation("Only safe, accessible folders that are empty at scan and deletion time are listed.", "Показываются только безопасные доступные папки, пустые во время сканирования и удаления.", "Yalnız skan və silinmə zamanı boş olan təhlükəsiz, əlçatan qovluqlar göstərilir.", "Yalnızca tarama ve silme sırasında boş olan güvenli, erişilebilir klasörler listelenir."),
    Translation("Run a scan to review folders. Root, protected Android and Tooliva folders are excluded.", "Запустите сканирование для проверки папок. Корень, защищённые папки Android и Tooliva исключены.", "Qovluqları yoxlamaq üçün skan başladın. Kök, qorunan Android və Tooliva qovluqları istisnadır.", "Klasörleri incelemek için tarama yapın. Kök, korumalı Android ve Tooliva klasörleri dışarıda bırakılır."),
    Translation("No files are selected for deletion. Go back and choose Delete for a file.", "Файлы для удаления не выбраны. Вернитесь назад и выберите «Удалить» для файла.", "Silmək üçün fayl seçilməyib. Geri qayıdın və fayl üçün «Sil» seçin.", "Silinecek dosya seçilmedi. Geri dönüp bir dosya için Sil seçeneğini seçin."),
    Translation("Nothing is deleted during review. Deletion starts only after the final confirmation.", "Во время проверки ничего не удаляется. Удаление начинается только после финального подтверждения.", "Yoxlama zamanı heç nə silinmir. Silinmə yalnız yekun təsdiqdən sonra başlayır.", "İnceleme sırasında hiçbir şey silinmez. Silme yalnızca son onaydan sonra başlar."),
    Translation("Swipe right to keep, left to delete, or up to skip. Buttons are always available below.", "Свайп вправо — оставить, влево — удалить, вверх — пропустить. Кнопки всегда доступны внизу.", "Sağa sürüşdür — saxla, sola — sil, yuxarı — keç. Düymələr aşağıda həmişə əlçatandır.", "Sağa kaydırarak tutun, sola kaydırarak silin, yukarı kaydırarak atlayın. Düğmeler aşağıda her zaman kullanılabilir."),
    Translation("On-device review. Nothing is selected or deleted automatically.", "Проверка на устройстве. Ничего не выбирается и не удаляется автоматически.", "Cihazda yoxlama. Heç nə avtomatik seçilmir və silinmir.", "Cihaz üzerinde inceleme. Hiçbir şey otomatik seçilmez veya silinmez."),
    Translation("Run an analysis to find possible issues locally.", "Запустите анализ, чтобы найти возможные проблемы локально.", "Mümkün problemləri lokal tapmaq üçün analiz başladın.", "Olası sorunları yerel olarak bulmak için analiz başlatın."),
    Translation("Checking screenshots and verifying the result…", "Проверка скриншотов и подтверждение результата…", "Ekran görüntüləri yoxlanılır və nəticə təsdiqlənir…", "Ekran görüntüleri kontrol ediliyor ve sonuç doğrulanıyor…"),
    Translation("Try another category, age or size filter.", "Попробуйте другой фильтр категории, возраста или размера.", "Başqa kateqoriya, yaş və ya ölçü filtri sınayın.", "Başka bir kategori, yaş veya boyut filtresi deneyin."),
    Translation("Dismiss", "Закрыть", "Bağla", "Kapat"),
    Translation("Search Downloads", "Поиск в загрузках", "Yükləmələrdə axtar", "İndirilenlerde ara"),
    Translation("No matching Downloads", "Подходящих загрузок нет", "Uyğun yükləmə yoxdur", "Eşleşen indirme yok"),
    Translation("Downloads analyzed", "Загрузки проанализированы", "Yükləmələr analiz edildi", "İndirilenler analiz edildi"),
    Translation("Analyze Downloads", "Анализировать загрузки", "Yükləmələri analiz et", "İndirilenleri analiz et"),
    Translation("Search locally", "Искать локально", "Lokal axtar", "Yerel olarak ara"),
    Translation("Factual local-processing summary", "Фактическое описание локальной обработки", "Lokal emalın faktiki xülasəsi", "Yerel işlemenin gerçeğe dayalı özeti"),
    Translation("Tooliva is an offline-first Android utility. No Pro, ads or billing are enabled in this build.", "Tooliva — Android-утилита с приоритетом автономной работы. В этой сборке нет Pro, рекламы и платежей.", "Tooliva oflayn işləməyə üstünlük verən Android alətidir. Bu yığımda Pro, reklam və ödəniş yoxdur.", "Tooliva çevrimdışı çalışmayı önceliklendiren bir Android aracıdır. Bu sürümde Pro, reklam veya ödeme etkin değil."),
    Translation("Real device checks, no invented health score", "Реальные проверки устройства, без выдуманной оценки здоровья", "Real cihaz yoxlamaları, uydurma sağlamlıq balı olmadan", "Gerçek cihaz kontrolleri, uydurma sağlık puanı yok"),
    Translation("Device information unavailable", "Сведения об устройстве недоступны", "Cihaz məlumatları mövcud deyil", "Cihaz bilgileri kullanılamıyor"),
    Translation("This hardware is not supported on this device.", "Это оборудование не поддерживается на данном устройстве.", "Bu avadanlıq bu cihazda dəstəklənmir.", "Bu donanım bu cihazda desteklenmiyor."),
    Translation("Test one component at a time", "Проверяйте по одному компоненту", "Hər dəfə bir komponenti yoxlayın", "Her seferinde bir bileşeni test edin"),
    Translation("All user-confirmed hardware results will return to Not tested.", "Все подтверждённые пользователем результаты оборудования вернутся в состояние «Не проверено».", "İstifadəçi tərəfindən təsdiqlənmiş aparat nəticələri «Yoxlanılmayıb» vəziyyətinə qayıdacaq.", "Kullanıcı tarafından onaylanan tüm donanım sonuçları Test edilmedi durumuna dönecek."),
    Translation("Press the button for a short vibration. Nothing starts automatically.", "Нажмите кнопку для короткой вибрации. Ничего не запускается автоматически.", "Qısa vibrasiya üçün düyməyə basın. Heç nə avtomatik başlamır.", "Kısa bir titreşim için düğmeye basın. Hiçbir şey otomatik başlamaz."),
    Translation("System touch vibration is currently disabled. Enable Touch feedback in Android settings before testing.", "Системная вибрация касаний отключена. Перед тестом включите тактильный отклик в настройках Android.", "Sistem toxunma vibrasiyası söndürülüb. Testdən əvvəl Android ayarlarında toxunma əks əlaqəsini aktiv edin.", "Sistem dokunma titreşimi kapalı. Testten önce Android ayarlarında Dokunma geri bildirimini etkinleştirin."),
    Translation("Turn the torch on, confirm it visually, then turn it off.", "Включите фонарик, убедитесь, что он работает, затем выключите его.", "Fənəri yandırın, işlədiyini yoxlayın və sonra söndürün.", "El fenerini açın, çalıştığını görsel olarak doğrulayın ve kapatın."),
    Translation("Cover and uncover the top of the phone.", "Закройте и откройте верхнюю часть телефона.", "Telefonun üst hissəsini örtün və açın.", "Telefonun üstünü kapatıp açın."),
    Translation("Move the phone to check for changing values.", "Переместите телефон, чтобы проверить изменение показаний.", "Göstəricilərin dəyişməsini yoxlamaq üçün telefonu tərpədin.", "Değerlerdeki değişimi kontrol etmek için telefonu hareket ettirin."),
    Translation("Speak near the phone to check for a changing signal.", "Скажите что-нибудь рядом с телефоном, чтобы проверить изменение сигнала.", "Siqnalın dəyişməsini yoxlamaq üçün telefonun yanında danışın.", "Sinyaldeki değişimi kontrol etmek için telefonun yanında konuşun."),
    Translation("A short neutral 440 Hz tone is generated locally. Tooliva does not change system volume.", "Короткий нейтральный тон 440 Гц создаётся локально. Tooliva не меняет системную громкость.", "Qısa neytral 440 Hz səs lokal yaradılır. Tooliva sistem səsini dəyişmir.", "Kısa, nötr bir 440 Hz tonu yerel olarak üretilir. Tooliva sistem sesini değiştirmez."),
    Translation("Tooliva never auto-passes a physical test. You decide whether the display, sound, touch or sensor behaved correctly.", "Tooliva никогда не засчитывает физический тест автоматически. Вы сами решаете, правильно ли работали экран, звук, сенсор или датчик.", "Tooliva fiziki testi heç vaxt avtomatik keçmiş saymır. Ekranın, səsin, toxunuşun və ya sensorun düzgün işləyib-işləmədiyini siz qərar verirsiniz.", "Tooliva fiziksel testi asla otomatik olarak başarılı saymaz. Ekranın, sesin, dokunmanın veya sensörün doğru çalışıp çalışmadığına siz karar verirsiniz."),
    Translation("Allow Usage Access so Android can provide cache statistics for the selected browsers and YouTube. This is not a runtime permission and no cache is changed.", "Выдайте доступ к статистике использования, чтобы Android показал размер кэша выбранных браузеров и YouTube. Это не системное разрешение, кэш не изменяется.", "Seçilmiş brauzerlər və YouTube üçün keş statistikasını almaq üçün İstifadə girişinə icazə verin. Bu iş vaxtı icazəsi deyil və keş dəyişdirilmir.", "Seçili tarayıcılar ve YouTube için Android'in önbellek istatistiklerini sağlaması amacıyla Kullanım Erişimine izin verin. Bu bir çalışma zamanı izni değildir ve önbellek değiştirilmez."),
    Translation("Open Usage Access settings", "Открыть настройки доступа к статистике использования", "İstifadə girişi ayarlarını aç", "Kullanım Erişimi ayarlarını aç"),
    Translation("Select apps here for review, then open their Android settings and press Clear cache yourself. Tooliva never changes app data or storage.", "Выберите приложения для проверки, затем откройте их настройки Android и сами нажмите «Очистить кэш». Tooliva не изменяет данные или хранилище приложений.", "Yoxlamaq üçün tətbiqləri seçin, sonra onların Android ayarlarını açıb «Keşi təmizlə» düyməsinə özünüz basın. Tooliva tətbiq məlumatlarını və yaddaşı dəyişmir.", "İncelemek için uygulamaları seçin, ardından Android ayarlarını açıp Önbelleği temizle düğmesine kendiniz basın. Tooliva uygulama verilerini veya depolamasını değiştirmez."),
    Translation("Only the selected app cache through Android's App Info storage controls. Cookies, passwords, history, downloads, accounts, settings and app data are not selected.", "Только кэш выбранного приложения через настройки хранилища Android. Файлы cookie, пароли, история, загрузки, аккаунты, настройки и данные приложения не выбираются.", "Yalnız Android Tətbiq məlumatları yaddaş idarəsi vasitəsilə seçilmiş tətbiqin keşi. Kukilər, parollar, tarixçə, yükləmələr, hesablar, ayarlar və tətbiq məlumatları seçilmir.", "Yalnızca Android'in Uygulama bilgisi depolama kontrolleri üzerinden seçilen uygulamanın önbelleği. Çerezler, parolalar, geçmiş, indirilenler, hesaplar, ayarlar ve uygulama verileri seçilmez."),
    Translation("Android controls the retention period and expiry date. Tooliva never permanently deletes Trash items automatically.", "Срок хранения и дата удаления определяются Android. Tooliva никогда не удаляет объекты из корзины навсегда автоматически.", "Saxlama müddətini və bitmə tarixini Android idarə edir. Tooliva zibil qutusundakı obyektləri heç vaxt avtomatik həmişəlik silmir.", "Saklama süresini ve sona erme tarihini Android belirler. Tooliva çöp kutusundaki öğeleri asla otomatik olarak kalıcı silmez."),
    Translation("Only Android-controlled MediaStore Trash items appear here.", "Здесь отображаются только объекты корзины MediaStore, которыми управляет Android.", "Burada yalnız Android-in idarə etdiyi MediaStore zibil qutusu obyektləri göstərilir.", "Burada yalnızca Android tarafından yönetilen MediaStore çöp kutusu öğeleri görünür."),
    Translation("Android limits direct folder aggregation until you allow access in system settings.", "Android ограничивает сбор данных по папкам, пока вы не разрешите доступ в системных настройках.", "Sistem ayarlarında girişə icazə verənədək Android qovluqlar üzrə birbaşa toplamanı məhdudlaşdırır.", "Sistem ayarlarında erişim izni verene kadar Android doğrudan klasör toplamasını sınırlar."),
    Translation("Analysis starts only when you tap Analyze storage. Files are not opened, hashed or thumbnailed.", "Анализ начинается только после нажатия «Анализировать хранилище». Файлы не открываются, не хешируются и не получают миниатюры.", "Analiz yalnız «Yaddaşı analiz et» düyməsinə toxunduqda başlayır. Fayllar açılmır, heşlənmir və miniatür yaradılmır.", "Analiz yalnızca Depolamayı analiz et seçeneğine dokunduğunuzda başlar. Dosyalar açılmaz, özetlenmez veya küçük resmi oluşturulmaz."),
    Translation("This map may be stale after a file operation. Analyze again manually to refresh it.", "После операции с файлами эта карта может устареть. Обновите её повторным анализом вручную.", "Fayl əməliyyatından sonra bu xəritə köhnələ bilər. Yeniləmək üçün əl ilə yenidən analiz edin.", "Bir dosya işleminden sonra bu harita güncelliğini yitirebilir. Yenilemek için yeniden manuel analiz yapın."),
    Translation("Android limits direct browsing until you allow access in system settings. Tooliva does not add another storage permission.", "Android ограничивает просмотр, пока вы не разрешите доступ в системных настройках. Tooliva не добавляет другое разрешение на хранилище.", "Sistem ayarlarında girişə icazə verənədək Android birbaşa baxışı məhdudlaşdırır. Tooliva əlavə yaddaş icazəsi istəmir.", "Sistem ayarlarında erişim izni verene kadar Android doğrudan göz atmayı sınırlar. Tooliva başka bir depolama izni eklemez."),
    Translation("Android may limit package visibility. This list is not presented as a complete device inventory.", "Android может ограничивать видимость пакетов. Этот список не является полным перечнем приложений устройства.", "Android paket görünüşünü məhdudlaşdıra bilər. Bu siyahı cihazdakı bütün tətbiqlərin tam siyahısı deyil.", "Android paket görünürlüğünü sınırlayabilir. Bu liste cihazdaki tüm uygulamaların eksiksiz envanteri değildir."),
    Translation("Apps without a real last-used timestamp are not classified as rarely used.", "Приложения без реальной даты последнего использования не считаются редко используемыми.", "Son istifadə vaxtı həqiqi olmayan tətbiqlər nadir istifadə olunan kimi təsnif edilmir.", "Gerçek bir son kullanım zamanı olmayan uygulamalar nadiren kullanılan olarak sınıflandırılmaz."),
    Translation("Grant it to see Android's last-used times, rarely-used review and app storage statistics. Tooliva does not read app content.", "Разрешите доступ, чтобы видеть время последнего использования, редко используемые приложения и статистику их хранилища. Tooliva не читает содержимое приложений.", "Son istifadə vaxtını, az istifadə olunan tətbiqləri və yaddaş statistikasını görmək üçün icazə verin. Tooliva tətbiq məzmununu oxumur.", "Son kullanım zamanlarını, nadir kullanılan uygulamaları ve uygulama depolama istatistiklerini görmek için izin verin. Tooliva uygulama içeriğini okumaz."),
    Translation("No recent usage recorded.", "Недавнее использование не зафиксировано.", "Son istifadə qeydə alınmayıb.", "Yakın zamanda kullanım kaydı yok."),
    Translation("Try another filter or search term.", "Попробуйте другой фильтр или поисковый запрос.", "Başqa filtr və ya axtarış sözü sınayın.", "Başka bir filtre veya arama terimi deneyin."),
    Translation("Android will show its own confirmation. Tooliva does not kill apps or promise a fake RAM boost.", "Android покажет собственное подтверждение. Tooliva не закрывает приложения принудительно и не обещает вымышленное ускорение RAM.", "Android öz təsdiqini göstərəcək. Tooliva tətbiqləri dayandırmır və saxta RAM artımı vəd etmir.", "Android kendi onayını gösterir. Tooliva uygulamaları zorla kapatmaz veya sahte RAM artışı vaat etmez."),
    Translation("These are before/after device readings. They are not presented as RAM freed by Tooliva.", "Это показания устройства до и после. Они не выдаются за объём RAM, освобождённый Tooliva.", "Bunlar cihazın əvvəlki və sonrakı göstəriciləridir. Tooliva tərəfindən boşaldılmış RAM kimi təqdim edilmir.", "Bunlar cihazın işlem öncesi ve sonrası ölçümleridir. Tooliva tarafından boşaltılan RAM olarak sunulmaz."),
    Translation("Optimization couldn't be completed", "Оптимизацию не удалось завершить", "Optimallaşdırmanı tamamlamaq mümkün olmadı", "Optimizasyon tamamlanamadı"),
    Translation("New notifications will appear after Android delivers them to Tooliva.", "Новые уведомления появятся после того, как Android передаст их Tooliva.", "Yeni bildirişlər Android onları Tooliva-ya göndərdikdən sonra görünəcək.", "Yeni bildirimler, Android onları Tooliva'ya ilettikten sonra görünür."),
    Translation("Grant Notification Access to save future notifications.", "Разрешите доступ к уведомлениям, чтобы сохранять будущие уведомления.", "Gələcək bildirişləri saxlamaq üçün Bildiriş girişinə icazə verin.", "Gelecekteki bildirimleri kaydetmek için Bildirim Erişimine izin verin."),
    Translation("Tooliva can only save notifications received after you explicitly enable access. Existing Android notifications cannot be recovered.", "Tooliva может сохранять только уведомления, полученные после явного включения доступа. Существующие уведомления Android восстановить нельзя.", "Tooliva yalnız girişi açdıqdan sonra alınan bildirişləri saxlaya bilər. Mövcud Android bildirişlərini bərpa etmək mümkün deyil.", "Tooliva yalnızca erişimi açıkça etkinleştirdikten sonra alınan bildirimleri kaydedebilir. Mevcut Android bildirimleri kurtarılamaz."),
    Translation("To save a private history of notifications, Tooliva needs Android Notification Access. When enabled, Tooliva can receive notification content Android provides, including the sending app, title, text and time. History stays on this device and is not uploaded or used for advertising. You can exclude apps and delete history at any time.", "Приватная история уведомлений требует доступа к уведомлениям Android. После включения Tooliva получает переданные Android приложение-источник, заголовок, текст и время. История остаётся на устройстве, не загружается на сервер и не используется для рекламы. Приложения можно исключить, а историю — удалить в любой момент.", "Şəxsi bildiriş tarixçəsini saxlamaq üçün Tooliva-ya Android Bildiriş girişi lazımdır. Aktiv olduqda Tooliva Android-in verdiyi tətbiq, başlıq, mətn və vaxt məlumatlarını qəbul edə bilər. Tarixçə cihazda qalır, yüklənmir və reklam üçün istifadə edilmir. Tətbiqləri istənilən vaxt istisna edə, tarixçəni silə bilərsiniz.", "Özel bildirim geçmişini kaydetmek için Tooliva'nın Android Bildirim Erişimine ihtiyacı vardır. Etkinleştirildiğinde Tooliva, Android'in sağladığı gönderen uygulama, başlık, metin ve zaman bilgilerini alabilir. Geçmiş bu cihazda kalır, yüklenmez ve reklam için kullanılmaz. Uygulamaları hariç tutabilir ve geçmişi istediğiniz zaman silebilirsiniz."),
    Translation("You can exclude banking, password-manager or private messaging apps if you do not want their notifications stored.", "Уведомления банковских, менеджеров паролей и личных мессенджеров можно исключить из сохранения.", "Bildirişlərinin saxlanmasını istəmədiyiniz bank, parol meneceri və ya şəxsi mesajlaşma tətbiqlərini istisna edə bilərsiniz.", "Bildirimlerinin saklanmasını istemediğiniz bankacılık, parola yöneticisi veya özel mesajlaşma uygulamalarını hariç tutabilirsiniz."),
    Translation("Keep history for", "История хранится", "Tarixçəni saxla", "Geçmişi saklama süresi"),
    Translation("Excluded apps", "Исключённые приложения", "İstisna edilmiş tətbiqlər", "Hariç tutulan uygulamalar"),
    Translation("Check my phone", "Проверить телефон", "Telefonumu yoxla", "Telefonumu kontrol et"),
    Translation("Copy", "Копировать", "Kopyala", "Kopyala"),
    Translation("Move", "Переместить", "Köçür", "Taşı"),
    Translation("Delete selected duplicates", "Удалить выбранные дубликаты", "Seçilmiş dublikatları sil", "Seçili aynı dosyaları sil"),
    Translation("Delete selected screenshots permanently?", "Удалить выбранные скриншоты навсегда?", "Seçilmiş ekran görüntüləri həmişəlik silinsin?", "Seçili ekran görüntüleri kalıcı olarak silinsin mi?"),
    Translation("Folder size is not calculated recursively", "Размер папки не рассчитывается рекурсивно", "Qovluğun ölçüsü rekursiv hesablanmır", "Klasör boyutu alt klasörler dahil hesaplanmaz"),
    Translation("Open Full Storage Access", "Открыть полный доступ к хранилищу", "Tam yaddaş girişini aç", "Tam Depolama Erişimini aç"),
    Translation("Photo Analyzer needs access to the media you choose to review. Full Storage Mode avoids a redundant media permission.", "Анализу фото нужен доступ к выбранным вами медиафайлам. Полный режим хранилища позволяет не запрашивать лишнее разрешение на медиа.", "Foto analizatoruna yoxlamaq istədiyiniz mediaya giriş lazımdır. Tam yaddaş rejimi əlavə media icazəsi tələb etmir.", "Fotoğraf analizörü, incelemeyi seçtiğiniz medyaya erişim ister. Tam Depolama Modu gereksiz bir medya izni istemez."),
    Translation("Preview truncated at 64 KB.", "Предпросмотр ограничен 64 КБ.", "Önizləmə 64 KB ilə məhdudlaşdırılıb.", "Önizleme 64 KB ile sınırlandırıldı."),
    Translation("Real device facts from local Android APIs. No health score or background scan.", "Факты об устройстве из локальных API Android. Без оценки здоровья и фонового сканирования.", "Yerli Android API-lərindən real cihaz məlumatları. Sağlamlıq balı və fon skanı yoxdur.", "Yerel Android API'lerinden gerçek cihaz bilgileri. Sağlık puanı veya arka plan taraması yok."),
    Translation("Run analysis again", "Запустить анализ снова", "Analizi yenidən başlat", "Analizi yeniden çalıştır"),
    Translation("Run hardware tests", "Запустить аппаратные тесты", "Aparat testlərini başlat", "Donanım testlerini çalıştır"),
    Translation("SD card, USB OTG and installed cloud providers through Android", "SD-карта, USB OTG и установленные облачные провайдеры через Android", "Android vasitəsilə SD kart, USB OTG və quraşdırılmış bulud xidmətləri", "Android üzerinden SD kart, USB OTG ve yüklü bulut sağlayıcıları"),
    Translation("Sensor is active only while this detail is visible.", "Датчик работает только пока отображаются эти сведения.", "Sensor yalnız bu məlumat görünən müddətdə aktivdir.", "Sensör yalnızca bu ayrıntı görünürken etkindir."),
    Translation("Sequence complete. Did all colors look correct?", "Последовательность завершена. Все цвета выглядели правильно?", "Ardıcıllıq tamamlandı. Bütün rənglər düzgün göründü?", "Dizi tamamlandı. Tüm renkler doğru göründü mü?"),
    Translation("Tap the color field to show the next display color. Check for uniform color and visible defects.", "Нажимайте на цветовое поле для показа следующего цвета экрана. Проверьте равномерность и видимые дефекты.", "Növbəti ekran rəngini göstərmək üçün rəng sahəsinə toxunun. Bərabər rəng və görünən qüsurları yoxlayın.", "Sonraki ekran rengini göstermek için renk alanına dokunun. Rengin düzgünlüğünü ve görünür kusurları kontrol edin."),
    Translation("Tooliva only shows files matching the two explainable rules.", "Tooliva показывает только файлы, соответствующие двум понятным правилам.", "Tooliva yalnız izah edilə bilən iki qaydaya uyğun faylları göstərir.", "Tooliva yalnızca açıklanabilir iki kurala uyan dosyaları gösterir."),
    Translation("Tooliva uses the microphone only during this user-started test to show a live input level. Audio is not saved, transcribed or uploaded.", "Tooliva использует микрофон только во время запущенного вами теста для показа уровня сигнала. Аудио не сохраняется, не расшифровывается и не загружается.", "Tooliva mikrofonu yalnız sizin başlatdığınız test zamanı canlı giriş səviyyəsini göstərmək üçün istifadə edir. Audio saxlanılmır, mətnə çevrilmir və yüklənmir.", "Tooliva mikrofonu yalnızca sizin başlattığınız test sırasında canlı giriş seviyesini göstermek için kullanır. Ses kaydedilmez, yazıya dökülmez veya yüklenmez."),
    Translation("Unselect anything you want to keep. The selected files below are the only files that can be deleted.", "Снимите выбор с того, что хотите оставить. Удалены могут быть только выбранные ниже файлы.", "Saxlamaq istədiyiniz hər şeyin seçimini götürün. Yalnız aşağıda seçilmiş fayllar silinə bilər.", "Tutmak istediklerinizin seçimini kaldırın. Yalnızca aşağıda seçilen dosyalar silinebilir."),
    Translation("Old Files is limited to shared-storage paths that Tooliva can genuinely inspect.", "Раздел «Старые файлы» ограничен путями общего хранилища, которые Tooliva действительно может проверить.", "«Köhnə fayllar» bölməsi Tooliva-nın həqiqətən yoxlaya bildiyi paylaşılan yaddaş yolları ilə məhdudlaşır.", "Eski Dosyalar, Tooliva'nın gerçekten inceleyebildiği paylaşılan depolama yollarıyla sınırlıdır."),
    Translation("Cleanup Swipe reviews local shared-storage files. Android access must be enabled before a category is loaded.", "«Очистка свайпом» проверяет локальные файлы общего хранилища. Перед загрузкой категории нужно включить доступ Android.", "«Sürüşdürərək təmizləmə» paylaşılan yaddaşdakı lokal faylları yoxlayır. Kateqoriya yüklənməzdən əvvəl Android girişi açılmalıdır.", "Kaydırarak temizleme, yerel paylaşılan depolama dosyalarını inceler. Bir kategori yüklenmeden önce Android erişimi etkinleştirilmelidir."),
    Translation("Far → Near → Far sequence detected", "Обнаружена последовательность «далеко → близко → далеко»", "«Uzaq → yaxın → uzaq» ardıcıllığı aşkarlandı", "Uzak → Yakın → Uzak dizisi algılandı"),
    Translation("Location:", "Расположение:", "Məkan:", "Konum:"),
    Translation("MIME", "MIME", "MIME", "MIME"),
    Translation("Notification", "Уведомление", "Bildiriş", "Bildirim"),
    Translation("Pinned", "Закреплено", "Bərkidilib", "Sabitlendi"),
    Translation("Tooliva's core utilities process accessible data locally on this device. Notification History stays local and is controlled by Android Notification Access. Tooliva does not upload user files, notification text or vault content.", "Основные инструменты Tooliva обрабатывают доступные данные локально на этом устройстве. История уведомлений остаётся локальной и управляется доступом Android к уведомлениям. Tooliva не загружает пользовательские файлы, тексты уведомлений или содержимое хранилища.", "Tooliva-nin əsas alətləri əlçatan məlumatları bu cihazda lokal emal edir. Bildiriş tarixçəsi lokal qalır və Android Bildiriş girişi ilə idarə olunur. Tooliva istifadəçi fayllarını, bildiriş mətnini və seyf məzmununu yükləmir.", "Tooliva'nın temel araçları erişilebilir verileri bu cihazda yerel olarak işler. Bildirim Geçmişi yerel kalır ve Android Bildirim Erişimi tarafından denetlenir. Tooliva kullanıcı dosyalarını, bildirim metnini veya kasa içeriğini yüklemez."),
    Translation("used", "занято", "istifadə olunur", "kullanılıyor"),
    Translation("zip", "zip", "zip", "zip"),
    Translation("no longer in the active media list", "больше нет в активном списке медиафайлов", "artıq aktiv media siyahısında deyil", "artık etkin medya listesinde değil"),
    Translation("Clear search", "Очистить поиск", "Axtarışı təmizlə", "Aramayı temizle"),
    Translation("Sort", "Сортировка", "Sıralama", "Sıralama"),
    Translation("Sort groups", "Сортировать группы", "Qrupları sırala", "Grupları sırala"),
    Translation("Show in Files", "Показать в Файлах", "Fayllarda göstər", "Dosyalarda göster"),
    Translation("Create folder", "Создать папку", "Qovluq yarat", "Klasör oluştur"),
    Translation("Change view", "Изменить вид", "Görünüşü dəyiş", "Görünümü değiştir"),
    Translation("Create ZIP", "Создать ZIP", "ZIP yarat", "ZIP oluştur"),
    Translation("More", "Ещё", "Daha çox", "Daha fazla"),
    Translation("Open app settings", "Открыть настройки приложения", "Tətbiq ayarlarını aç", "Uygulama ayarlarını aç"),
    Translation("Preview unavailable", "Предпросмотр недоступен", "Önizləmə mövcud deyil", "Önizleme kullanılamıyor"),
    Translation("Undo", "Отменить", "Geri al", "Geri al"),
    Translation("Open in Files", "Открыть в Файлах", "Fayllarda aç", "Dosyalarda aç"),
)

internal fun localizeToolivaText(value: String, language: ToolivaLanguage): String {
    if (language == ToolivaLanguage.ENGLISH) return value
    translations.firstOrNull { it.source == value }?.let { translation ->
        return when (language) {
            ToolivaLanguage.RUSSIAN -> translation.russian
            ToolivaLanguage.AZERBAIJANI -> translation.azerbaijani
            ToolivaLanguage.TURKISH -> translation.turkish
            ToolivaLanguage.ENGLISH -> value
        }
    }
    return localizeTemplate(value, language)
}

private fun localizeTemplate(value: String, language: ToolivaLanguage): String {
    fun labeled(prefix: String, russian: String, azerbaijani: String, turkish: String): String? {
        if (!value.startsWith(prefix)) return null
        val suffix = value.removePrefix(prefix)
        return when (language) {
            ToolivaLanguage.RUSSIAN -> russian + suffix
            ToolivaLanguage.AZERBAIJANI -> azerbaijani + suffix
            ToolivaLanguage.TURKISH -> turkish + suffix
            ToolivaLanguage.ENGLISH -> value
        }
    }
    labeled("Open ", "Открыть ", "Aç: ", "Aç: ")?.let { return it }
    labeled("Share ", "Поделиться: ", "Paylaş: ", "Paylaş: ")?.let { return it }
    labeled("Details for ", "Сведения: ", "Məlumat: ", "Ayrıntılar: ")?.let { return it }
    labeled("Show ", "Показать ", "Göstər: ", "Göster: ")?.let { return it }
    Regex("^(\\d+) files · (.+)$").matchEntire(value)?.let { match ->
        val count = match.groupValues[1]
        val details = match.groupValues[2]
        return when (language) {
            ToolivaLanguage.RUSSIAN -> "$count файлов · $details"
            ToolivaLanguage.AZERBAIJANI -> "$count fayl · $details"
            ToolivaLanguage.TURKISH -> "$count dosya · $details"
            ToolivaLanguage.ENGLISH -> value
        }
    }
    Regex("^(\\d+) items · (.+)$").matchEntire(value)?.let { match ->
        val count = match.groupValues[1]
        val details = match.groupValues[2]
        return when (language) {
            ToolivaLanguage.RUSSIAN -> "$count объектов · $details"
            ToolivaLanguage.AZERBAIJANI -> "$count obyekt · $details"
            ToolivaLanguage.TURKISH -> "$count öğe · $details"
            ToolivaLanguage.ENGLISH -> value
        }
    }
    Regex("^Delete (\\d+) · (.+)$").matchEntire(value)?.let { match ->
        val count = match.groupValues[1]
        val details = match.groupValues[2]
        return when (language) {
            ToolivaLanguage.RUSSIAN -> "Удалить $count · $details"
            ToolivaLanguage.AZERBAIJANI -> "$count · sil"
            ToolivaLanguage.TURKISH -> "$count · sil"
            ToolivaLanguage.ENGLISH -> value
        }
    }
    Regex("^(\\d+) apps in this view$").matchEntire(value)?.let { match ->
        val count = match.groupValues[1]
        return when (language) {
            ToolivaLanguage.RUSSIAN -> "$count приложений в этом представлении"
            ToolivaLanguage.AZERBAIJANI -> "Bu görünüşdə $count tətbiq"
            ToolivaLanguage.TURKISH -> "Bu görünümde $count uygulama"
            ToolivaLanguage.ENGLISH -> value
        }
    }
    Regex("^(\\d+) of (\\d+) supported tests completed$").matchEntire(value)?.let { match ->
        val completed = match.groupValues[1]
        val total = match.groupValues[2]
        return when (language) {
            ToolivaLanguage.RUSSIAN -> "$completed из $total поддерживаемых тестов завершено"
            ToolivaLanguage.AZERBAIJANI -> "$total testdən $completed tamamlandı"
            ToolivaLanguage.TURKISH -> "$total desteklenen testten $completed tamamlandı"
            ToolivaLanguage.ENGLISH -> value
        }
    }
    val countPattern = Regex("^(\\d+) (files|files checked|files selected|selected|shown|matching files|visible to Tooliva|screenshots|notifications|groups|candidates|items|item\\(s\\))$")
    countPattern.matchEntire(value)?.let { match ->
        val count = match.groupValues[1]
        val noun = match.groupValues[2]
        return when (language) {
            ToolivaLanguage.RUSSIAN -> when (noun) {
                "files checked" -> "$count файлов проверено"
                "files selected" -> "$count файлов выбрано"
                "selected" -> "$count выбрано"
                "shown" -> "$count показано"
                "matching files" -> "$count подходящих файлов"
                "visible to Tooliva" -> "$count доступно Tooliva"
                "screenshots" -> "$count скриншотов"
                "notifications" -> "$count уведомлений"
                "groups" -> "$count групп"
                "candidates" -> "$count кандидатов"
                "items" -> "$count объектов"
                "item(s)" -> "$count объектов"
                else -> "$count файлов"
            }
            ToolivaLanguage.AZERBAIJANI -> when (noun) {
                "files checked" -> "$count fayl yoxlanıldı"
                "files selected" -> "$count fayl seçildi"
                "selected" -> "$count seçildi"
                "shown" -> "$count göstərilir"
                "matching files" -> "$count uyğun fayl"
                "visible to Tooliva" -> "$count fayl Tooliva üçün əlçatandır"
                "screenshots" -> "$count ekran görüntüsü"
                "notifications" -> "$count bildiriş"
                "groups" -> "$count qrup"
                "candidates" -> "$count namizəd"
                "items" -> "$count obyekt"
                "item(s)" -> "$count obyekt"
                else -> "$count fayl"
            }
            ToolivaLanguage.TURKISH -> when (noun) {
                "files checked" -> "$count dosya kontrol edildi"
                "files selected" -> "$count dosya seçildi"
                "selected" -> "$count seçildi"
                "shown" -> "$count gösteriliyor"
                "matching files" -> "$count eşleşen dosya"
                "visible to Tooliva" -> "$count dosya Tooliva tarafından görülebiliyor"
                "screenshots" -> "$count ekran görüntüsü"
                "notifications" -> "$count bildirim"
                "groups" -> "$count grup"
                "candidates" -> "$count aday"
                "items" -> "$count öğe"
                "item(s)" -> "$count öğe"
                else -> "$count dosya"
            }
            ToolivaLanguage.ENGLISH -> value
        }
    }
    Regex("^(\\d+) days$").matchEntire(value)?.let { match ->
        val count = match.groupValues[1]
        return when (language) {
            ToolivaLanguage.RUSSIAN -> "$count дн."
            ToolivaLanguage.AZERBAIJANI -> "$count gün"
            ToolivaLanguage.TURKISH -> "$count gün"
            ToolivaLanguage.ENGLISH -> value
        }
    }
    if (value.startsWith("Sort: ")) {
        val prefix = when (language) {
            ToolivaLanguage.RUSSIAN -> "Сортировка: "
            ToolivaLanguage.AZERBAIJANI -> "Sıralama: "
            ToolivaLanguage.TURKISH -> "Sıralama: "
            ToolivaLanguage.ENGLISH -> "Sort: "
        }
        return prefix + localizeToolivaText(value.removePrefix("Sort: "), language)
    }
    return value
}
