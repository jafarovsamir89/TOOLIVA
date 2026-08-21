package az.simplesoft.tooliva.feature.clean

import android.content.ActivityNotFoundException
import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import az.simplesoft.tooliva.ui.LocalizedIcon as Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.feature.home.HomeViewModel
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import az.simplesoft.tooliva.ui.theme.ToolivaSpacing

private data class CleanTool(val id: String, val title: String, val subtitle: String, val icon: ImageVector)

private val cleanTools = listOf(
    CleanTool("large-files", "Large files", "Find the biggest files worth reviewing", Icons.Outlined.Storage),
    CleanTool("downloads", "Downloads", "Review installers, archives, documents and old downloads", Icons.Outlined.Download),
    CleanTool("recommendations", "Files to review", "Old installers, downloads and conservative residual candidates", Icons.Outlined.FindInPage),
    CleanTool("cache", "Cache Cleaner", "Review browser and YouTube app caches", Icons.Outlined.Storage),
    CleanTool("screenshots", "Screenshots", "Review old screenshots by age", Icons.Outlined.Screenshot),
    CleanTool("photo-analyzer", "Photo Analyzer", "Review similar, blurry and large media on-device", Icons.Outlined.Image),
    CleanTool("duplicates", "Exact duplicates", "Find identical files and safely keep one copy", Icons.Outlined.PhotoLibrary),
    CleanTool("old-files", "Old files", "Review conservative age-based candidates", Icons.Outlined.VideoLibrary),
    CleanTool("empty-folders", "Empty folders", "Review safe empty folders", Icons.Outlined.FolderDelete),
    CleanTool("cleanup-swipe", "Cleanup Swipe", "Keep or delete files with a fast review flow", Icons.Outlined.DeleteSweep),
)

@Composable
fun CleanRoute(onOpenTool: (String) -> Unit, viewModel: HomeViewModel = viewModel(), analysisViewModel: CleanerAnalysisViewModel = viewModel()) {
    val homeState by viewModel.uiState.collectAsStateWithLifecycle()
    val analysisState by analysisViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    var accessState by remember(context) { mutableStateOf(accessCoordinator.currentState()) }
    var accessActionError by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        accessState = accessCoordinator.currentState()
        analysisViewModel.refreshAccess()
    }
    val used = Formatter.formatFileSize(context, homeState.storageUsedBytes)
    val total = Formatter.formatFileSize(context, homeState.storageTotalBytes)
    val free = Formatter.formatFileSize(context, homeState.storageAvailableBytes)
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(ToolivaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Clean", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text("Review real files. Nothing is deleted automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Row(modifier = Modifier.fillMaxWidth().padding(ToolivaSpacing.xl), horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.xl), verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(progress = { homeState.storageUsedFraction }, modifier = Modifier.padding(ToolivaSpacing.xs), strokeWidth = 9.dp, trackColor = MaterialTheme.colorScheme.surfaceVariant)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Storage", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("$used of $total used")
                        Text("$free available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            StorageAccessCard(fullMode = accessState.mode == StorageAccessMode.FULL, supported = accessState.fullStorageSupported, errorMessage = accessActionError, onEnableFull = {
                try {
                    accessCoordinator.allFilesSettingsIntent()?.let(context::startActivity) ?: run { accessActionError = "Full Storage Access is not available on this Android version." }
                } catch (_: ActivityNotFoundException) { accessActionError = "Android did not provide the Full Storage Access settings screen." }
            })
        }
        item {
            Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                Column(modifier = Modifier.padding(ToolivaSpacing.xl), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
                    Text("Cleaner analysis", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(when (analysisState.status) {
                        CleanerAnalysisStatus.ANALYZING -> "Checking accessible files and folders…"
                        CleanerAnalysisStatus.COMPLETE -> "Analysis complete. Choose a category to review."
                        CleanerAnalysisStatus.CANCELLED -> "Analysis canceled. Partial results remain visible."
                        CleanerAnalysisStatus.ERROR -> analysisState.errorMessage ?: "Analysis could not be completed."
                        CleanerAnalysisStatus.IDLE -> if (analysisState.accessState.mode == StorageAccessMode.FULL) "One scan builds a live review plan. Nothing is selected or deleted automatically." else "Limited Mode shows only media Android exposes through MediaStore."
                    }, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (analysisState.isAnalyzing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Text("${analysisState.snapshot.filesChecked} files · ${analysisState.snapshot.foldersChecked} folders · ${Formatter.formatFileSize(context, analysisState.snapshot.bytesChecked)} checked", style = MaterialTheme.typography.bodySmall)
                        OutlinedButton(onClick = analysisViewModel::cancelAnalyze) { Text("Cancel scan") }
                    } else Button(onClick = analysisViewModel::analyze) { Text(if (analysisState.status == CleanerAnalysisStatus.IDLE) "Analyze storage" else "Run analysis again") }
                }
            }
        }
        if (analysisState.snapshot.summaries.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Action plan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Reviewable bytes are not a promise of reclaimable space. Each category opens its review flow.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(analysisState.snapshot.summaries, key = { it.bucket.name }) { summary ->
                Card(onClick = { onOpenTool(summary.bucket.route) }, shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Row(modifier = Modifier.fillMaxWidth().padding(ToolivaSpacing.xl), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
                        Icon(cleanerBucketIcon(summary.bucket), contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(summary.bucket.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("${summary.count} item(s) · ${Formatter.formatFileSize(context, summary.bytes)} reviewable")
                            Text(summary.bucket.explanation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item { Text("Cleanup tools", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        items(cleanTools, key = { it.id }) { tool ->
            Card(onClick = { onOpenTool(tool.id) }, shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(modifier = Modifier.fillMaxWidth().padding(ToolivaSpacing.xl), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
                    Icon(tool.icon, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(tool.subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

private fun cleanerBucketIcon(bucket: CleanerBucket): ImageVector = when (bucket) {
    CleanerBucket.LARGE_FILES -> Icons.Outlined.Storage
    CleanerBucket.DOWNLOADS, CleanerBucket.APK_INSTALLERS -> Icons.Outlined.Download
    CleanerBucket.ARCHIVES -> Icons.Outlined.Archive
    CleanerBucket.DOCUMENTS -> Icons.Outlined.Description
    CleanerBucket.IMAGES, CleanerBucket.SCREENSHOTS -> Icons.Outlined.Image
    CleanerBucket.VIDEOS -> Icons.Outlined.Movie
    CleanerBucket.AUDIO -> Icons.Outlined.AudioFile
    CleanerBucket.OLD_FILES -> Icons.Outlined.FindInPage
    CleanerBucket.EMPTY_FOLDERS -> Icons.Outlined.FolderDelete
    CleanerBucket.RESIDUALS -> Icons.Outlined.DeleteSweep
}
