package az.simplesoft.tooliva.feature.files

import android.content.ActivityNotFoundException
import android.content.Context
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import az.simplesoft.tooliva.ui.theme.ToolivaSpacing
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageFileActions
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import az.simplesoft.tooliva.core.storage.StorageVolumeInfo
import az.simplesoft.tooliva.core.storage.tryOpen
import az.simplesoft.tooliva.core.storage.tryShare
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import java.io.File

@Composable
fun FileManagerRoute(
    onOpenLargeFiles: () -> Unit,
    initialDirectory: String? = null,
    viewModel: FileManagerViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var accessError by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }
    LaunchedEffect(initialDirectory, state.accessState.allFilesAccessGranted, state.volumes) {
        if (initialDirectory != null && state.accessState.allFilesAccessGranted && state.volumes.isNotEmpty() && state.currentDirectory?.absolutePath != initialDirectory) {
            viewModel.openDirectory(File(initialDirectory))
        }
    }

    BackHandler(
        enabled = state.currentDirectory != null &&
            state.currentDirectory?.absolutePath != state.currentVolume?.root?.absolutePath &&
            state.cleanupResult == null,
    ) {
        viewModel.goUp()
    }

    if (state.cleanupResult != null) {
        BackHandler { viewModel.dismissCleanupResult() }
        CleanupResultScreen(result = state.cleanupResult!!, onDone = viewModel::dismissCleanupResult)
        return
    }

    FileManagerScreen(
        state = state,
        visibleEntries = viewModel.visibleEntries(),
        selectedBytes = viewModel.selectedBytes(),
        accessError = accessError,
        onOpenSettings = {
            try {
                StorageAccessCoordinator(context).allFilesSettingsIntent()?.let(context::startActivity)
                    ?: run { accessError = "Full Storage Access is not available on this Android version." }
            } catch (_: ActivityNotFoundException) { accessError = "Android did not provide the storage settings screen." }
        },
        onRefresh = viewModel::refreshAccess,
        onOpenVolume = viewModel::openVolume,
        onOpenDirectory = viewModel::openDirectory,
        onGoHome = viewModel::goHome,
        onGoUp = viewModel::goUp,
        onSearch = viewModel::setSearchQuery,
        onRecursiveSearch = viewModel::runRecursiveSearch,
        onCancelSearch = viewModel::cancelSearch,
        onShortcut = { if (it == FileManagerShortcut.LARGE) onOpenLargeFiles() else viewModel.runShortcut(it) },
        onSort = viewModel::setSortOrder,
        onViewMode = viewModel::setViewMode,
        onToggleSelection = viewModel::toggleSelection,
        onSelectAll = viewModel::selectAllVisible,
        onClearSelection = viewModel::clearSelection,
        onOpen = { entry -> if (entry.isDirectory) viewModel.openDirectory(File(entry.path)) else context.tryOpen(entry)?.let { accessError = it } },
        onShare = { entry -> context.tryShare(entry)?.let { accessError = it } },
        onDetails = viewModel::showDetails,
        onDismissDetails = viewModel::dismissDetails,
        onRename = viewModel::rename,
        onCreateFolder = viewModel::createFolder,
        onStartOperation = viewModel::startOperation,
        onCancelOperation = viewModel::cancelOperation,
        onDismissOperation = viewModel::dismissOperationResult,
        onOpenDestination = viewModel::openDestination,
        onClearDestination = viewModel::clearDestination,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileManagerScreen(
    state: FileManagerUiState,
    visibleEntries: List<StorageEntry>,
    selectedBytes: Long,
    accessError: String?,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    onOpenVolume: (StorageVolumeInfo) -> Unit,
    onOpenDirectory: (File) -> Unit,
    onGoHome: () -> Unit,
    onGoUp: () -> Unit,
    onSearch: (String) -> Unit,
    onRecursiveSearch: () -> Unit,
    onCancelSearch: () -> Unit,
    onShortcut: (FileManagerShortcut) -> Unit,
    onSort: (StorageSortOrder) -> Unit,
    onViewMode: (FileManagerViewMode) -> Unit,
    onToggleSelection: (StorageEntry) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOpen: (StorageEntry) -> Unit,
    onShare: (StorageEntry) -> Unit,
    onDetails: (StorageEntry) -> Unit,
    onDismissDetails: () -> Unit,
    onRename: (StorageEntry, String) -> String?,
    onCreateFolder: (String) -> String?,
    onStartOperation: (FileOperationKind, File?, CollisionPolicy) -> Unit,
    onCancelOperation: () -> Unit,
    onDismissOperation: () -> Unit,
    onOpenDestination: (File) -> Unit,
    onClearDestination: () -> Unit,
) {
    var showSort by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf<StorageEntry?>(null) }
    var showCreateFolder by remember { mutableStateOf(false) }
    var showDestination by remember { mutableStateOf<FileOperationKind?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    val selectedEntries = state.entries.filter { it.path in state.selectedPaths }
    LaunchedEffect(state.error) { if (state.error != null) message = state.error }
    LaunchedEffect(accessError) { if (accessError != null) message = accessError }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(if (state.currentDirectory == null) "Files" else state.currentDirectory.name.ifBlank { "Internal storage" }) },
            navigationIcon = { if (state.currentDirectory != null) IconButton(onClick = onGoUp) { Icon(Icons.Outlined.FolderOpen, contentDescription = "Up") } },
            actions = {
                if (state.currentDirectory == null) IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Storage, contentDescription = "Refresh") }
                if (state.currentDirectory != null) {
                    IconButton(onClick = { showCreateFolder = true }) { Icon(Icons.Outlined.CreateNewFolder, contentDescription = "Create folder") }
                    IconButton(onClick = { showSort = true }) { Icon(Icons.Outlined.Sort, contentDescription = "Sort") }
                    IconButton(onClick = { onViewMode(if (state.viewMode == FileManagerViewMode.LIST) FileManagerViewMode.GRID else FileManagerViewMode.LIST) }) {
                        Icon(if (state.viewMode == FileManagerViewMode.LIST) Icons.Outlined.GridView else Icons.Outlined.List, contentDescription = "Change view")
                    }
                }
            },
        )

            Box(Modifier.weight(1f)) {
            if (state.currentDirectory == null) {
                RootContent(state, accessError, onOpenSettings, onOpenVolume, onShortcut)
            } else {
                BrowserContent(
                    state, visibleEntries, onSearch, onRecursiveSearch, onCancelSearch, onToggleSelection, onSelectAll,
                    onClearSelection, onOpen, onDetails,
                )
            }
            }
        }

    if (showSort) {
        DropdownMenu(expanded = true, onDismissRequest = { showSort = false }) {
            listOf(StorageSortOrder.NAME to "Name A–Z", StorageSortOrder.NEWEST to "Newest", StorageSortOrder.OLDEST to "Oldest", StorageSortOrder.SIZE to "Largest").forEach { (order, label) ->
                DropdownMenuItem(text = { Text(label) }, onClick = { onSort(order); showSort = false })
            }
        }
    }
    if (showMore) {
        AlertDialog(onDismissRequest = { showMore = false }, title = { Text("More actions") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Choose an action for the selected items.")
                TextButton(onClick = { selectedEntries.firstOrNull()?.let { showRename = it }; showMore = false }) { Text("Rename first selected") }
            }
        }, confirmButton = {
            TextButton(onClick = { selectedEntries.firstOrNull()?.let(onDetails); showMore = false }) { Text("Details") }
        }, dismissButton = { TextButton(onClick = { showMore = false }) { Text("Cancel") } })
    }
    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete selected items?") },
            text = { Text("${selectedEntries.size} item(s), ${Formatter.formatFileSize(LocalContext.current, selectedBytes)}. ${if (selectedEntries.any { it.isDirectory }) "Selected folders and their contents will be removed. " else ""}This is an explicit permanent deletion in Full Storage Mode.") },
            confirmButton = { TextButton(onClick = { showDelete = false; onStartOperation(FileOperationKind.DELETE, null, CollisionPolicy.REPLACE) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
        )
    }
    showRename?.let { entry -> RenameDialog(entry, onDismiss = { showRename = null }, onRename = { newName -> message = onRename(entry, newName); if (message == null) showRename = null }) }
    if (showCreateFolder) CreateFolderDialog(onDismiss = { showCreateFolder = false }, onCreate = { name -> message = onCreateFolder(name); if (message == null) showCreateFolder = false })
    state.detailsEntry?.let { DetailsDialog(it, onDismissDetails) }
    state.operationProgress?.let { OperationProgressDialog(it, onCancelOperation) }
    state.operationResult?.let { result -> OperationResultDialog(result, onDismissOperation) }
    if (showDestination != null) {
        DestinationDialog(
            kind = showDestination!!,
            state = state,
            onOpen = onOpenDestination,
            onSelect = { destination, policy -> val kind = showDestination!!; showDestination = null; onClearDestination(); onStartOperation(kind, destination, policy) },
            onDismiss = { showDestination = null; onClearDestination() },
        )
    }

    if (selectedEntries.isNotEmpty()) {
        Surface(shadowElevation = 8.dp, tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter)) {
            Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${selectedEntries.size} · ${Formatter.formatFileSize(LocalContext.current, selectedBytes)}", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                IconButton(onClick = { selectedEntries.firstOrNull()?.let(onShare) }) { Icon(Icons.Outlined.Share, contentDescription = "Share") }
                IconButton(onClick = { showDestination = FileOperationKind.COPY; state.currentDirectory?.let(onOpenDestination) }) { Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy") }
                IconButton(onClick = { showDestination = FileOperationKind.MOVE; state.currentDirectory?.let(onOpenDestination) }) { Icon(Icons.Outlined.UploadFile, contentDescription = "Move") }
                IconButton(onClick = { showDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete") }
                IconButton(onClick = { showMore = true }) { Icon(Icons.Outlined.MoreVert, contentDescription = "More") }
            }
        }
    }
    message?.let { text ->
        AlertDialog(onDismissRequest = { message = null }, title = { Text("Files") }, text = { Text(text) }, confirmButton = { TextButton(onClick = { message = null }) { Text("OK") } })
    }
    }
}

