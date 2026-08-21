package az.simplesoft.tooliva.feature.clean.downloads

import android.app.Activity
import android.content.ActivityNotFoundException
import android.os.Build
import android.text.format.DateFormat
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.storage.ApkMetadata
import az.simplesoft.tooliva.core.storage.ApkMetadataReader
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import az.simplesoft.tooliva.core.storage.tryOpen
import az.simplesoft.tooliva.core.storage.tryShare
import az.simplesoft.tooliva.feature.clean.StorageAccessCard
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import java.util.Date

@Composable
fun DownloadsAnalyzerRoute(viewModel: DownloadsAnalyzerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    val deleteCoordinator = remember(context) { MediaStoreDeleteCoordinator(context) }
    val fullMode = state.accessState.mode == StorageAccessMode.FULL
    var accessActionError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var detailsEntry by remember { mutableStateOf<StorageEntry?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        state.pendingDelete?.let { pending ->
            viewModel.onSystemDeleteResult(
                requestId = pending.requestId,
                approved = result.resultCode == Activity.RESULT_OK,
                coordinator = deleteCoordinator,
            )
        }
    }

    LaunchedEffect(state.pendingDelete?.requestId) {
        state.pendingDelete?.let { pending ->
            deleteLauncher.launch(IntentSenderRequest.Builder(pending.intentSender).build())
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }

    val cleanupResult = state.cleanupResult
    if (cleanupResult != null) {
        BackHandler { viewModel.dismissCleanupResult() }
        CleanupResultScreen(result = cleanupResult, onDone = viewModel::dismissCleanupResult)
        return
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete selected Downloads?") },
            text = {
                Text(
                    "${state.selectedFiles.size} item(s), ${Formatter.formatFileSize(context, state.selectedBytes)} selected. " +
                        "Full Storage Mode will delete these files after confirmation. Nothing changes if you cancel.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.requestDelete(deleteCoordinator)
                    },
                    enabled = state.selectedFiles.isNotEmpty(),
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } },
        )
    }

    detailsEntry?.let { entry ->
        DownloadDetailsDialog(
            entry = entry,
            metadata = if (entry.category == StorageCategory.APK) ApkMetadataReader.read(context, entry) else null,
            onDismiss = { detailsEntry = null },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (state.selectedFiles.isNotEmpty()) 104.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Downloads", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(
                        "Review installers, archives, documents and old downloads. Nothing is deleted automatically.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            item {
                StorageAccessCard(
                    fullMode = fullMode,
                    supported = state.accessState.fullStorageSupported,
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

            if (!fullMode) {
                item {
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Text("Full Storage Access required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Automatic Downloads analysis needs access to the Downloads folders. Limited Mode does not pretend to cover the whole folder.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = {
                                try {
                                    accessCoordinator.allFilesSettingsIntent()?.let(context::startActivity)
                                } catch (_: ActivityNotFoundException) {
                                    accessActionError = "Android did not provide the Full Storage Access settings screen."
                                }
                            }) { Text("Open Full Storage Access") }
                        }
                    }
                }
            } else {
                item {
                    SummaryCard(
                        state = state,
                        context = context,
                        onCategory = { viewModel.setCategoryFilter(it) },
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = viewModel::analyze, enabled = !state.isLoading && !state.isPreparingDelete) {
                            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                            Text(if (state.isLoading) "Analyzing…" else "Analyze Downloads")
                        }
                        if (state.isLoading) {
                            OutlinedButton(onClick = viewModel::cancelAnalyze) {
                                Icon(Icons.Outlined.Cancel, contentDescription = null)
                                Text("Cancel", modifier = Modifier.padding(start = 6.dp))
                            }
                        }
                    }
                }

                if (state.hasAnalyzed) {
                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::setSearchQuery,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            trailingIcon = if (state.searchQuery.isNotEmpty()) {
                                { IconButton(onClick = { viewModel.setSearchQuery("") }) { Icon(Icons.Outlined.Clear, contentDescription = "Clear search") } }
                            } else null,
                            placeholder = { Text("Search Downloads") },
                        )
                    }
                    item { CategoryFilters(state, viewModel) }
                    item { AgeAndSizeFilters(state, viewModel) }
                    item {
                        Box {
                            OutlinedButton(onClick = { showSortMenu = true }) { Text("Sort: ${sortLabel(state.sortOrder)}") }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                StorageSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(
                                        text = { Text(sortLabel(order)) },
                                        onClick = { showSortMenu = false; viewModel.setSortOrder(order) },
                                    )
                                }
                            }
                        }
                    }
                    state.errorMessage?.let { message ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Text(message, modifier = Modifier.padding(16.dp))
                            }
                        }
                    }
                    if (state.visibleFiles.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text("${state.visibleFiles.size} files · ${Formatter.formatFileSize(context, state.visibleFiles.sumOf { it.sizeBytes })}", fontWeight = FontWeight.Bold)
                                TextButton(onClick = viewModel::toggleSelectAllVisible) { Text(if (state.allVisibleSelected) "Clear all" else "Select all") }
                            }
                        }
                    }
                    if (!state.isLoading && state.visibleFiles.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("No matching Downloads", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Try another category, age or size filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    items(state.visibleFiles, key = { it.ref.toString() }) { entry ->
                        DownloadFileCard(
                            entry = entry,
                            selected = entry.ref.toString() in state.selectedRefs,
                            onToggle = { viewModel.toggleSelection(entry.ref.toString()) },
                            onOpen = { actionError = context.tryOpen(entry) },
                            onShare = { actionError = context.tryShare(entry) },
                            onDetails = { detailsEntry = entry },
                        )
                    }
                    actionError?.let { message ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text(message, modifier = Modifier.weight(1f))
                                    TextButton(onClick = { actionError = null }) { Text("Dismiss") }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.selectedFiles.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Button(
                    onClick = { showDeleteConfirmation = true },
                    enabled = !state.isPreparingDelete,
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                ) {
                    if (state.isPreparingDelete) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text("Delete ${state.selectedFiles.size} · ${Formatter.formatFileSize(context, state.selectedBytes)}", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    state: DownloadsAnalyzerUiState,
    context: android.content.Context,
    onCategory: (DownloadsCategoryFilter) -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val totalBytes = state.files.sumOf { it.sizeBytes }
            Text("Downloads analyzed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${state.files.size} files · ${Formatter.formatFileSize(context, totalBytes)}", color = MaterialTheme.colorScheme.primary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.summaries.filter { it.count > 0 }, key = { it.category }) { summary ->
                    Card(
                        onClick = { onCategory(summary.category) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(downloadsCategoryLabel(summary.category), fontWeight = FontWeight.SemiBold)
                            Text(Formatter.formatFileSize(context, summary.bytes), color = MaterialTheme.colorScheme.primary)
                            Text("${summary.count} files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryFilters(state: DownloadsAnalyzerUiState, viewModel: DownloadsAnalyzerViewModel) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(DownloadsCategoryFilter.entries, key = { it }) { filter ->
            FilterChip(
                selected = state.categoryFilter == filter,
                onClick = { viewModel.setCategoryFilter(filter) },
                label = { Text(downloadsCategoryLabel(filter)) },
            )
        }
    }
}

@Composable
private fun AgeAndSizeFilters(state: DownloadsAnalyzerUiState, viewModel: DownloadsAnalyzerViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Age", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold) }
            item {
                FilterChip(selected = state.ageFilter == null, onClick = { viewModel.setAgeFilter(null) }, label = { Text("Any age") })
            }
            items(ageFilters, key = { it.days }) { filter ->
                FilterChip(selected = state.ageFilter == filter, onClick = { viewModel.setAgeFilter(filter) }, label = { Text(filter.label) })
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { Text("Size", modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.SemiBold) }
            item {
                FilterChip(selected = state.sizeFilter == null, onClick = { viewModel.setSizeFilter(null) }, label = { Text("Any size") })
            }
            items(sizeFilters, key = { it.bytes }) { filter ->
                FilterChip(selected = state.sizeFilter == filter, onClick = { viewModel.setSizeFilter(filter) }, label = { Text(filter.label) })
            }
        }
    }
}

@Composable
private fun DownloadFileCard(
    entry: StorageEntry,
    selected: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
) {
    Card(
        onClick = onToggle,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
                Icon(downloadCategoryIcon(entry), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(entry.name.ifBlank { "Unnamed file" }, fontWeight = FontWeight.Bold, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(downloadsCategoryLabel(DownloadsAnalyzerRules.analyzerCategory(entry)), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(entry.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${Formatter.formatFileSize(LocalContext.current, entry.sizeBytes)} · ${DateFormat.getDateFormat(LocalContext.current).format(Date(entry.modifiedAtMillis))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    TextButton(onClick = onOpen) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null); Text("Open", Modifier.padding(start = 4.dp)) }
                    IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "Share ${entry.name}") }
                    IconButton(onClick = onDetails) { Icon(Icons.Outlined.Info, contentDescription = "Details for ${entry.name}") }
                }
            }
        }
    }
}

@Composable
private fun DownloadDetailsDialog(entry: StorageEntry, metadata: ApkMetadata?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(entry.name, fontWeight = FontWeight.Bold)
                Text("Type: ${downloadsCategoryLabel(DownloadsAnalyzerRules.analyzerCategory(entry))}")
                Text("Size: ${Formatter.formatFileSize(LocalContext.current, entry.sizeBytes)}")
                Text("Modified: ${DateFormat.getDateFormat(LocalContext.current).format(Date(entry.modifiedAtMillis))}")
                Text("Path: ${entry.path}")
                metadata?.let {
                    Text("App: ${it.label ?: "Unknown"}")
                    Text("Package: ${it.packageName ?: "Unavailable"}")
                    Text("Version: ${it.versionName ?: "Unavailable"} (${it.versionCode ?: "Unavailable"})")
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private val ageFilters = listOf(
    DownloadsAgeFilter(30, "30+ days"),
    DownloadsAgeFilter(90, "90+ days"),
    DownloadsAgeFilter(180, "180+ days"),
    DownloadsAgeFilter(365, "365+ days"),
)

private val sizeFilters = listOf(
    DownloadsSizeFilter(100L * 1024L * 1024L, "100 MB+"),
    DownloadsSizeFilter(500L * 1024L * 1024L, "500 MB+"),
    DownloadsSizeFilter(1024L * 1024L * 1024L, "1 GB+"),
)

private fun downloadsCategoryLabel(category: DownloadsCategoryFilter): String = when (category) {
    DownloadsCategoryFilter.ALL -> "All"
    DownloadsCategoryFilter.APK -> "APK installers"
    DownloadsCategoryFilter.ARCHIVE -> "Archives"
    DownloadsCategoryFilter.DOCUMENT -> "Documents"
    DownloadsCategoryFilter.MEDIA -> "Media"
    DownloadsCategoryFilter.OTHER -> "Other"
}

private fun downloadCategoryIcon(entry: StorageEntry): ImageVector = when (entry.category) {
    StorageCategory.VIDEO -> Icons.Outlined.Movie
    StorageCategory.IMAGE -> Icons.Outlined.Image
    StorageCategory.AUDIO -> Icons.Outlined.AudioFile
    StorageCategory.APK -> Icons.AutoMirrored.Outlined.InsertDriveFile
    StorageCategory.ARCHIVE -> Icons.Outlined.Archive
    StorageCategory.DOCUMENT -> Icons.Outlined.Description
    StorageCategory.DOWNLOAD -> Icons.Outlined.Download
    StorageCategory.ALL, StorageCategory.OTHER -> Icons.Outlined.FolderOpen
}

private fun sortLabel(order: StorageSortOrder): String = when (order) {
    StorageSortOrder.SIZE -> "Size"
    StorageSortOrder.NEWEST -> "Newest"
    StorageSortOrder.OLDEST -> "Oldest"
    StorageSortOrder.NAME -> "Name"
}
