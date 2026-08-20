@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package az.simplesoft.tooliva.feature.clean.swipe

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.media.MediaThumbnailLoader
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun CleanupSwipeRoute(
    onBack: () -> Unit,
    onOpenInFiles: (File) -> Unit,
    viewModel: CleanupSwipeViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accessCoordinator = remember(context) { az.simplesoft.tooliva.core.storage.StorageAccessCoordinator(context) }
    var accessError by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }

    BackHandler {
        when {
            state.cleanupResult != null -> viewModel.dismissCleanupResult()
            state.phase == CleanupSwipePhase.FINAL_REVIEW -> viewModel.backToReview()
            state.phase == CleanupSwipePhase.REVIEW -> viewModel.dismissCleanupResult()
            state.isLoading -> viewModel.cancelLoading()
            else -> onBack()
        }
    }

    state.cleanupResult?.let {
        CleanupResultScreen(result = it, onDone = viewModel::dismissCleanupResult)
        return
    }

    CleanupSwipeScreen(
        state = state,
        onBack = onBack,
        onLoad = viewModel::load,
        onCancelLoad = viewModel::cancelLoading,
        onSort = viewModel::setSort,
        onDecide = viewModel::decide,
        onUndo = viewModel::undo,
        onFinalReview = viewModel::openFinalReview,
        onBackToReview = viewModel::backToReview,
        onUnselect = viewModel::unselectForDelete,
        onShowDetails = viewModel::showDetails,
        onDismissDetails = viewModel::dismissDetails,
        onShowDeleteConfirmation = viewModel::showDeleteConfirmation,
        onDismissDeleteConfirmation = viewModel::dismissDeleteConfirmation,
        onConfirmDelete = viewModel::confirmDelete,
        onOpenInFiles = onOpenInFiles,
        context = context,
        onOpenStorageSettings = {
            try {
                accessCoordinator.allFilesSettingsIntent()?.let(context::startActivity)
                    ?: run { accessError = "Full Storage Access is not available on this Android version." }
            } catch (_: ActivityNotFoundException) {
                accessError = "Android did not provide the storage settings screen."
            }
        },
        accessError = accessError,
    )
}