@Composable
private fun RootContent(
    state: FileManagerUiState,
    accessError: String?,
    onOpenSettings: () -> Unit,
    onOpenVolume: (StorageVolumeInfo) -> Unit,
    onShortcut: (FileManagerShortcut) -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Browse shared storage", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black) }
        item { Text("Open a folder to browse. Searches and category scans start only when you ask.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        if (!state.accessState.allFilesAccessGranted) {
            item {
                Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.Storage, contentDescription = null)
                        Text("Full Storage Access is needed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Android limits direct browsing until you allow access in system settings. Tooliva does not add another storage permission.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = onOpenSettings) { Text("Allow access") }
                        if (accessError != null) Text(accessError, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        } else {
            items(state.volumes, key = { it.id }) { volume -> VolumeCard(volume, onClick = { onOpenVolume(volume) }) }
            item { Text("Categories", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FileManagerShortcut.values().toList().chunked(3).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            row.forEach { shortcut -> ShortcutCard(shortcut, modifier = Modifier.weight(1f), onClick = { onShortcut(shortcut) }) }
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VolumeCard(volume: StorageVolumeInfo, onClick: () -> Unit) {
    Card(onClick = onClick, shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Storage, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(volume.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("${Formatter.formatFileSize(LocalContext.current, volume.availableBytes)} free of ${Formatter.formatFileSize(LocalContext.current, volume.totalBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ShortcutCard(shortcut: FileManagerShortcut, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(onClick = onClick, modifier = modifier.height(56.dp), contentPadding = PaddingValues(horizontal = 6.dp)) { Text(shortcut.title, maxLines = 2) }
}

@Composable
private fun BrowserContent(
    state: FileManagerUiState,
    entries: List<StorageEntry>,
    onSearch: (String) -> Unit,
    onRecursiveSearch: () -> Unit,
    onCancelSearch: () -> Unit,
    onToggleSelection: (StorageEntry) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onOpen: (StorageEntry) -> Unit,
    onDetails: (StorageEntry) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(value = state.searchQuery, onValueChange = onSearch, modifier = Modifier.weight(1f), singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text("Search this folder") })
            IconButton(onClick = if (state.isSearching) onCancelSearch else onSelectAll) { Text(if (state.isSearching) "×" else "✓", style = MaterialTheme.typography.titleLarge) }
        }
        if (state.searchQuery.isNotBlank() && !state.recursiveSearch) {
            TextButton(onClick = onRecursiveSearch, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Search in this folder") }
        }
        if (state.isSearching) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                Text("${state.progressMatches} matches · ${state.progressVisited} checked", color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = onCancelSearch) { Text("Cancel") }
            }
        }
        if (state.recursiveSearch) TextButton(onClick = onClearSelection, modifier = Modifier.padding(horizontal = 12.dp)) { Text("Clear selection") }
        HorizontalDivider(Modifier.padding(top = 4.dp))
        if (entries.isEmpty() && !state.isLoading && !state.isSearching) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text("No files here", style = MaterialTheme.typography.titleLarge); Text("Try another folder or search.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else if (state.viewMode == FileManagerViewMode.LIST) {
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) { items(entries, key = { it.path }) { entry -> FileRow(entry = entry, itIsSelected = entry.path in state.selectedPaths, onOpen = onOpen, onToggleSelection = onToggleSelection, onDetails = onDetails) } }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(entries, key = { it.path }) { entry -> FileGridCell(entry, entry.path in state.selectedPaths, onOpen, onToggleSelection) } }
        }
    }
}

@Composable
private fun FileRow(entry: StorageEntry, itIsSelected: Boolean, onOpen: (StorageEntry) -> Unit, onToggleSelection: (StorageEntry) -> Unit, onDetails: (StorageEntry) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(entry) }, onLongClick = { onToggleSelection(entry) }), shape = ToolivaShapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = itIsSelected, onCheckedChange = { onToggleSelection(entry) })
            Icon(fileIcon(entry), null, Modifier.size(28.dp), tint = if (entry.isDirectory) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary)
            Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(entry.name, maxLines = 1, style = MaterialTheme.typography.bodyLarge); Text(if (entry.isDirectory) "Folder" else Formatter.formatFileSize(LocalContext.current, entry.sizeBytes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = { onDetails(entry) }) { Icon(Icons.Outlined.MoreVert, "Details") }
        }
    }
}

