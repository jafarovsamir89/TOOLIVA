@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package az.simplesoft.tooliva.feature.doctor

import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

private data class CheckupAction(val id: String, val title: String, val subtitle: String)

private val checkupActions = listOf(
    CheckupAction("large-files", "Large Files", "Review the biggest accessible files"),
    CheckupAction("downloads", "Downloads", "Review installers, archives and documents"),
    CheckupAction("duplicates", "Exact Duplicates", "Analyze identical files when you choose"),
    CheckupAction("screenshots", "Screenshots", "Review screenshots by age"),
    CheckupAction("cache", "Cache Cleaner", "Review app cache measurements"),
    CheckupAction("optimizer", "Phone Optimizer", "Open real memory and system cache tools"),
)

@Composable
fun CheckupRoute(
    onBack: () -> Unit,
    onOpenAction: (String) -> Unit,
    viewModel: CheckupViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    Scaffold(topBar = {
        TopAppBar(title = { Text("Check My Phone", fontWeight = FontWeight.Black) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } })
    }) { padding ->
        if (state.result == null) {
            Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Icon(Icons.Outlined.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 24.dp))
                Text("Review your phone", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("This is a quick local check of device facts, memory, storage, battery, thermal status and known hardware test results. It does not run expensive storage analysis.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.weight(1f))
                Button(onClick = viewModel::runCheckup, enabled = !state.running, modifier = Modifier.fillMaxWidth()) { Text(if (state.running) "Checking…" else "Run checkup") }
            }
        } else {
            CheckupResultScreen(state.result!!, onOpenAction)
        }
    }
}

@Composable
private fun CheckupResultScreen(result: CheckupResult, onOpenAction: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Checkup complete", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        if (result.attentionItems.isNotEmpty()) item {
            Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Needs attention", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); result.attentionItems.forEach { Text("• $it") } }
            }
        }
        item { SummaryCard(result) }
        item { CheckupFacts(result) }
        item { Text("Cleanup tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
        items(checkupActions, key = { it.id }) { action ->
            Card(onClick = { onOpenAction(action.id) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(actionIcon(action.id), null, tint = MaterialTheme.colorScheme.primary); Column { Text(action.title, fontWeight = FontWeight.Bold); Text(action.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            }
        }
        item { Text("Exact Duplicates and other expensive analyses remain user-started. No automatic scan was launched.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@Composable
private fun SummaryCard(result: CheckupResult) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Hardware tests", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("${result.hardware.completed} of ${result.hardware.supported} supported tests completed"); if (result.hardware.failed > 0) Text("${result.hardware.failed} problem reported", color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
private fun CheckupFacts(result: CheckupResult) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val snapshot = result.snapshot
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Device and system", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Fact("Device", snapshot.device.model); Fact("Android", "${snapshot.system.androidVersion} · SDK ${snapshot.system.sdkLevel}"); Fact("Security patch", snapshot.system.securityPatch)
            Fact("Memory", snapshot.memory?.let { "${Formatter.formatFileSize(context, it.availableBytes)} available · ${it.pressureLabel}" } ?: "Unavailable")
            Fact("Storage", snapshot.storage.firstOrNull()?.let { "${Formatter.formatFileSize(context, it.availableBytes)} available" } ?: "Unavailable")
            Fact("Battery", snapshot.battery.levelPercent?.let { "$it% · ${snapshot.battery.status} · ${snapshot.battery.temperatureCelsius?.let { t -> "%.1f °C".format(java.util.Locale.US, t) } ?: "temperature unavailable"}" } ?: "Unavailable")
            Fact("Thermal", snapshot.thermal.label)
        }
    }
}

@Composable private fun Fact(label: String, value: String) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.SemiBold) } }

private fun actionIcon(id: String) = when (id) {
    "large-files" -> Icons.Outlined.Storage
    "downloads" -> Icons.Outlined.Download
    "duplicates" -> Icons.Outlined.PhotoLibrary
    "screenshots" -> Icons.Outlined.Screenshot
    "cache" -> Icons.Outlined.CleaningServices
    else -> Icons.Outlined.Memory
}