@Composable
private fun CleanupSwipeScreen(
    state: CleanupSwipeSnapshot,
    onBack: () -> Unit,
    onLoad: (CleanupSwipeCategory) -> Unit,
    onCancelLoad: () -> Unit,
    onSort: (CleanupSwipeSort) -> Unit,
    onDecide: (SwipeDecision) -> Unit,
    onUndo: () -> Unit,
    onFinalReview: () -> Unit,
    onBackToReview: () -> Unit,
    onUnselect: (String) -> Unit,
    onShowDetails: (StorageEntry) -> Unit,
    onDismissDetails: () -> Unit,
    onShowDeleteConfirmation: () -> Unit,
    onDismissDeleteConfirmation: () -> Unit,
    onConfirmDelete: () -> Unit,
    onOpenInFiles: (File) -> Unit,
    context: android.content.Context,
    onOpenStorageSettings: () -> Unit,
    accessError: String?,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cleanup Swipe", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Review files one by one", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Nothing is deleted during review. Deletion starts only after the final confirmation.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (state.accessState.mode != StorageAccessMode.FULL) {
                item {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Full Storage Access is required", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Cleanup Swipe reviews local shared-storage files. Android access must be enabled before a category is loaded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = onOpenStorageSettings) { Text("Allow access") }
                            accessError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }

            if (state.phase == CleanupSwipePhase.PICKER) {
                item { CategoryPicker(state, onLoad) }
                state.errorMessage?.let { message -> item { ErrorCard(message) } }
            }

            if (state.phase == CleanupSwipePhase.LOADING) {
                item {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Text("Loading ${state.selectedCategory?.title ?: "files"}…", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text("${state.filesChecked} files checked", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = onCancelLoad) { Text("Cancel") }
                        }
                    }
                }
            }

            if (state.phase == CleanupSwipePhase.REVIEW) {
                item { SortBar(state.sort, onSort) }
                item {
                    ReviewSummary(state, context, onUndo, onFinalReview)
                }
                val session = state.session
                if (session?.current != null) {
                    item {
                        SwipeCard(session.current!!, state.selectedCategory, context, onDecide, onShowDetails)
                    }
                    item { Text("Swipe right to keep, left to delete, or up to skip. Buttons are always available below.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                } else {
                    item { EmptyReview(session?.category) }
                }
            }

            if (state.phase == CleanupSwipePhase.FINAL_REVIEW) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Final review", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text("Unselect anything you want to keep. The selected files below are the only files that can be deleted.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                state.session?.selectedDeleteEntries?.let { selected ->
                    if (selected.isEmpty()) item { EmptyDeleteSelection() }
                    else items(selected, key = { "selected-${it.path}" }) { entry ->
                        ReviewEntryRow(entry, SwipeDecision.DELETE, context, onUnselect, onShowDetails, onOpenInFiles)
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onBackToReview, modifier = Modifier.weight(1f)) { Text("Back") }
                        Button(onClick = onShowDeleteConfirmation, enabled = !state.session?.selectedDeleteEntries.isNullOrEmpty() && !state.isDeleting, modifier = Modifier.weight(1f)) {
                            Text(if (state.isDeleting) "Deleting…" else "Delete selected")
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                if (state.phase != CleanupSwipePhase.PICKER) item { ErrorCard(message) }
            }
        }
    }

    state.detailsEntry?.let { entry ->
        AlertDialog(
            onDismissRequest = onDismissDetails,
            title = { Text(entry.name) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(entry.path); Text(Formatter.formatFileSize(context, entry.sizeBytes)); entry.mimeType?.let { Text(it) } } },
            confirmButton = { TextButton(onClick = onDismissDetails) { Text("Close") } },
            dismissButton = { TextButton(onClick = { onDismissDetails(); onOpenInFiles(File(entry.path).parentFile ?: File(entry.path)) }) { Text("Open in Files") } },
        )
    }
    if (state.showFinalConfirmation) {
        val session = state.session
        AlertDialog(
            onDismissRequest = onDismissDeleteConfirmation,
            title = { Text("Delete selected files?") },
            text = { Text("${session?.selectedDeleteEntries?.size ?: 0} files · ${Formatter.formatFileSize(context, session?.selectedDeleteBytes ?: 0L)}. Tooliva will verify the result after the existing file operation completes.") },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onDismissDeleteConfirmation) { Text("Cancel") } },
        )
    }
}

@Composable
private fun CategoryPicker(state: CleanupSwipeSnapshot, onLoad: (CleanupSwipeCategory) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Choose a category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        CleanupSwipeCategory.entries.forEach { category ->
            Card(onClick = { onLoad(category) }, enabled = state.accessState.mode == StorageAccessMode.FULL, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Outlined.FolderOpen, null, tint = MaterialTheme.colorScheme.primary)
                    Column(Modifier.weight(1f)) { Text(category.title, fontWeight = FontWeight.Bold); Text(category.subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                }
            }
        }
    }
}

@Composable
private fun SortBar(sort: CleanupSwipeSort, onSort: (CleanupSwipeSort) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CleanupSwipeSort.entries.forEach { option -> FilterChip(selected = sort == option, onClick = { onSort(option) }, label = { Text(option.title) }) }
    }
}

@Composable
private fun ReviewSummary(state: CleanupSwipeSnapshot, context: android.content.Context, onUndo: () -> Unit, onFinalReview: () -> Unit) {
    val session = state.session
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("${session?.decidedCount ?: 0} of ${session?.entries?.size ?: 0} reviewed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Delete selection: ${session?.selectedDeleteEntries?.size ?: 0} · ${Formatter.formatFileSize(context, session?.selectedDeleteBytes ?: 0L)}", color = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onUndo, enabled = !session?.decisionHistory.isNullOrEmpty()) { Icon(Icons.Outlined.Undo, "Undo") }
            }
            if (session?.isComplete == true) Button(onClick = onFinalReview, modifier = Modifier.fillMaxWidth()) { Text("Open final review") }
        }
    }
}

