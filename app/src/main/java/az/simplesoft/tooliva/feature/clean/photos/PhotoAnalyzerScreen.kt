package az.simplesoft.tooliva.feature.clean.photos

import android.app.Activity
import android.graphics.Bitmap
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import az.simplesoft.tooliva.ui.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.MediaThumbnailLoader
import az.simplesoft.tooliva.core.media.hasRequiredMediaPermissions
import az.simplesoft.tooliva.core.media.requiredMediaPermissions
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PhotoAnalyzerRoute(viewModel: PhotoAnalyzerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val coordinator = remember(context) { MediaStoreDeleteCoordinator(context) }
    val accessCoordinator = remember(context) { StorageAccessCoordinator(context) }
    var hasAccess by remember { mutableStateOf(accessCoordinator.currentState().mode == StorageAccessMode.FULL || hasRequiredMediaPermissions(context)) }
    var confirmDelete by remember { mutableStateOf(false) }
    val deleteLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onSystemDeleteResult(result.resultCode == Activity.RESULT_OK, coordinator)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        hasAccess = accessCoordinator.currentState().mode == StorageAccessMode.FULL || hasRequiredMediaPermissions(context)
    }
    LaunchedEffect(state.pendingDelete?.requestId) {
        state.pendingDelete?.let { deleteLauncher.launch(IntentSenderRequest.Builder(it.intentSender).build()) }
    }
    if (state.cleanupResult != null) {
        BackHandler { viewModel.dismissCleanupResult() }
        CleanupResultScreen(result = state.cleanupResult!!, onDone = viewModel::dismissCleanupResult)
        return
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Review media cleanup") },
            text = { Text("${state.selectedItems.size} item(s), ${Formatter.formatFileSize(context, state.selectedBytes)} selected. Nothing changes if you cancel.") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.requestDelete(coordinator) }) { Text("Continue") } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    PhotoAnalyzerScreen(state, hasAccess, { permissionLauncher.launch(requiredMediaPermissions()) }, viewModel::scan, viewModel::cancelScan, viewModel::setFilter, viewModel::toggleSelection, viewModel::toggleSelectAll, viewModel::showPreview, { confirmDelete = true })
    state.previewItem?.let { item -> PhotoPreviewDialog(item, onDismiss = viewModel::dismissPreview) }
}

@Composable
private fun PhotoAnalyzerScreen(
    state: PhotoAnalyzerUiState,
    hasAccess: Boolean,
    onRequestAccess: () -> Unit,
    onScan: () -> Unit,
    onCancel: () -> Unit,
    onFilter: (PhotoAnalysisKind?) -> Unit,
    onToggle: (PhotoAnalysisItem) -> Unit,
    onSelectAll: () -> Unit,
    onPreview: (PhotoAnalysisItem) -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Photo Analyzer", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("On-device review. Nothing is selected or deleted automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = state.filter == null, onClick = { onFilter(null) }, label = { Text("All") })
                PhotoAnalysisKind.entries.forEach { kind -> FilterChip(selected = state.filter == kind, onClick = { onFilter(kind) }, label = { Text(kind.title) }) }
            }
            if (!hasAccess) {
                Text("Photo Analyzer needs access to the media you choose to review. Full Storage Mode avoids a redundant media permission.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = onRequestAccess, modifier = Modifier.fillMaxWidth()) { Text("Grant media access") }
            } else if (!state.isLoading) Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) { Text(if (state.items.isEmpty()) "Analyze media" else "Analyze again") }
            else {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Text("${state.progress.checked} checked · ${state.progress.candidates} review candidates"); TextButton(onClick = onCancel) { Text("Cancel") } }
            }
            if (state.visibleItems.isNotEmpty()) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${state.visibleItems.size} candidates", fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    TextButton(onClick = onSelectAll) { Icon(Icons.Outlined.SelectAll, null); Text(if (state.allSelected) "Clear" else "Select all") }
                }
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        if (state.visibleItems.isEmpty() && !state.isLoading) {
            Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.Image, null); Text("No review candidates yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Run an analysis to find possible issues locally.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = if (state.selectedItems.isEmpty()) 16.dp else 96.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(state.visibleItems, key = { "${it.uri}:${it.kind}" }) { item -> PhotoCandidateCard(item, item.uri.toString() in state.selectedUris, onToggle, onPreview, context) }
            }
        }
    }
    if (state.selectedItems.isNotEmpty()) {
        Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${state.selectedItems.size} · ${Formatter.formatFileSize(context, state.selectedBytes)}", Modifier.weight(1f), fontWeight = FontWeight.Bold)
            OutlinedButton(onClick = onDelete) { Icon(Icons.Outlined.Delete, null); Text("Trash") }
        }
    }
}

@Composable
private fun PhotoCandidateCard(item: PhotoAnalysisItem, selected: Boolean, onToggle: (PhotoAnalysisItem) -> Unit, onPreview: (PhotoAnalysisItem) -> Unit, context: android.content.Context) {
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = item.uri) { value = withContext(Dispatchers.IO) { MediaThumbnailLoader.load(context, item.uri) } }
    Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.clickable { onPreview(item) }) {
            Box(Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)), contentAlignment = Alignment.TopEnd) {
                if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = item.displayName, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                else Icon(Icons.Outlined.Image, null, Modifier.align(Alignment.Center))
                Checkbox(checked = selected, onCheckedChange = { onToggle(item) })
            }
            Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.displayName, maxLines = 1, fontWeight = FontWeight.SemiBold)
                Text(item.kind.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(Formatter.formatFileSize(context, item.sizeBytes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PhotoPreviewDialog(item: PhotoAnalysisItem, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = item.uri) { value = withContext(Dispatchers.IO) { MediaThumbnailLoader.load(context, item.uri) } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text(item.displayName) }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { if (bitmap != null) Image(bitmap!!.asImageBitmap(), item.displayName, Modifier.fillMaxWidth().aspectRatio(1f), contentScale = ContentScale.Fit); Text("${item.kind.title} · ${Formatter.formatFileSize(context, item.sizeBytes)}") } }, confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } })
}
