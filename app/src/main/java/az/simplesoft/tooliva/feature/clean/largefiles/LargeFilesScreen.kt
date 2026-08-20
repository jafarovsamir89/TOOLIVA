package az.simplesoft.tooliva.feature.clean.largefiles

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.text.format.Formatter
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.io.File
import az.simplesoft.tooliva.core.media.LargeMediaFile
import az.simplesoft.tooliva.core.media.MediaThumbnailLoader
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.hasRequiredMediaPermissions
import az.simplesoft.tooliva.core.media.requiredMediaPermissions
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.tryShare
import az.simplesoft.tooliva.core.storage.StorageFileActions
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import az.simplesoft.tooliva.feature.clean.StorageAccessCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun LargeFilesRoute(onOpenInFiles: (String) -> Unit = {}, viewModel: LargeFilesViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    var hasMediaAccess by remember { mutableStateOf(hasRequiredMediaPermissions(context)) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var accessActionError by remember { mutableStateOf<String?>(null) }
    var detailsFile by remember { mutableStateOf<LargeMediaFile?>(null) }
    val deleteCoordinator = remember(context) { MediaStoreDeleteCoordinator(context) }
    val fullMode = state.accessState.mode == StorageAccessMode.FULL
    val directDelete = fullMode

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasMediaAccess = hasRequiredMediaPermissions(context)
        if (hasMediaAccess || fullMode) viewModel.scan()
    }
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshAccess()
        hasMediaAccess = hasRequiredMediaPermissions(context)
    }

    val cleanupResult = state.cleanupResult
    if (cleanupResult != null) {
        BackHandler { viewModel.dismissCleanupResult() }
        CleanupResultScreen(result = cleanupResult, onDone = viewModel::dismissCleanupResult)
        return
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(if (directDelete) "Delete selected files permanently?" else "Move selected files to Trash?") },
            text = {
                Text(
                    "${state.selectedFiles.size} item(s), ${Formatter.formatFileSize(context, state.selectedBytes)} selected. " +
                        if (directDelete) {
                            "Full Storage Mode will delete these shared-storage files after this confirmation. Nothing changes if you cancel."
                        } else {
                            "Android will ask for final Trash confirmation. Nothing changes if you cancel."
                        },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.requestDelete(deleteCoordinator, directDelete = directDelete)
                    },
                    enabled = state.selectedFiles.isNotEmpty(),
                ) { Text(if (directDelete) "Delete" else "Continue") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } },
        )
    }

    detailsFile?.let { file ->
        AlertDialog(
            onDismissRequest = { detailsFile = null },
            title = { Text("File details") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(file.displayName, fontWeight = FontWeight.Bold)
                    Text("Type: ${file.mimeType ?: file.category.name}")
                    Text("Size: ${Formatter.formatFileSize(context, file.sizeBytes)}")
                    Text("Path: ${file.path ?: "Unavailable"}")
                }
            },
            confirmButton = { TextButton(onClick = { detailsFile = null }) { Text("Done") } },
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = 16.dp,
                end = 16.dp,
                bottom = if (state.selectedFiles.isNotEmpty()) 104.dp else 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Large files", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(
                        "Review every accessible shared-storage file larger than 100 MB. Nothing is deleted automatically.",
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

            if (!fullMode && !hasMediaAccess) {
                item {
                    Card(
                        shape = ToolivaShapes.hero,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                            Text("Limited media access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                "Without Full Storage Access, Tooliva can only scan media that Android exposes through MediaStore.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Button(onClick = { permissionLauncher.launch(requiredMediaPermissions()) }) { Text("Allow media access") }
                        }
                    }
                }
            } else {
                item {
                    Card(
                        shape = ToolivaShapes.hero,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${state.visibleFiles.size} matching files", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    Formatter.formatFileSize(context, state.visibleFiles.sumOf { it.sizeBytes }),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                if (state.isLoading) Text("Visited ${state.visitedFiles} files", style = MaterialTheme.typography.bodySmall)
                            }
                            if (state.isLoading) CircularProgressIndicator()
                            if (state.visibleFiles.isNotEmpty()) {
                                TextButton(onClick = viewModel::toggleSelectAllVisible) {
                                    Text(if (state.allVisibleSelected) "Clear all" else "Select all")
                                }
                            }
                        }
                    }
                }

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
                        placeholder = { Text("Search name or path") },
                    )
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(thresholdOptions) { option ->
                            FilterChip(
                                selected = state.thresholdBytes == option.bytes,
                                onClick = { viewModel.setThreshold(option.bytes) },
                                label = { Text(option.label) },
                            )
                        }
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(categoryOptions) { category ->
                            FilterChip(
                                selected = state.categoryFilter == category,
                                onClick = { viewModel.setCategory(category) },
                                label = { Text(categoryLabel(category)) },
                            )
                        }
                    }
                }

                item {
                    Box {
                        OutlinedButton(onClick = { showSortMenu = true }) { Text("Sort: ${sortLabel(state.sortOrder)}") }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            StorageSortOrder.entries.forEach { order ->
                                DropdownMenuItem(
                                    text = { Text(sortLabel(order)) },
                                    onClick = {
                                        showSortMenu = false
                                        viewModel.setSortOrder(order)
                                    },
                                )
                            }
                        }
                    }
                }

                state.errorMessage?.let { message ->
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Scan needs attention", fontWeight = FontWeight.Bold)
                                Text(message)
                                Button(onClick = viewModel::scan, modifier = Modifier.padding(top = 8.dp)) { Text("Try again") }
                            }
                        }
                    }
                }

                if (state.isLoading) {
                    item {
                        OutlinedButton(onClick = viewModel::cancelScan, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Cancel, contentDescription = null)
                            Text("Cancel scan", modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }

                if (!state.isLoading && state.errorMessage == null && state.visibleFiles.isEmpty()) {
                    item {
                        Card(
                            shape = ToolivaShapes.hero,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Find large files", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "Scan accessible storage for files larger than ${Formatter.formatFileSize(context, state.thresholdBytes)}. Nothing is deleted automatically.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Button(onClick = viewModel::scan) { Text("Scan large files") }
                            }
                        }
                    }
                }

                items(state.visibleFiles, key = { it.uri.toString() }) { file -> LargeFileCard(file, state.selectedUris, viewModel, onOpenInFiles, { detailsFile = file }) }
            }
        }

        if (state.selectedFiles.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { shareSelected(context, state.selectedFiles, viewModel) }, enabled = !state.isPreparingDelete, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Share, contentDescription = null)
                        Text("Share", modifier = Modifier.padding(start = 6.dp))
                    }
                    Button(onClick = {
                        if (directDelete || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) showDeleteConfirmation = true
                        else viewModel.requestDelete(deleteCoordinator, directDelete = false)
                    }, enabled = !state.isPreparingDelete, modifier = Modifier.weight(1.6f)) {
                        if (state.isPreparingDelete) { CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp); Text("Preparing…") }
                        else { Icon(Icons.Outlined.Delete, contentDescription = null); Text("${if (directDelete) "Delete" else "Move"} ${state.selectedFiles.size} · ${Formatter.formatFileSize(context, state.selectedBytes)}", modifier = Modifier.padding(start = 6.dp)) }
                    }
                }
            }
        }
    }
}
@Composable
private fun LargeFileCard(file: LargeMediaFile, selectedUris: Set<String>, viewModel: LargeFilesViewModel, onOpenInFiles: (String) -> Unit, onDetails: () -> Unit) {
    val context = LocalContext.current
    val selected = file.uri.toString() in selectedUris
    Card(
        onClick = { viewModel.toggleSelection(file.uri.toString()) },
        shape = ToolivaShapes.large,
        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = selected, onCheckedChange = { viewModel.toggleSelection(file.uri.toString()) })
            LargeFileVisual(file)
            Column(modifier = Modifier.weight(1f)) {
                Text(file.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Text(categoryLabel(file.category), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                Text(file.path ?: file.mimeType ?: "Accessible file", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            Text(Formatter.formatFileSize(context, file.sizeBytes), fontWeight = FontWeight.Bold)
            IconButton(onClick = { context.tryShare(file.asStorageEntry())?.let(viewModel::showError) }) {
                Icon(Icons.Outlined.Share, contentDescription = "Share ${file.displayName}")
            }
            IconButton(onClick = onDetails) { Icon(Icons.Outlined.Info, contentDescription = "Details for ${file.displayName}") }
            IconButton(onClick = {
                val openUri = if (file.uri.scheme == "file") {
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(file.uri.path.orEmpty()))
                } else file.uri
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(openUri, file.mimeType ?: "*/*")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                try {
                    context.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    viewModel.showError("No app can open this file.")
                } catch (_: IllegalArgumentException) {
                    viewModel.showError("This file cannot be opened from its current location.")
                }
            }) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open ${file.displayName}") }
            IconButton(onClick = { file.path?.let { onOpenInFiles(File(it).parent ?: it) } }) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = "Show ${file.displayName} in Files")
            }
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun LargeFileVisual(file: LargeMediaFile) {
    val context = LocalContext.current
    val bitmap by androidx.compose.runtime.produceState<android.graphics.Bitmap?>(initialValue = null, key1 = file.uri.toString()) {
        if (file.category == StorageCategory.IMAGE || file.category == StorageCategory.VIDEO) {
            value = withContext(Dispatchers.IO) { MediaThumbnailLoader.load(context, file.uri) }
        }
    }
    if (bitmap != null) {
        Image(bitmap = bitmap!!.asImageBitmap(), contentDescription = file.displayName, modifier = Modifier.size(52.dp), contentScale = ContentScale.Crop)
    } else {
        Icon(categoryIcon(file.category), contentDescription = null, modifier = Modifier.padding(4.dp))
    }
}

private data class ThresholdOption(val label: String, val bytes: Long)

private val thresholdOptions = listOf(
    ThresholdOption("100 MB+", LargeFilesUiState.MIN_LARGE_FILE_BYTES),
    ThresholdOption("500 MB+", 500L * 1024L * 1024L),
    ThresholdOption("1 GB+", 1024L * 1024L * 1024L),
)

private val categoryOptions = listOf(
    StorageCategory.ALL,
    StorageCategory.VIDEO,
    StorageCategory.IMAGE,
    StorageCategory.AUDIO,
    StorageCategory.APK,
    StorageCategory.ARCHIVE,
    StorageCategory.DOCUMENT,
    StorageCategory.DOWNLOAD,
    StorageCategory.OTHER,
)

private fun categoryLabel(category: StorageCategory): String = when (category) {
    StorageCategory.ALL -> "All"
    StorageCategory.VIDEO -> "Video"
    StorageCategory.IMAGE -> "Image"
    StorageCategory.AUDIO -> "Audio"
    StorageCategory.APK -> "APK"
    StorageCategory.ARCHIVE -> "Archives"
    StorageCategory.DOCUMENT -> "Documents"
    StorageCategory.DOWNLOAD -> "Downloads"
    StorageCategory.OTHER -> "Other"
}

private fun sortLabel(order: StorageSortOrder): String = when (order) {
    StorageSortOrder.SIZE -> "Largest first"
    StorageSortOrder.NEWEST -> "Newest"
    StorageSortOrder.OLDEST -> "Oldest"
    StorageSortOrder.NAME -> "Name"
}

private fun LargeMediaFile.asStorageEntry() = StorageEntry(
    ref = uri,
    name = displayName,
    path = path ?: uri.toString(),
    category = category,
    sizeBytes = sizeBytes,
    modifiedAtMillis = modifiedEpochSeconds * 1_000L,
    mimeType = mimeType,
    extension = displayName.substringAfterLast('.', "").lowercase().takeIf { it.isNotBlank() },
)

private fun shareSelected(context: android.content.Context, files: List<LargeMediaFile>, viewModel: LargeFilesViewModel) {
    try {
        val uris = files.map { StorageFileActions.shareableUri(context, it.uri) }
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "*/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share selected files"))
    } catch (_: ActivityNotFoundException) {
        viewModel.showError("No app can share the selected files.")
    } catch (_: IllegalArgumentException) {
        viewModel.showError("Some selected files cannot be shared from their current location.")
    }
}

private fun categoryIcon(category: StorageCategory): ImageVector = when (category) {
    StorageCategory.VIDEO -> Icons.Outlined.Movie
    StorageCategory.IMAGE -> Icons.Outlined.Image
    StorageCategory.AUDIO -> Icons.Outlined.AudioFile
    StorageCategory.APK -> Icons.Outlined.InsertDriveFile
    StorageCategory.ARCHIVE -> Icons.Outlined.Archive
    StorageCategory.DOCUMENT -> Icons.Outlined.Description
    StorageCategory.DOWNLOAD -> Icons.Outlined.Download
    StorageCategory.ALL, StorageCategory.OTHER -> Icons.Outlined.InsertDriveFile
}