@Composable
private fun SwipeCard(entry: StorageEntry, category: CleanupSwipeCategory?, context: android.content.Context, onDecide: (SwipeDecision) -> Unit, onShowDetails: (StorageEntry) -> Unit) {
    var dragAmount by remember(entry.path) { mutableFloatStateOf(0f) }
    Card(
        modifier = Modifier.fillMaxWidth().pointerInput(entry.path) {
            detectHorizontalDragGestures(
                onHorizontalDrag = { _, amount -> dragAmount += amount },
                onDragEnd = {
                    when {
                        dragAmount > 120f -> onDecide(SwipeDecision.KEEP)
                        dragAmount < -120f -> onDecide(SwipeDecision.DELETE)
                    }
                    dragAmount = 0f
                },
                onDragCancel = { dragAmount = 0f },
            )
        },
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (entry.category == az.simplesoft.tooliva.core.storage.StorageCategory.IMAGE) {
                SwipePreview(entry, context)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Description, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(entry.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(category?.title ?: "File", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                IconButton(onClick = { onShowDetails(entry) }) { Icon(Icons.Outlined.Info, "Details") }
            }
            Text(Formatter.formatFileSize(context, entry.sizeBytes), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
            Text(entry.path, maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(
                    onClick = { onDecide(SwipeDecision.SKIP) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) { Text("Skip", maxLines = 1, softWrap = false) }
                OutlinedButton(
                    onClick = { onDecide(SwipeDecision.KEEP) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) { Text("Keep", maxLines = 1, softWrap = false) }
                Button(
                    onClick = { onDecide(SwipeDecision.DELETE) },
                    modifier = Modifier.weight(1f).height(48.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Text("Delete", modifier = Modifier.padding(start = 3.dp), maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun SwipePreview(entry: StorageEntry, context: android.content.Context) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = entry.path) {
        value = withContext(Dispatchers.IO) { MediaThumbnailLoader.load(context, entry.ref) }
    }
    if (bitmap == null) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f)
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = "Preview unavailable", modifier = Modifier.size(42.dp))
        }
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = entry.name,
            modifier = Modifier.fillMaxWidth().aspectRatio(1.6f).clip(RoundedCornerShape(18.dp)),
            contentScale = ContentScale.Crop,
        )
    }
}

@Composable
private fun ReviewEntryRow(entry: StorageEntry, decision: SwipeDecision, context: android.content.Context, onUnselect: (String) -> Unit, onShowDetails: (StorageEntry) -> Unit, onOpenInFiles: (File) -> Unit) {
    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) { Text(entry.name, fontWeight = FontWeight.Bold); Text(Formatter.formatFileSize(context, entry.sizeBytes), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            IconButton(onClick = { onShowDetails(entry) }) { Icon(Icons.Outlined.Info, "Details") }
            IconButton(onClick = { onOpenInFiles(File(entry.path).parentFile ?: File(entry.path)) }) { Icon(Icons.Outlined.FolderOpen, "Open in Files") }
            TextButton(onClick = { onUnselect(entry.path) }) { Text(if (decision == SwipeDecision.DELETE) "Keep" else "Select") }
        }
    }
}

@Composable
private fun EmptyReview(category: CleanupSwipeCategory?) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Outlined.Info, null, modifier = Modifier.size(36.dp))
            Text("No ${category?.title?.lowercase() ?: "files"} found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Only files visible to Android and matching this category are shown.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyDeleteSelection() {
    Card(shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.WarningAmber, null)
            Text("No files are selected for deletion. Go back and choose Delete for a file.")
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer) }
}
