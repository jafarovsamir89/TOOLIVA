package az.simplesoft.tooliva.feature.clean.duplicates

import android.content.ActivityNotFoundException
import android.content.Context
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import az.simplesoft.tooliva.ui.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import az.simplesoft.tooliva.ui.LocalizedText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.MediaThumbnailLoader
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import az.simplesoft.tooliva.core.storage.tryOpen
import az.simplesoft.tooliva.core.storage.tryOpenParent
import az.simplesoft.tooliva.core.storage.tryShare
import az.simplesoft.tooliva.feature.clean.StorageAccessCard
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun ExactDuplicatesRoute(viewModel: ExactDuplicatesViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    val deleteCoordinator = remember(context) { MediaStoreDeleteCoordinator(context) }
    var accessError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }

    if (state.cleanupResult != null) {
        BackHandler { viewModel.dismissCleanupResult() }
        CleanupResultScreen(result = state.cleanupResult!!, onDone = viewModel::dismissCleanupResult)
        return
    }

    ExactDuplicatesScreen(
        state = state,
        accessError = accessError,
        actionError = actionError,
        onEnableFull = {
            try {
                accessCoordinator.allFilesSettingsIntent()?.let(context::startActivity)
                    ?: run { accessError = "Full Storage Access is not available on this Android version." }
            } catch (_: ActivityNotFoundException) { accessError = "Android did not provide the storage settings screen." }
        },
        onAnalyze = viewModel::analyze,
        onCancel = viewModel::cancelAnalysis,
        onFilter = viewModel::setFilter,
        onSort = viewModel::setSortOrder,
        onSearch = viewModel::setSearchQuery,
        onToggle = viewModel::toggleSelection,
        onKeep = viewModel::keepThisCopy,
        onClearSelection = viewModel::clearSelection,
        onDelete = { viewModel.requestDelete(deleteCoordinator) },
        onOpen = { entry -> context.tryOpen(entry)?.let { actionError = it } },
        onShare = { entry -> context.tryShare(entry)?.let { actionError = it } },
        onShowInFiles = { entry -> context.tryOpenParent(entry)?.let { actionError = it } },
        onDismissError = { viewModel.clearError(); actionError = null; accessError = null },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExactDuplicatesScreen(
    state: ExactDuplicatesUiState,
    accessError: String?,
    actionError: String?,
    onEnableFull: () -> Unit,
    onAnalyze: () -> Unit,
    onCancel: () -> Unit,
    onFilter: (DuplicateTypeFilter) -> Unit,
    onSort: (DuplicateSortOrder) -> Unit,
    onSearch: (String) -> Unit,
    onToggle: (DuplicateGroup, StorageEntry) -> Unit,
    onKeep: (DuplicateGroup, StorageEntry) -> Unit,
    onClearSelection: () -> Unit,
    onDelete: () -> Unit,
    onOpen: (StorageEntry) -> Unit,
    onShare: (StorageEntry) -> Unit,
    onShowInFiles: (StorageEntry) -> Unit,
    onDismissError: () -> Unit,
) {
    val context = LocalContext.current
    var showSort by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var detailsEntry by remember { mutableStateOf<StorageEntry?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    val visibleGroups = state.visibleGroups

    LaunchedEffect(state.errorMessage, actionError, accessError) {
        message = state.errorMessage ?: actionError ?: accessError
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (state.selectedEntries.isNotEmpty()) 104.dp else 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Exact duplicates", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Find identical files and safely keep one copy.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item {
                StorageAccessCard(
                    fullMode = state.accessState.mode == StorageAccessMode.FULL,
                    supported = state.accessState.fullStorageSupported,
                    errorMessage = accessError,
                    onEnableFull = onEnableFull,
                )
            }
            if (state.accessState.allFilesAccessGranted) {
                item { AnalyzeCard(state, onAnalyze, onCancel) }
                if (state.groups.isNotEmpty() || state.stage == DuplicateAnalysisStage.COMPLETED || state.stage == DuplicateAnalysisStage.CANCELED) {
                    item { SummaryCard(state) }
                    item {
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = onSearch,
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            placeholder = { Text("Search filename or path") },
                        )
                    }
                    item { FilterRow(state.filter, onFilter) }
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("${visibleGroups.size} groups", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            IconButton(onClick = { showSort = true }) { Icon(Icons.Outlined.Sort, contentDescription = "Sort groups") }
                        }
                    }
                    if (visibleGroups.isEmpty()) {
                        item { EmptyDuplicates(state.stage) }
                    } else {
                        items(visibleGroups, key = { it.sessionId }) { group ->
                            DuplicateGroupCard(group, state.selectedPaths, onToggle, onKeep, onOpen, onShare, onShowInFiles, { detailsEntry = it })
                        }
                    }
                }
            }
        }

        if (state.selectedEntries.isNotEmpty()) {
            Surface(Modifier.align(Alignment.BottomCenter).fillMaxWidth(), tonalElevation = 4.dp, shadowElevation = 8.dp) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${state.selectedEntries.size} · ${Formatter.formatFileSize(context, state.selectedBytes)}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    TextButton(onClick = onClearSelection) { Text("Clear") }
                    IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete selected duplicates") }
                }
            }
        }
    }

    if (showSort) {
        DropdownMenu(expanded = true, onDismissRequest = { showSort = false }) {
            listOf(
                DuplicateSortOrder.MOST_RECOVERABLE to "Most recoverable",
                DuplicateSortOrder.LARGEST to "Largest files",
                DuplicateSortOrder.MOST_COPIES to "Most copies",
                DuplicateSortOrder.NAME to "Name",
            ).forEach { (sort, label) -> DropdownMenuItem(text = { Text(label) }, onClick = { onSort(sort); showSort = false }) }
        }
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete selected duplicates?") },
            text = { Text("${state.selectedEntries.size} files selected · ${Formatter.formatFileSize(context, state.selectedBytes)}. Tooliva will verify each file and keep at least one copy per group.") },
            confirmButton = { TextButton(onClick = { showDelete = false; onDelete() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
    detailsEntry?.let { entry -> DetailsDialog(entry, onDismiss = { detailsEntry = null }) }
    message?.let { text ->
        AlertDialog(onDismissRequest = { message = null; onDismissError() }, title = { Text("Exact duplicates") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null; onDismissError() }) { Text("OK") } })
    }
}

@Composable
private fun AnalyzeCard(state: ExactDuplicatesUiState, onAnalyze: () -> Unit, onCancel: () -> Unit) {
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isAnalyzing) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp) else Icon(Icons.Outlined.PhotoLibrary, null, Modifier.size(28.dp), tint = MaterialTheme.colorScheme.primary)
                Column(Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(stageTitle(state.stage), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stageDescription(state.stage), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.isAnalyzing) {
                Text("${state.progress.filesChecked} files checked · ${state.progress.candidateFiles} candidates · ${state.progress.groupsConfirmed} groups", color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (state.progress.filesHashed > 0 || state.progress.filesReusedFromCache > 0) Text("${state.progress.filesHashed} files compared · ${state.progress.filesReusedFromCache} reused · ${Formatter.formatFileSize(LocalContext.current, state.progress.bytesHashed)} read", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            } else {
                Button(onClick = onAnalyze) { Text(if (state.stage == DuplicateAnalysisStage.IDLE) "Analyze duplicates" else "Analyze again") }
            }
        }
    }
}

