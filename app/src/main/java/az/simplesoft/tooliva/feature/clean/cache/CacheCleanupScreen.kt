package az.simplesoft.tooliva.feature.clean.cache

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.feature.clean.StorageAccessCard

@Composable
fun CacheCleanupRoute(viewModel: CacheCleanupViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    var accessActionError by remember { mutableStateOf<String?>(null) }

    val systemLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        viewModel.onSystemResult(result.resultCode)
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    val result = state.result
    if (result != null) {
        BackHandler { viewModel.clearResult() }
        CacheCleanupResultScreen(result = result, onDone = viewModel::clearResult)
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Cache cleanup", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Ask Android to clear app cache files", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        if (state.availability == CacheCleanupAvailability.PERMISSION_REQUIRED) {
            StorageAccessCard(
                fullMode = false,
                supported = true,
                errorMessage = accessActionError,
                onEnableFull = {
                    try {
                        accessCoordinator.allFilesSettingsIntent()?.let(context::startActivity)
                            ?: run { accessActionError = "Full Storage Access is not available on this Android version." }
                    } catch (_: ActivityNotFoundException) {
                        accessActionError = "Android did not provide the Full Storage Access settings screen."
                    }
                },
            )
        }

        when (state.availability) {
            CacheCleanupAvailability.AVAILABLE -> AvailableCacheCleanupContent(
                awaitingResult = state.awaitingSystemResult,
                onContinue = {
                    if (viewModel.beginLaunch()) {
                        val intent = viewModel.actionIntent()
                        if (intent == null) {
                            viewModel.launchFailed("Android did not provide the system cache cleanup screen.")
                        } else {
                            try {
                                systemLauncher.launch(intent)
                            } catch (_: ActivityNotFoundException) {
                                viewModel.launchFailed("Cache cleanup is not available on this device.")
                            } catch (_: SecurityException) {
                                viewModel.launchFailed("Android did not allow the cache cleanup request to start.")
                            }
                        }
                    }
                },
            )
            CacheCleanupAvailability.PERMISSION_REQUIRED -> PermissionRequiredContent()
            CacheCleanupAvailability.UNSUPPORTED -> UnsupportedCacheCleanupContent()
        }

        state.launchError?.let { message ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Text(message, modifier = Modifier.padding(16.dp))
            }
        }
    }
}

@Composable
private fun AvailableCacheCleanupContent(awaitingResult: Boolean, onContinue: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Icon(Icons.Outlined.Cached, contentDescription = null)
            Text("Clear app caches", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Android will show a system confirmation before clearing app cache files. Tooliva does not directly access private app data.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("What may happen", fontWeight = FontWeight.Bold)
            Text("• Apps may need to download temporary content again.\n• Some apps may open a little slower the next time.\n• User files, accounts and settings are not presented as cleanup targets.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onContinue, enabled = !awaitingResult, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Settings, contentDescription = null)
                Text(if (awaitingResult) "Waiting for Android…" else "Continue to Android", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun PermissionRequiredContent() {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Full Storage Access required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Android requires Full Storage Access for this system-mediated cache cleanup flow. No cache is touched until you continue through Android.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun UnsupportedCacheCleanupContent() {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Cache cleanup isn't available on this device", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("This Android version or device does not provide the official system cache cleanup screen.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CacheCleanupResultScreen(result: CacheCleanupResult, onDone: () -> Unit) {
    val (title, explanation) = when (result) {
        CacheCleanupResult.SUCCESS -> "Cache cleanup completed" to "Android completed the system-mediated cache cleanup. Tooliva cannot verify or claim a byte total for private app caches."
        CacheCleanupResult.CANCELED -> "Cache cleanup canceled" to "No successful system cleanup was confirmed. You can try again whenever you choose."
        CacheCleanupResult.FAILED -> "Cache cleanup couldn't be completed" to "Android or the device manufacturer did not complete the official cache cleanup flow. No byte total is available."
    }
    Column(modifier = Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Icon(Icons.Outlined.Cached, contentDescription = null)
        Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
        Text(explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Done") }
    }
}
