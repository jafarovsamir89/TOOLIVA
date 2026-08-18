package az.simplesoft.tooliva.feature.optimizer

import android.content.ActivityNotFoundException
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.cache.CacheCleanupAvailability
import az.simplesoft.tooliva.core.cache.CacheCleanupResult
import az.simplesoft.tooliva.core.device.MemorySnapshot
import az.simplesoft.tooliva.feature.clean.StorageAccessCard

@Composable
fun PhoneOptimizerRoute(viewModel: PhoneOptimizerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var actionError by remember { mutableStateOf<String?>(null) }
    val systemLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result -> viewModel.onSystemResult(result.resultCode) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }

    if (state.result != null) {
        BackHandler { viewModel.clearResult() }
        OptimizerResultScreen(state = state, onDone = viewModel::clearResult)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Phone Optimizer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Memory and temporary system cache", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item { MemoryCard(state.memory) }
        item { StorageCard(state.storage) }
        if (state.availability == CacheCleanupAvailability.PERMISSION_REQUIRED) {
            item {
                StorageAccessCard(
                    fullMode = false,
                    supported = true,
                    errorMessage = actionError,
                    onEnableFull = {
                        runCatching { context.startActivity(az.simplesoft.tooliva.core.storage.StorageAccessCoordinator(context).allFilesSettingsIntent()) }
                            .onFailure { actionError = "Android did not provide Full Storage Access settings." }
                    },
                )
            }
        }
        item {
            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Temporary app cache cleanup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Android will show its own confirmation. Tooliva does not kill apps or promise a fake RAM boost.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = {
                            if (viewModel.beginOptimize()) {
                                val intent = viewModel.actionIntent()
                                if (intent == null) viewModel.launchFailed("System cache cleanup is unavailable on this device.")
                                else try { systemLauncher.launch(intent) } catch (_: ActivityNotFoundException) { viewModel.launchFailed("System cache cleanup is unavailable on this device.") } catch (_: SecurityException) { viewModel.launchFailed("Android did not allow the system cache cleanup request.") }
                            }
                        },
                        enabled = state.availability == CacheCleanupAvailability.AVAILABLE && !state.awaitingSystemResult,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.awaitingSystemResult) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Outlined.Speed, contentDescription = null)
                        Text(if (state.awaitingSystemResult) "Waiting for Android…" else "Optimize", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
        if (state.availability == CacheCleanupAvailability.UNSUPPORTED) item { Text("System cache cleanup isn't available on this device.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        state.launchError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
    }
}

@Composable
private fun MemoryCard(memory: MemorySnapshot?) {
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Outlined.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary); Text("Memory", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            if (memory == null) Text("Memory information unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else {
                MetricRow("Total RAM", formatBytes(context, memory.totalBytes))
                MetricRow("Available memory", formatBytes(context, memory.availableBytes))
                MetricRow("Used estimate", formatBytes(context, memory.usedEstimateBytes))
                MetricRow("Memory pressure", memory.pressureLabel)
            }
        }
    }
}

@Composable
private fun StorageCard(storage: az.simplesoft.tooliva.core.device.DeviceSnapshot?) {
    val context = LocalContext.current
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(Icons.Outlined.Storage, contentDescription = null); Text("Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Text(storage?.let { "${formatBytes(context, it.storageAvailableBytes)} available of ${formatBytes(context, it.storageTotalBytes)}" } ?: "Storage information unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MetricRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, fontWeight = FontWeight.Bold) }
}

@Composable
private fun OptimizerResultScreen(state: PhoneOptimizerUiState, onDone: () -> Unit) {
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Outlined.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
        Text(if (state.result == CacheCleanupResult.SUCCESS) "Optimization complete" else if (state.result == CacheCleanupResult.CANCELED) "Optimization canceled" else "Optimization couldn't be completed", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text("Android cache cleanup: ${when (state.result) { CacheCleanupResult.SUCCESS -> "Completed"; CacheCleanupResult.CANCELED -> "Canceled"; else -> "Unavailable" }}", color = MaterialTheme.colorScheme.onSurfaceVariant)
        state.beforeMemory?.let { before -> state.afterMemory?.let { after -> MetricRow("Available memory", "${formatBytes(context, before.availableBytes)} → ${formatBytes(context, after.availableBytes)}") } }
        state.beforeStorage?.let { before -> state.afterStorage?.let { after -> MetricRow("Available storage", "${formatBytes(context, before.storageAvailableBytes)} → ${formatBytes(context, after.storageAvailableBytes)}") } }
        Text("These are before/after device readings. They are not presented as RAM freed by Tooliva.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.weight(1f))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}

private fun formatBytes(context: android.content.Context, bytes: Long): String = Formatter.formatFileSize(context, bytes.coerceAtLeast(0L))