@Composable
private fun SummaryCard(state: ExactDuplicatesUiState) {
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("${state.groups.size} duplicate groups", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text("${state.identicalFileCount} identical files · ${Formatter.formatFileSize(LocalContext.current, state.potentialRecoverableBytes)} potentially recoverable")
            Text("${state.progress.filesChecked} files checked · ${state.progress.filesHashed} hashed · ${state.progress.filesReusedFromCache} reused", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun FilterRow(selected: DuplicateTypeFilter, onFilter: (DuplicateTypeFilter) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(DuplicateTypeFilter.values().toList(), key = { it.name }) { filter -> FilterChip(selected = selected == filter, onClick = { onFilter(filter) }, label = { Text(filter.label) }) } }
}

@Composable
private fun DuplicateGroupCard(
    group: DuplicateGroup,
    selectedPaths: Set<String>,
    onToggle: (DuplicateGroup, StorageEntry) -> Unit,
    onKeep: (DuplicateGroup, StorageEntry) -> Unit,
    onOpen: (StorageEntry) -> Unit,
    onShare: (StorageEntry) -> Unit,
    onShowInFiles: (StorageEntry) -> Unit,
    onDetails: (StorageEntry) -> Unit,
) {
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${group.copyCount} identical copies", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${Formatter.formatFileSize(LocalContext.current, group.fileSizeBytes)} each · ${Formatter.formatFileSize(LocalContext.current, group.potentialRecoverableBytes)} potentially recoverable", color = MaterialTheme.colorScheme.onSurfaceVariant)
            HorizontalDivider()
            group.entries.forEachIndexed { index, entry ->
                DuplicateEntryRow(
                    entry = entry,
                    isFirst = index == 0,
                    selected = entry.path in selectedPaths,
                    onToggle = { onToggle(group, entry) },
                    onKeep = { onKeep(group, entry) },
                    onOpen = onOpen,
                    onShare = onShare,
                    onShowInFiles = onShowInFiles,
                    onDetails = onDetails,
                )
                if (index != group.entries.lastIndex) HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun DuplicateEntryRow(
    entry: StorageEntry,
    isFirst: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onKeep: () -> Unit,
    onOpen: (StorageEntry) -> Unit,
    onShare: (StorageEntry) -> Unit,
    onShowInFiles: (StorageEntry) -> Unit,
    onDetails: (StorageEntry) -> Unit,
) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Checkbox(checked = selected, onCheckedChange = { onToggle() })
            DuplicateThumbnail(entry, Modifier.size(56.dp).aspectRatio(1f))
            Column(Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.name.ifBlank { "Unnamed file" }, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(entry.path.substringBeforeLast(File.separator, entry.path), maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(Formatter.formatFileSize(LocalContext.current, entry.sizeBytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onKeep) { Text(if (isFirst) "Keep this copy" else "Keep") }
            TextButton(onClick = { onOpen(entry) }) { Icon(Icons.Outlined.OpenInNew, contentDescription = null); Text("Open", Modifier.padding(start = 4.dp)) }
            IconButton(onClick = { onShowInFiles(entry) }) { Icon(Icons.Outlined.FolderOpen, contentDescription = "Show in Files") }
            IconButton(onClick = { onShare(entry) }) { Icon(Icons.Outlined.Storage, contentDescription = "Share") }
            IconButton(onClick = { onDetails(entry) }) { Icon(Icons.Outlined.Info, contentDescription = "Details") }
        }
    }
}

@Composable
private fun DuplicateThumbnail(entry: StorageEntry, modifier: Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(initialValue = null, key1 = entry.path) {
        if (entry.category == StorageCategory.IMAGE || entry.category == StorageCategory.VIDEO) {
            value = withContext(Dispatchers.IO) { MediaThumbnailLoader.load(context, entry.ref) }
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = entry.name, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) { Icon(fileIcon(entry), null) }
    }
}

@Composable
private fun DetailsDialog(entry: StorageEntry, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(entry.name) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Type: ${entry.extension?.uppercase() ?: "File"}"); Text("MIME: ${entry.mimeType ?: "—"}"); Text("Size: ${Formatter.formatFileSize(LocalContext.current, entry.sizeBytes)}"); Text("Path: ${entry.path}"); Text("Modified: ${entry.modifiedAtMillis}") } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable
private fun EmptyDuplicates(stage: DuplicateAnalysisStage) {
    Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Icon(Icons.Outlined.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
        Text(if (stage == DuplicateAnalysisStage.COMPLETED) "No exact duplicates found" else "No verified groups in this session", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Tooliva compared matching-size files and verified their contents.", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun stageTitle(stage: DuplicateAnalysisStage): String = when (stage) {
    DuplicateAnalysisStage.METADATA -> "Finding possible duplicates"
    DuplicateAnalysisStage.HASHING -> "Comparing candidate files"
    DuplicateAnalysisStage.VERIFYING -> "Verifying matches"
    DuplicateAnalysisStage.COMPLETED -> "Analysis complete"
    DuplicateAnalysisStage.CANCELED -> "Analysis canceled"
    DuplicateAnalysisStage.ERROR -> "Analysis stopped"
    DuplicateAnalysisStage.IDLE -> "Ready to analyze"
}

private fun stageDescription(stage: DuplicateAnalysisStage): String = when (stage) {
    DuplicateAnalysisStage.METADATA -> "Sizes and paths are checked first."
    DuplicateAnalysisStage.HASHING -> "SHA-256 is calculated only for matching-size files."
    DuplicateAnalysisStage.VERIFYING -> "Matching hashes are verified byte by byte."
    DuplicateAnalysisStage.COMPLETED -> "Only exact, verified matches are shown."
    DuplicateAnalysisStage.CANCELED -> "Verified groups from this session remain reviewable."
    DuplicateAnalysisStage.ERROR -> "Restore access or try the analysis again."
    DuplicateAnalysisStage.IDLE -> "No scan starts until you press Analyze duplicates."
}

private fun fileIcon(entry: StorageEntry): ImageVector = when {
    entry.category == StorageCategory.IMAGE -> Icons.Outlined.Image
    entry.category == StorageCategory.VIDEO -> Icons.Outlined.Movie
    entry.category == StorageCategory.AUDIO -> Icons.Outlined.AudioFile
    entry.category == StorageCategory.ARCHIVE -> Icons.Outlined.Archive
    entry.category == StorageCategory.DOCUMENT -> Icons.Outlined.Description
    entry.category == StorageCategory.APK -> Icons.Outlined.Android
    else -> Icons.Outlined.InsertDriveFile
}
