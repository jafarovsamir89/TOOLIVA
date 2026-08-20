@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package az.simplesoft.tooliva.feature.storage

import android.content.ActivityNotFoundException
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.List
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storage
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageMapNode
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import java.io.File

@Composable
fun StorageMapRoute(
    onBack: () -> Unit,
    onOpenInFiles: (File) -> Unit,
    viewModel: StorageMapViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    var accessError by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }

    BackHandler {
        if (state.cleanupResult != null) viewModel.dismissCleanupResult()
        else if (state.currentPath != null) viewModel.goUp()
        else onBack()
    }

    val cleanupResult = state.cleanupResult
    if (cleanupResult != null) {
        CleanupResultScreen(result = cleanupResult, onDone = viewModel::dismissCleanupResult)
        return
    }

    StorageMapScreen(
        state = state,
        onBack = onBack,
        onAnalyze = viewModel::analyze,
        onCancel = viewModel::cancelAnalyze,
        onRefreshAccess = viewModel::refreshAccess,
        onOpenSettings = {
            try {
                accessCoordinator.allFilesSettingsIntent()?.let(context::startActivity)
                    ?: run { accessError = "Full Storage Access is not available on this Android version." }
            } catch (_: ActivityNotFoundException) { accessError = "Android did not provide the storage settings screen." }
        },
        onSetView = viewModel::setView,
        onOpenNode = viewModel::openNode,
        onGoUp = { viewModel.goUp() },
        onShowDetails = viewModel::showDetails,
        onDismissDetails = viewModel::dismissDetails,
        onRequestDelete = viewModel::requestDelete,
        onDismissDelete = viewModel::dismissDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onOpenInFiles = onOpenInFiles,
        accessError = accessError,
    )
}

@Composable
private fun StorageMapScreen(
    state: StorageMapUiState,
    onBack: () -> Unit,
    onAnalyze: () -> Unit,
    onCancel: () -> Unit,
    onRefreshAccess: () -> Unit,
    onOpenSettings: () -> Unit,
    onSetView: (StorageMapView) -> Unit,
    onOpenNode: (StorageMapNode) -> Unit,
    onGoUp: () -> Unit,
    onShowDetails: (StorageMapNode) -> Unit,
    onDismissDetails: () -> Unit,
    onRequestDelete: (StorageMapNode) -> Unit,
    onDismissDelete: () -> Unit,
    onConfirmDelete: (StorageMapNode) -> Unit,
    onOpenInFiles: (File) -> Unit,
    accessError: String?,
) {
    val context = LocalContext.current
    val result = state.result
    val currentNode = result?.find(state.currentPath.orEmpty())
    val nodes = currentNode?.children ?: result?.roots.orEmpty()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage Map", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
                actions = { if (state.phase == StorageMapPhase.COMPLETE) IconButton(onClick = onAnalyze) { Icon(Icons.Outlined.Refresh, "Analyze again") } },
            )
        },
    ) { innerPadding ->
        LazyColumn(Modifier.fillMaxSize().padding(innerPadding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("See which folders use the most storage.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Analysis starts only when you tap Analyze storage. Files are not opened, hashed or thumbnailed.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (state.accessState.mode != StorageAccessMode.FULL) {
                item {
                    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Full Storage Access is needed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Android limits direct folder aggregation until you allow access in system settings.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = onOpenSettings) { Text("Allow access") }
                            accessError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            if (state.phase == StorageMapPhase.IDLE) {
                item {
                    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Outlined.Map, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
                            Text("Ready to analyze", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Button(onClick = onAnalyze, enabled = state.accessState.mode == StorageAccessMode.FULL, modifier = Modifier.fillMaxWidth()) { Text("Analyze storage") }
                            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
            if (state.phase == StorageMapPhase.LOADING) {
                item {
                    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp); Text("Analyzing storage…", fontWeight = FontWeight.Bold) }
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text("${state.filesChecked} files checked · ${state.bytesCounted.formatBytes(context)} counted", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = onCancel) { Text("Cancel") }
                        }
                    }
                }
            }
            if (state.phase == StorageMapPhase.COMPLETE) {
                item { MapSummaryCard(state, context) }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = state.view == StorageMapView.MAP, onClick = { onSetView(StorageMapView.MAP) }, label = { Text("Map") }, leadingIcon = { Icon(Icons.Outlined.Map, null) })
                        FilterChip(selected = state.view == StorageMapView.LIST, onClick = { onSetView(StorageMapView.LIST) }, label = { Text("List") }, leadingIcon = { Icon(Icons.Outlined.List, null) })
                    }
                }
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Location:", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(currentNode?.path ?: "Storage roots", maxLines = 1)
                    }
                }
                if (currentNode != null) item { TextButton(onClick = onGoUp) { Icon(Icons.Outlined.ArrowBack, null); Text("Parent folder", Modifier.padding(start = 6.dp)) } }
                if (nodes.isEmpty()) item { Text("No accessible child folders were found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                else if (state.view == StorageMapView.MAP) item { Treemap(nodes, currentNode, context, onOpenNode) }
                else items(nodes, key = { it.path }) { node -> FolderRow(node, currentNode, context, onOpenNode, onShowDetails, onRequestDelete) }
                if (state.warningCount > 0) item { Text("${state.warningCount} protected or unreadable locations were skipped.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
                if (state.isStale) item { Text("This map may be stale after a file operation. Analyze again manually to refresh it.", color = MaterialTheme.colorScheme.tertiary) }
                item {
                    Text("Tap a folder to drill down. Use details or Open in Files for actions.", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
            state.errorMessage?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        }
    }

    state.detailsNode?.let { node ->
        AlertDialog(
            onDismissRequest = onDismissDetails,
            title = { Text(node.name) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text("Path: ${node.path}"); Text("Total: ${node.totalBytes.formatBytes(context)}"); Text("Files: ${node.fileCount}"); Text("Folders: ${node.folderCount}") } },
            confirmButton = { TextButton(onClick = onDismissDetails) { Text("Close") } },
            dismissButton = { TextButton(onClick = { onDismissDetails(); onOpenInFiles(File(node.path)) }) { Text("Open in Files") } },
        )
    }
    state.deleteNode?.let { node ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text("Delete folder?") },
            text = { Text("${node.name}\n${node.totalBytes.formatBytes(context)} · ${node.fileCount} files. This uses the existing file operation and cannot be undone from Tooliva.") },
            confirmButton = { TextButton(onClick = { onConfirmDelete(node) }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text("Cancel") } },
        )
    }
}

