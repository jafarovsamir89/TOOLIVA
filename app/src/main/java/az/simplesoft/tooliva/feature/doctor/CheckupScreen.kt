@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package az.simplesoft.tooliva.feature.doctor

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.device.BatterySnapshot
import az.simplesoft.tooliva.core.device.StorageVolumeSnapshot
import az.simplesoft.tooliva.feature.clean.CleanerBucket
import az.simplesoft.tooliva.feature.clean.CleanerAnalysisSnapshot
import az.simplesoft.tooliva.ui.CheckupStrings
import az.simplesoft.tooliva.ui.LocalToolivaStrings
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import az.simplesoft.tooliva.core.settings.ToolivaLanguage

private data class CheckupAction(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val bucket: CleanerBucket? = null,
)

@Composable
private fun checkupActions(copy: CheckupStrings): List<CheckupAction> = listOf(
    CheckupAction("large-files", "Large files", "Review the biggest accessible files", Icons.Outlined.Storage, CleanerBucket.LARGE_FILES),
    CheckupAction("downloads", "Downloads", "Review installers, archives and documents", Icons.Outlined.Download, CleanerBucket.DOWNLOADS),
    CheckupAction("duplicates", "Exact duplicates", "Analyze identical files when you choose", Icons.Outlined.PhotoLibrary),
    CheckupAction("screenshots", "Screenshots", "Review screenshots by age", Icons.Outlined.Screenshot, CleanerBucket.SCREENSHOTS),
    CheckupAction("photo-analyzer", "Photo Analyzer", "Review similar, blurry and large media on-device", Icons.Outlined.Image),
    CheckupAction("cache", "Cache Cleaner", "Review app cache measurements", Icons.Outlined.CleaningServices),
    CheckupAction("optimizer", "Phone Optimizer", "Open real memory and system cache tools", Icons.Outlined.Memory),
).map { action ->
    action.copy(
        title = copy.actionTitles[action.id] ?: action.title,
        subtitle = copy.actionSubtitles[action.id] ?: action.subtitle,
    )
}

@Composable
fun CheckupRoute(
    onBack: () -> Unit,
    onOpenAction: (String) -> Unit,
    viewModel: CheckupViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val copy = LocalToolivaStrings.current.checkup
    BackHandler(onBack = onBack)
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(copy.checkMyPhone, fontWeight = FontWeight.Black) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, copy.checkMyPhone) } },
        )
    }) { padding ->
        if (state.result == null) {
            CheckupStartScreen(copy, padding, state.running, state.error, viewModel::runCheckup)
        } else {
            CheckupResultScreen(state.result!!, copy, padding, onOpenAction, viewModel::runCheckup)
        }
    }
}

