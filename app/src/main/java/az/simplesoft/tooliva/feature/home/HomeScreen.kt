package az.simplesoft.tooliva.feature.home

import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import az.simplesoft.tooliva.ui.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import az.simplesoft.tooliva.ui.LocalizedText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.ui.theme.ToolivaIconTile
import az.simplesoft.tooliva.ui.theme.ToolivaSectionHeader
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import az.simplesoft.tooliva.ui.theme.ToolivaSpacing
import az.simplesoft.tooliva.ui.theme.ToolivaToolTile
import az.simplesoft.tooliva.ui.LocalToolivaStrings
import java.text.DateFormat
import java.util.Date

data class HomeTool(val id: String, val title: String, val subtitle: String, val icon: ImageVector)

private val primaryTools = listOf(
    HomeTool("clean", "Clean", "Review storage", Icons.Outlined.CleaningServices),
    HomeTool("files", "Files", "Browse storage", Icons.Outlined.Folder),
    HomeTool("duplicates", "Duplicates", "Keep one copy", Icons.Outlined.CleaningServices),
    HomeTool("photo-analyzer", "Photo Analyzer", "Review media safely", Icons.Outlined.PhotoLibrary),
    HomeTool("storage-map", "Storage Map", "Folder usage", Icons.Outlined.Storage),
    HomeTool("doctor", "Phone Doctor", "Device facts", Icons.Outlined.HealthAndSafety),
    HomeTool("hardware", "Hardware Tests", "Check components", Icons.Outlined.Build),
    HomeTool("optimizer", "Optimizer", "Memory metrics", Icons.Outlined.Memory),
    HomeTool("app-manager", "App Manager", "Review apps", Icons.Outlined.Apps),
    HomeTool("notifications", "Notifications", "Local history", Icons.Outlined.Notifications),
)

@Composable
fun HomeRoute(
    onCheckup: () -> Unit,
    onOpenTool: (String) -> Unit,
    onSettings: () -> Unit = {},
    onOpenHistory: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state, onCheckup, onOpenTool, onSettings, onOpenHistory)
}

@Composable
private fun HomeScreen(state: HomeUiState, onCheckup: () -> Unit, onOpenTool: (String) -> Unit, onSettings: () -> Unit, onOpenHistory: () -> Unit) {
    val strings = LocalToolivaStrings.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(ToolivaSpacing.lg),
        horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.md),
        verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.xs)) {
                    Text("TOOLIVA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(strings.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = onSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
            }
        }
        item(span = { GridItemSpan(maxLineSpan) }) { StorageHero(state) }
        item(span = { GridItemSpan(maxLineSpan) }) { CheckupCard(onCheckup) }
        item(span = { GridItemSpan(maxLineSpan) }) { ActionPlanCard(state, onOpenTool, onOpenHistory) }
        item(span = { GridItemSpan(maxLineSpan) }) { ToolivaSectionHeader("Storage & cleanup") }
        items(primaryTools, key = { it.id }) { tool ->
            ToolivaToolTile(tool.title, tool.icon, subtitle = tool.subtitle) { onOpenTool(tool.id) }
        }
    }
}

@Composable
private fun ActionPlanCard(state: HomeUiState, onOpenTool: (String) -> Unit, onOpenHistory: () -> Unit) {
    val strings = LocalToolivaStrings.current
    val context = LocalContext.current
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(ToolivaSpacing.xl), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.sm)) {
                Icon(Icons.Outlined.CleaningServices, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.weight(1f)) {
                    Text(strings.actionPlan, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(strings.reviewRealFindings, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenHistory) { Icon(Icons.Outlined.History, contentDescription = strings.history); Text(strings.history) }
            }
            val plan = state.actionPlan
            if (plan == null || plan.summaries.isEmpty()) {
                Text(strings.runFirstPlan, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                plan.summaries.take(4).forEach { summary ->
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.sm)) {
                        Column(Modifier.weight(1f)) {
                            Text(summary.bucket.title, fontWeight = FontWeight.SemiBold)
                            Text("${summary.count} items · ${Formatter.formatFileSize(context, summary.bytes)} to review", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { onOpenTool(summary.bucket.route) }) { Text(strings.review) }
                    }
                }
            }
            state.scanHistory.firstOrNull()?.let { history ->
                Text(
                    "Last scan ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(history.finishedAtMillis))} · ${Formatter.formatFileSize(context, history.usedBytes)} used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StorageHero(state: HomeUiState) {
    val context = LocalContext.current
    val animatedFraction by animateFloatAsState(state.storageUsedFraction, tween(380), label = "storage")
    val used = Formatter.formatFileSize(context, state.storageUsedBytes)
    val total = Formatter.formatFileSize(context, state.storageTotalBytes)
    val available = Formatter.formatFileSize(context, state.storageAvailableBytes)
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Box(modifier = Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)))).padding(ToolivaSpacing.xxl)) {
            Column(verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.lg)) {
                Text("Storage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.xxl)) {
                    Box(modifier = Modifier.size(116.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(progress = { animatedFraction }, modifier = Modifier.fillMaxSize(), strokeWidth = 11.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) { Text("${(animatedFraction * 100).toInt()}%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("used", style = MaterialTheme.typography.labelMedium) }
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.xs)) {
                        Text(used, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("of $total", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$available available", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    }
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.sm)) {
                    Box(modifier = Modifier.weight(1f)) { HomeMetric(Icons.Outlined.BatteryChargingFull, "Battery", state.batteryPercent?.let { "$it%" } ?: "—") }
                    Box(modifier = Modifier.weight(1f)) { HomeMetric(Icons.Outlined.Thermostat, "Thermal", state.thermalLabel) }
                    Box(modifier = Modifier.weight(1f)) { HomeMetric(Icons.Outlined.Memory, "Memory", state.memoryPressure) }
                }
            }
        }
    }
}

@Composable
private fun HomeMetric(icon: ImageVector, label: String, value: String) {
    Card(shape = ToolivaShapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))) {
        Column(modifier = Modifier.padding(ToolivaSpacing.md), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.xs)) { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)); Text(label, style = MaterialTheme.typography.labelMedium); Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1) }
    }
}

@Composable
private fun CheckupCard(onCheckup: () -> Unit) {
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(modifier = Modifier.padding(ToolivaSpacing.xxl), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) { ToolivaIconTile(Icons.Outlined.HealthAndSafety, Modifier.size(48.dp)); Column { Text("Check My Phone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black); Text("Real device checks, no invented health score", color = MaterialTheme.colorScheme.onPrimaryContainer) } }
            Button(onClick = onCheckup, modifier = Modifier.fillMaxWidth()) { Text("Start checkup", fontWeight = FontWeight.Bold) }
        }
    }
}