@Composable
private fun MapSummaryCard(state: StorageMapUiState, context: android.content.Context) {
    Card(shape = ToolivaShapes.hero, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(Icons.Outlined.Storage, null, tint = MaterialTheme.colorScheme.primary); Text("Storage analyzed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            Text("${state.bytesCounted.formatBytes(context)} counted in ${state.foldersFound} folders")
            Text("${state.filesChecked} files checked", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Treemap(nodes: List<StorageMapNode>, parent: StorageMapNode?, context: android.content.Context, onOpenNode: (StorageMapNode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        nodes.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { node ->
                    val weight = (node.totalBytes.toDouble() / (row.sumOf { it.totalBytes }.coerceAtLeast(1L))).toFloat().coerceAtLeast(0.2f)
                    Card(modifier = Modifier.weight(weight).height(92.dp).clickable { onOpenNode(node) }, shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) { Text(node.name, maxLines = 1, fontWeight = FontWeight.Bold); Text(node.totalBytes.formatBytes(context)); Text("${node.percentOf(parent)}%", style = MaterialTheme.typography.labelSmall) }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(0.2f))
            }
        }
    }
}

@Composable
private fun FolderRow(
    node: StorageMapNode,
    parent: StorageMapNode?,
    context: android.content.Context,
    onOpenNode: (StorageMapNode) -> Unit,
    onShowDetails: (StorageMapNode) -> Unit,
    onRequestDelete: (StorageMapNode) -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().clickable { onOpenNode(node) }, shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f)) { Text(node.name, fontWeight = FontWeight.Bold); Text("${node.fileCount} files · ${node.percentOf(parent)}% of parent", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) }
            Text(node.totalBytes.formatBytes(context), fontWeight = FontWeight.Bold)
            IconButton(onClick = { onShowDetails(node) }) { Icon(Icons.Outlined.Info, "Details") }
            IconButton(onClick = { onRequestDelete(node) }) { Icon(Icons.Outlined.DeleteOutline, "Delete") }
        }
    }
}

private fun Long.formatBytes(context: android.content.Context): String = Formatter.formatFileSize(context, coerceAtLeast(0L))