@Composable
private fun CheckupStartScreen(copy: CheckupStrings, contentPadding: PaddingValues, running: Boolean, hasError: Boolean, onRun: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(Icons.Outlined.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 12.dp).size(48.dp))
        Text(copy.reviewYourPhone, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(copy.intro, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (hasError) {
            Card(shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(copy.noErrorDetails, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
            }
        }
        Spacer(Modifier.weight(1f))
        Button(onClick = onRun, enabled = !running, modifier = Modifier.fillMaxWidth()) {
            if (running) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            Text(if (running) copy.checking else if (hasError) copy.retry else copy.runCheckup, Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun CheckupResultScreen(result: CheckupResult, copy: CheckupStrings, contentPadding: PaddingValues, onOpenAction: (String) -> Unit, onRunAgain: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val actions = checkupActions(copy)
    LazyColumn(
        Modifier.fillMaxSize().padding(contentPadding).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(copy.checkupComplete, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("${copy.lastChecked}: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(result.checkedAtMillis))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onRunAgain) { Text(copy.retry) }
            }
        }
        item {
            Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (result.attentionItems.isNotEmpty()) copy.needsAttention else copy.noAttention, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    result.attentionItems.forEach { Text("• ${localizedAttention(it, copy)}") }
                }
            }
        }
        item { SummaryCard(result, copy) { onOpenAction("hardware-tests") } }
        item { DeviceAndSystemCard(result, copy) }
        item { BatteryCard(result.snapshot.battery, copy) }
        item { HardwareFactsCard(result, copy) }
        item { Text(copy.storagePlan, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        item { StoragePlanCard(result.cleanerSnapshot, copy, context) }
        item { Text(copy.cleanupTools, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(actions, key = { it.id }) { action ->
            val summary = action.bucket?.let { bucket -> result.cleanerSnapshot?.summaries?.firstOrNull { it.bucket == bucket } }
            val subtitle = summary?.let { copy.reviewableFormat.format(it.count, Formatter.formatFileSize(context, it.bytes)) } ?: action.subtitle
            Card(onClick = { onOpenAction(action.id) }, shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(action.icon, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) { Text(action.title, fontWeight = FontWeight.Bold); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item { Text(copy.noAutomaticScan, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SummaryCard(result: CheckupResult, copy: CheckupStrings, onOpenHardware: () -> Unit) {
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(copy.hardwareTests, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(copy.hardwareTestsCompleted.format(result.hardware.completed, result.hardware.supported))
            if (result.hardware.failed > 0) Text(copy.problemReported.format(result.hardware.failed), color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = onOpenHardware, modifier = Modifier.fillMaxWidth()) { Text(copy.viewHardwareTests) }
        }
    }
}

@Composable
private fun DeviceAndSystemCard(result: CheckupResult, copy: CheckupStrings) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot = result.snapshot
    FactsCard(copy.deviceAndSystem) {
        Fact(copy.device, "${snapshot.device.manufacturer} ${snapshot.device.model}")
        Fact(copy.android, "${snapshot.system.androidVersion} · SDK ${snapshot.system.sdkLevel}")
        Fact(copy.securityPatch, snapshot.system.securityPatch)
        Fact(copy.supportedAbis, snapshot.system.supportedAbis.joinToString().ifBlank { copy.unavailable })
        Fact(copy.memory, snapshot.memory?.let { "${Formatter.formatFileSize(context, it.availableBytes)} ${copy.available} · ${localizedValue(it.pressureLabel, copy)}" } ?: copy.unavailable)
        snapshot.storage.forEach { volume -> StorageFact(volume, copy, context) }
    }
}

@Composable
private fun StorageFact(volume: StorageVolumeSnapshot, copy: CheckupStrings, context: android.content.Context) {
    Fact(volume.label, "${Formatter.formatFileSize(context, volume.availableBytes)} ${copy.available} / ${Formatter.formatFileSize(context, volume.totalBytes)}")
}

@Composable
private fun BatteryCard(battery: BatterySnapshot, copy: CheckupStrings) {
    FactsCard(copy.battery) {
        Fact(copy.battery, battery.levelPercent?.let { "$it% · ${localizedValue(battery.status, copy)}" } ?: copy.unavailable)
        Fact(copy.powerSource, localizedValue(battery.powerSource, copy))
        Fact(copy.batteryHealth, battery.health?.let { localizedValue(it, copy) } ?: copy.unavailable)
        Fact(copy.temperature, battery.temperatureCelsius?.let { "%.1f °C".format(Locale.US, it) } ?: copy.unavailable)
        Fact(copy.voltage, battery.voltageVolts?.let { "%.3f V".format(Locale.US, it) } ?: copy.unavailable)
        Fact(copy.current, battery.currentNowMilliamps?.let { "%.0f mA".format(Locale.US, it) } ?: copy.unavailable)
    }
}

@Composable
private fun HardwareFactsCard(result: CheckupResult, copy: CheckupStrings) {
    val display = result.snapshot.display
    FactsCard(copy.display) {
        Fact(copy.display, listOfNotNull(display.widthPixels, display.heightPixels).takeIf { it.size == 2 }?.let { "${it[0]} × ${it[1]} px" } ?: copy.unavailable)
        Fact(copy.refreshRate, display.refreshRateHz?.let { "%.1f Hz".format(Locale.US, it) } ?: copy.unavailable)
        Fact(copy.sensors, "${result.snapshot.sensors.size} ${copy.available}")
        Fact(copy.thermal, localizedValue(result.snapshot.thermal.label, copy))
    }
}

@Composable
private fun StoragePlanCard(snapshot: CleanerAnalysisSnapshot?, copy: CheckupStrings, context: android.content.Context) {
    Card(shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (snapshot == null) {
                Text(copy.noStorageScan, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(copy.scanSummaryFormat.format(snapshot.summaries.size, snapshot.filesChecked), fontWeight = FontWeight.SemiBold)
                Text(copy.checkedBytesFormat.format(Formatter.formatFileSize(context, snapshot.bytesChecked)), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun FactsCard(title: String, content: @Composable () -> Unit) {
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); content() }
    }
}

@Composable
private fun Fact(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
        Text(label, Modifier.weight(0.38f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, Modifier.weight(0.62f), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

private fun localizedValue(value: String, copy: CheckupStrings): String = when (copy.language) {
    ToolivaLanguage.RUSSIAN -> when (value) {
        "Normal" -> "Норма"
        "High" -> "Высокая нагрузка"
        "Charging" -> "Заряжается"
        "Discharging" -> "Разряжается"
        "Full" -> "Полностью заряжена"
        "Not charging" -> "Не заряжается"
        "Wireless" -> "Беспроводная зарядка"
        "Good" -> "Хорошее"
        "Overheat" -> "Перегрев"
        "Dead" -> "Неисправна"
        "Over voltage" -> "Перенапряжение"
        "Unspecified failure" -> "Неуказанная ошибка"
        else -> value
    }
    ToolivaLanguage.AZERBAIJANI -> when (value) {
        "Normal" -> "Normal"
        "High" -> "Yüksək yük"
        "Charging" -> "Doldurulur"
        "Discharging" -> "Boşalır"
        "Full" -> "Tam dolu"
        "Not charging" -> "Doldurulmur"
        "Wireless" -> "Simsiz"
        "Good" -> "Yaxşı"
        "Overheat" -> "Həddindən artıq istilik"
        else -> value
    }
    ToolivaLanguage.TURKISH -> when (value) {
        "Normal" -> "Normal"
        "High" -> "Yüksek"
        "Charging" -> "Şarj oluyor"
        "Discharging" -> "Şarjı azalıyor"
        "Full" -> "Dolu"
        "Not charging" -> "Şarj olmuyor"
        "Wireless" -> "Kablosuz"
        "Good" -> "İyi"
        "Overheat" -> "Aşırı ısınma"
        else -> value
    }
    ToolivaLanguage.ENGLISH -> value
}

private fun localizedAttention(value: String, copy: CheckupStrings): String = when (copy.language) {
    ToolivaLanguage.RUSSIAN -> value
        .replace("Memory pressure is high", "Высокая нагрузка на память")
        .replace("Thermal status: ", "Температура системы: ")
        .replace("Android reports a battery health warning", "Android сообщает о проблеме с батареей")
        .replace("hardware tests reported a problem", "аппаратных теста сообщили о проблеме")
        .replace("hardware test reported a problem", "аппаратный тест сообщил о проблеме")
    ToolivaLanguage.AZERBAIJANI -> value
        .replace("Memory pressure is high", "Yaddaş yükü yüksəkdir")
        .replace("Thermal status: ", "Temperatur vəziyyəti: ")
        .replace("Android reports a battery health warning", "Android batareya xəbərdarlığı bildirir")
    ToolivaLanguage.TURKISH -> value
        .replace("Memory pressure is high", "Bellek kullanımı yüksek")
        .replace("Thermal status: ", "Sıcaklık durumu: ")
        .replace("Android reports a battery health warning", "Android pil sağlığı uyarısı bildiriyor")
    ToolivaLanguage.ENGLISH -> value
}
