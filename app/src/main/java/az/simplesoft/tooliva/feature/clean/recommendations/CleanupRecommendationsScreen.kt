package az.simplesoft.tooliva.feature.clean.recommendations

import android.app.Activity
import android.content.ActivityNotFoundException
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
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
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
import az.simplesoft.tooliva.core.cleanup.CleanupCandidate
import az.simplesoft.tooliva.core.cleanup.CleanupReasonId
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.tryOpen
import az.simplesoft.tooliva.core.storage.tryShare
import az.simplesoft.tooliva.feature.clean.StorageAccessCard
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import java.util.Date

@Composable
fun CleanupRecommendationsRoute(viewModel: CleanupRecommendationsViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    val deleteCoordinator = remember(context) { MediaStoreDeleteCoordinator(context) }
    val fullMode = state.accessState.mode == StorageAccessMode.FULL
    var accessActionError by remember { mutableStateOf<String?>(null) }
    var actionError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var whyCandidate by remember { mutableStateOf<CleanupCandidate?>(null) }
    var detailsCandidate by remember { mutableStateOf<CleanupCandidate?>(null) }

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

    state.cleanupResult?.let { result ->
        BackHandler { viewModel.dismissCleanupResult() }
        CleanupResultScreen(result = result, onDone = viewModel::dismissCleanupResult)
        return
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete selected review files?") },
            text = {
                Text(
                    "${state.selectedCandidates.size} item(s), ${Formatter.formatFileSize(context, state.selectedBytes)} selected. " +
                        "These are only recommendations; nothing is changed if you cancel.",
                )
            },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirmation = false; viewModel.requestDelete(deleteCoordinator) }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") } },
        )
    }

    whyCandidate?.let { candidate ->
        AlertDialog(
            onDismissRequest = { whyCandidate = null },
            title = { Text("Why is this shown?") },
            text = { Text(candidate.reason.explanation) },
            confirmButton = { TextButton(onClick = { whyCandidate = null }) { Text("Got it") } },
        )
    }

    detailsCandidate?.let { candidate ->
        RecommendationDetailsDialog(candidate = candidate, onDismiss = { detailsCandidate = null })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (state.selectedCandidates.isNotEmpty()) 104.dp else 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Text("Files to review", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Old installers and downloads you may no longer need", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Outlined.Download, contentDescription = null)
                            Text("Full Storage Access required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text("Automatic recommendations need the Downloads folders. Limited Mode does not pretend to cover the whole folder.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    RecommendationSummaryCard(state = state, context = context, onReason = viewModel::setReasonFilter)
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = viewModel::analyze, enabled = !state.isLoading && !state.isPreparingDelete) {
                            if (state.isLoading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                            Text(if (state.isLoading) "Analyzing…" else "Analyze")
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
                            placeholder = { Text("Search name or path") },
                        )
                    }
                    item { RecommendationFilters(state, viewModel) }
                    item {
                        Box {
                            OutlinedButton(onClick = { showSortMenu = true }) { Text("Sort: ${recommendationSortLabel(state.sortOrder)}") }
                            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                                RecommendationSortOrder.entries.forEach { order ->
                                    DropdownMenuItem(text = { Text(recommendationSortLabel(order)) }, onClick = { showSortMenu = false; viewModel.setSortOrder(order) })
                                }
                            }
                        }
                    }
                    state.errorMessage?.let { message ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Text(message, modifier = Modifier.padding(16.dp)) }
                        }
                    }
                    if (state.visibleCandidates.isNotEmpty()) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${state.visibleCandidates.size} files · ${Formatter.formatFileSize(context, state.visibleCandidates.sumOf { it.entry.sizeBytes })}", fontWeight = FontWeight.Bold)
                                TextButton(onClick = viewModel::toggleSelectAllVisible) { Text(if (state.allVisibleSelected) "Clear all" else "Select all") }
                            }
                        }
                    }
                    if (!state.isLoading && state.visibleCandidates.isEmpty()) {
                        item {
                            Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("No files to review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                    Text("Tooliva only shows files matching the two explainable rules.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    items(state.visibleCandidates, key = { it.entry.ref.toString() }) { candidate ->
                        RecommendationCard(
                            candidate = candidate,
                            selected = candidate.entry.ref.toString() in state.selectedRefs,
                            onToggle = { viewModel.toggleSelection(candidate.entry.ref.toString()) },
                            onWhy = { whyCandidate = candidate },
                            onOpen = { actionError = context.tryOpen(candidate.entry) },
                            onShare = { actionError = context.tryShare(candidate.entry) },
                            onDetails = { detailsCandidate = candidate },
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

        if (state.selectedCandidates.isNotEmpty()) {
            Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
                Button(onClick = { showDeleteConfirmation = true }, enabled = !state.isPreparingDelete, modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                    if (state.isPreparingDelete) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                    Icon(Icons.Outlined.Delete, contentDescription = null)
                    Text("Delete ${state.selectedCandidates.size} · ${Formatter.formatFileSize(context, state.selectedBytes)}", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun RecommendationSummaryCard(
    state: CleanupRecommendationsUiState,
    context: android.content.Context,
    onReason: (CleanupReasonId?) -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("${Formatter.formatFileSize(context, state.candidates.sumOf { it.entry.sizeBytes })} ready for review", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Nothing is selected automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.summaries.filter { it.count > 0 }, key = { it.reasonId }) { summary ->
                    Card(onClick = { onReason(summary.reasonId) }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(summary.title, fontWeight = FontWeight.SemiBold)
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
private fun RecommendationFilters(state: CleanupRecommendationsUiState, viewModel: CleanupRecommendationsViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item { FilterChip(selected = state.reasonFilter == null, onClick = { viewModel.setReasonFilter(null) }, label = { Text("All") }) }
            item { FilterChip(selected = state.reasonFilter == CleanupReasonId.OLD_APK_INSTALLER, onClick = { viewModel.setReasonFilter(CleanupReasonId.OLD_APK_INSTALLER) }, label = { Text("APK installers") }) }
            item { FilterChip(selected = state.reasonFilter == CleanupReasonId.OLD_DOWNLOAD, onClick = { viewModel.setReasonFilter(CleanupReasonId.OLD_DOWNLOAD) }, label = { Text("Old Downloads") }) }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(recommendationAgeFilters, key = { it.days }) { filter ->
                FilterChip(selected = state.ageFilter == filter, onClick = { viewModel.setAgeFilter(filter) }, label = { Text(filter.label) })
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    candidate: CleanupCandidate,
    selected: Boolean,
    onToggle: () -> Unit,
    onWhy: () -> Unit,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onDetails: () -> Unit,
) {
    Card(onClick = onToggle, shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Checkbox(checked = selected, onCheckedChange = { onToggle() })
                Icon(recommendationIcon(candidate.entry), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(candidate.entry.name.ifBlank { "Unnamed file" }, fontWeight = FontWeight.Bold, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Text(candidate.reason.title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(candidate.entry.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(Formatter.formatFileSize(LocalContext.current, candidate.entry.sizeBytes), fontWeight = FontWeight.SemiBold)
                    Text(DateFormat.getDateFormat(LocalContext.current).format(Date(candidate.entry.modifiedAtMillis)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onWhy, contentPadding = PaddingValues(horizontal = 4.dp)) { Text("Why") }
                    TextButton(onClick = onOpen, contentPadding = PaddingValues(horizontal = 4.dp)) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = null); Text("Open", Modifier.padding(start = 4.dp)) }
                    IconButton(onClick = onShare) { Icon(Icons.Outlined.Share, contentDescription = "Share ${candidate.entry.name}") }
                    IconButton(onClick = onDetails) { Icon(Icons.Outlined.Info, contentDescription = "Details for ${candidate.entry.name}") }
                }
            }
        }
    }
}

@Composable
private fun RecommendationDetailsDialog(candidate: CleanupCandidate, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("File details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(candidate.entry.name, fontWeight = FontWeight.Bold)
                Text("Reason: ${candidate.reason.title}")
                Text("Size: ${Formatter.formatFileSize(context, candidate.entry.sizeBytes)}")
                Text("Modified: ${DateFormat.getDateFormat(context).format(Date(candidate.entry.modifiedAtMillis))}")
                Text("Path: ${candidate.entry.path}")
                Text(candidate.reason.explanation, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

private val recommendationAgeFilters = listOf(
    RecommendationAgeFilter(30, "30+ days"),
    RecommendationAgeFilter(90, "90+ days"),
    RecommendationAgeFilter(180, "180+ days"),
    RecommendationAgeFilter(365, "365+ days"),
)

private fun recommendationSortLabel(order: RecommendationSortOrder): String = when (order) {
    RecommendationSortOrder.OLDEST -> "Oldest"
    RecommendationSortOrder.NEWEST -> "Newest"
    RecommendationSortOrder.LARGEST -> "Largest"
    RecommendationSortOrder.SMALLEST -> "Smallest"
    RecommendationSortOrder.NAME -> "Name"
}

private fun recommendationIcon(entry: StorageEntry): ImageVector = when (entry.category) {
    StorageCategory.APK -> Icons.AutoMirrored.Outlined.InsertDriveFile
    StorageCategory.ARCHIVE -> Icons.Outlined.Archive
    StorageCategory.DOCUMENT -> Icons.Outlined.Description
    StorageCategory.DOWNLOAD, StorageCategory.OTHER -> Icons.Outlined.Download
    StorageCategory.VIDEO, StorageCategory.IMAGE, StorageCategory.AUDIO, StorageCategory.ALL -> Icons.Outlined.Download
}
