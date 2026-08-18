package az.simplesoft.tooliva.feature.clean

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Cached
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoLibrary
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
import android.content.ActivityNotFoundException
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.feature.home.HomeViewModel
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

private data class CleanTool(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val cleanTools = listOf(
    CleanTool("large-files", "Large files", "Find the biggest files worth reviewing", Icons.Outlined.Storage),
    CleanTool("downloads", "Downloads", "Review installers, archives, documents and old downloads", Icons.Outlined.Download),
    CleanTool("recommendations", "Files to review", "Old installers and downloads you may no longer need", Icons.Outlined.FindInPage),
    CleanTool("cache", "Cache cleanup", "Ask Android to clear app cache files", Icons.Outlined.Cached),
    CleanTool("screenshots", "Screenshots", "Review old screenshots by age", Icons.Outlined.Screenshot),
    CleanTool("duplicates", "Exact duplicates", "Find byte-identical photos safely", Icons.Outlined.PhotoLibrary),
    CleanTool("old-videos", "Old videos", "Review large and old videos", Icons.Outlined.VideoLibrary),
    CleanTool("cleanup-swipe", "Cleanup Swipe", "Keep or delete files with a fast review flow", Icons.Outlined.DeleteSweep),
)

@Composable
fun CleanRoute(
    onOpenTool: (String) -> Unit,
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    var accessState by remember(context) { mutableStateOf(accessCoordinator.currentState()) }
    var accessActionError by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessState = accessCoordinator.currentState()
    }
    val used = Formatter.formatFileSize(context, state.storageUsedBytes)
    val total = Formatter.formatFileSize(context, state.storageTotalBytes)
    val free = Formatter.formatFileSize(context, state.storageAvailableBytes)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Clean", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    "Review real files. Nothing is deleted automatically.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(
                        progress = { state.storageUsedFraction },
                        modifier = Modifier.padding(4.dp),
                        strokeWidth = 9.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Storage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$used of $total used")
                        Text(
                            "$free available",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        item {
            StorageAccessCard(
                fullMode = accessState.mode == az.simplesoft.tooliva.core.storage.StorageAccessMode.FULL,
                supported = accessState.fullStorageSupported,
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

        item {
            Text(
                "Cleanup tools",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        items(cleanTools, key = { it.id }) { tool ->
            Card(
                onClick = { onOpenTool(tool.id) },
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Icon(tool.icon, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            tool.subtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