@Composable
private fun FileGridCell(entry: StorageEntry, selected: Boolean, onOpen: (StorageEntry) -> Unit, onToggleSelection: (StorageEntry) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = { onOpen(entry) }, onLongClick = { onToggleSelection(entry) }), shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(fileIcon(entry), null, Modifier.size(42.dp)); Text(entry.name, maxLines = 2, style = MaterialTheme.typography.bodyMedium); Text(if (entry.isDirectory) "Folder" else Formatter.formatFileSize(LocalContext.current, entry.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun fileIcon(entry: StorageEntry): ImageVector = when {
    entry.isDirectory -> Icons.Outlined.Folder
    entry.category == StorageCategory.IMAGE -> Icons.Outlined.Image
    entry.category == StorageCategory.VIDEO -> Icons.Outlined.Movie
    entry.category == StorageCategory.AUDIO -> Icons.Outlined.AudioFile
    entry.category == StorageCategory.ARCHIVE -> Icons.Outlined.Archive
    entry.category == StorageCategory.DOCUMENT -> Icons.Outlined.Description
    entry.category == StorageCategory.APK -> Icons.Outlined.Android
    else -> Icons.Outlined.InsertDriveFile
}

@Composable
private fun RenameDialog(entry: StorageEntry, onDismiss: () -> Unit, onRename: (String) -> Unit) {
    var value by remember(entry.path) { mutableStateOf(entry.name) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Rename") }, text = { OutlinedTextField(value, { value = it }, singleLine = true) }, confirmButton = { TextButton(onClick = { onRename(value) }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun CreateFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var value by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("New folder") }, text = { OutlinedTextField(value, { value = it }, singleLine = true, placeholder = { Text("Folder name") }) }, confirmButton = { TextButton(onClick = { onCreate(value) }) { Text("Create") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}

@Composable
private fun DetailsDialog(entry: StorageEntry, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(entry.name) }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { DetailLine("Type", if (entry.isDirectory) "Folder" else entry.extension?.uppercase() ?: "File"); DetailLine("MIME", entry.mimeType ?: "—"); DetailLine("Size", if (entry.isDirectory) "Folder size is not calculated recursively" else Formatter.formatFileSize(LocalContext.current, entry.sizeBytes)); DetailLine("Path", entry.path); DetailLine("Readable", File(entry.path).canRead().toString()); DetailLine("Writable", File(entry.path).canWrite().toString()) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}

@Composable private fun DetailLine(label: String, value: String) { Text("$label: $value", style = MaterialTheme.typography.bodyMedium) }

@Composable
private fun OperationProgressDialog(progress: FileOperationProgress, onCancel: () -> Unit) {
    AlertDialog(onDismissRequest = {}, title = { Text("${progress.kind.name.lowercase().replaceFirstChar { it.uppercase() }} in progress") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { LinearProgress(progress.completedBytes, progress.totalBytes); Text(progress.currentName); Text("${progress.completedItems} of ${progress.totalItems} items") } }, confirmButton = { TextButton(onClick = onCancel) { Text("Cancel") } })
}

@Composable private fun LinearProgress(done: Long, total: Long) { androidx.compose.material3.LinearProgressIndicator(progress = { if (total > 0) (done.toFloat() / total).coerceIn(0f, 1f) else 0f }, modifier = Modifier.fillMaxWidth()) }

@Composable
private fun OperationResultDialog(result: FileOperationResult, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (result.canceled) "Operation canceled" else "Operation complete") }, text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Completed: ${result.completedItems}"); Text("Skipped: ${result.skippedItems}"); Text("Failed: ${result.failedItems}"); if (result.errors.isNotEmpty()) Text(result.errors.joinToString("\n"), color = MaterialTheme.colorScheme.error) } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } })
}

@Composable
private fun DestinationDialog(kind: FileOperationKind, state: FileManagerUiState, onOpen: (File) -> Unit, onSelect: (File, CollisionPolicy) -> Unit, onDismiss: () -> Unit) {
    val destination = state.destinationDirectory
    var policy by remember { mutableStateOf(CollisionPolicy.KEEP_BOTH) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (kind == FileOperationKind.COPY) "Copy to" else "Move to") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(destination?.absolutePath ?: "Choose a folder", maxLines = 2)
            Text("If a name already exists", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CollisionPolicy.values().forEach { option ->
                    FilterChip(selected = policy == option, onClick = { policy = option }, label = { Text(option.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }) })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { destination?.parentFile?.let(onOpen) }) { Text("Up") }
                TextButton(onClick = { state.volumes.firstOrNull()?.root?.let(onOpen) }) { Text("Root") }
            }
            LazyColumn(Modifier.height(220.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.destinationEntries.filter { it.isDirectory }, key = { it.path }) { entry -> TextButton(onClick = { onOpen(File(entry.path)) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Folder, null); Spacer(Modifier.size(8.dp)); Text(entry.name) } }
            }
        }
    }, confirmButton = { TextButton(onClick = { destination?.let { onSelect(it, policy) } }, enabled = destination != null) { Text("Select this folder") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
