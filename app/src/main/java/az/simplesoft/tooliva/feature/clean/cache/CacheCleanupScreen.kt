package az.simplesoft.tooliva.feature.clean.cache

import android.text.format.Formatter
import android.widget.ImageView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import az.simplesoft.tooliva.ui.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import az.simplesoft.tooliva.ui.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.cache.CacheAppCategory
import az.simplesoft.tooliva.core.cache.CacheAppEntry
import az.simplesoft.tooliva.core.cache.CacheMeasurementState

@Composable
fun CacheCleanupRoute(viewModel: CacheCleanerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var actionError by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccessState() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Cache Cleaner", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Review and clear app caches", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (!state.usageAccessGranted) {
            item {
                Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Measure app cache sizes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Allow Usage Access so Android can provide cache statistics for the selected browsers and YouTube. This is not a runtime permission and no cache is changed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = {
                            runCatching { context.startActivity(viewModel.usageAccessIntent()) }
                                .onFailure { actionError = "Android did not provide Usage Access settings." }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Open Usage Access settings") }
                    }
                }
                Text(
                    "Usage Access is used only to read Android-provided cache statistics for the apps shown here. Tooliva does not read browser pages, history, passwords or account data.",
                    modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        item { SummaryCard(state) }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = viewModel::analyze,
                    enabled = state.usageAccessGranted && !state.isLoading,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.isLoading) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Text(if (state.isLoading) "Analyzing…" else "Analyze cache", modifier = Modifier.padding(start = 8.dp))
                }
                if (state.entries.isNotEmpty()) OutlinedButton(onClick = viewModel::selectAll, enabled = !state.isLoading) { Text("Select all") }
            }
        }

        state.errorMessage?.let { message -> item { ErrorCard(message) } }

        if (state.entries.isEmpty() && !state.isLoading) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Text(
                        if (state.usageAccessGranted) "Tap Analyze cache to measure installed browsers and YouTube. Nothing is selected or cleaned automatically." else "Grant Usage Access, return here, then tap Analyze cache.",
                        modifier = Modifier.padding(20.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        CacheAppSection("Browsers", state.entries.filter { it.category == CacheAppCategory.BROWSER }, viewModel, context) { actionError = it }
        CacheAppSection("Video", state.entries.filter { it.category == CacheAppCategory.VIDEO }, viewModel, context) { actionError = it }

        if (state.entries.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("What will be cleaned?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Only the selected app cache through Android's App Info storage controls. Cookies, passwords, history, downloads, accounts, settings and app data are not selected.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Select apps here for review, then open their Android settings and press Clear cache yourself. Tooliva never changes app data or storage.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        val first = state.entries.firstOrNull { it.packageName in state.selectedPackages }
                        if (first != null) {
                            runCatching { context.startActivity(viewModel.manualSettingsIntent(first.packageName)) }
                                .onFailure { actionError = "Android could not open settings for ${first.appLabel}." }
                        }
                    },
                    enabled = state.selectedBytes > 0L,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Outlined.Cached, contentDescription = null)
                    Text("Clean selected · ${formatBytes(context, state.selectedBytes)}", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        item { Spacer(Modifier.height(8.dp)) }
    }

}

@Composable
private fun SummaryCard(state: CacheCleanerUiState) {
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Outlined.Cached, contentDescription = null, modifier = Modifier.size(42.dp), tint = MaterialTheme.colorScheme.primary)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("App caches", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatBytes(context, state.measuredTotalBytes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text("${state.selectedPackages.size} selected · ${formatBytes(context, state.selectedBytes)}", color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

private fun LazyListScope.CacheAppSection(
    title: String,
    entries: List<CacheAppEntry>,
    viewModel: CacheCleanerViewModel,
    context: android.content.Context,
    onError: (String) -> Unit,
) {
    if (entries.isEmpty()) return
    item(key = "$title-header") { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp)) }
    items(entries, key = { it.packageName }) { entry -> CacheAppRow(entry, viewModel, context, onError) }
}

@Composable
private fun CacheAppRow(entry: CacheAppEntry, viewModel: CacheCleanerViewModel, context: android.content.Context, onError: (String) -> Unit) {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            AppIcon(entry.packageName, modifier = Modifier.size(44.dp))
            Column(modifier = Modifier.weight(1f).padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(entry.appLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(cacheLabel(context, entry), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Checkbox(checked = entry.selected, onCheckedChange = { viewModel.toggleSelection(entry.packageName) }, enabled = entry.measurementState != CacheMeasurementState.UNAVAILABLE)
            IconButton(onClick = {
                runCatching { context.startActivity(viewModel.manualSettingsIntent(entry.packageName)) }
                    .onFailure { onError("Android could not open settings for ${entry.appLabel}.") }
            }) { Icon(Icons.Outlined.OpenInNew, contentDescription = "Open app settings") }
        }
    }
}

@Composable
private fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val drawable = remember(packageName) { runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull() }
    AndroidView(modifier = modifier, factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } }, update = { it.setImageDrawable(drawable) })
}

private fun cacheLabel(context: android.content.Context, entry: CacheAppEntry): String = when (entry.measurementState) {
    CacheMeasurementState.MEASURED -> formatBytes(context, entry.cacheBytes ?: 0L) + " cache"
    CacheMeasurementState.ZERO -> "0 B cache"
    CacheMeasurementState.UNAVAILABLE -> "Cache size unavailable"
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(message, modifier = Modifier.padding(16.dp)) }
}

private fun formatBytes(context: android.content.Context, bytes: Long): String = Formatter.formatFileSize(context, bytes.coerceAtLeast(0L))
