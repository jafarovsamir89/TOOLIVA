package az.simplesoft.tooliva.feature.clean.emptyfolders

import android.content.ActivityNotFoundException
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.feature.clean.StorageAccessCard
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen

@Composable
fun EmptyFoldersRoute(viewModel: EmptyFoldersViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val access = remember(context) { StorageAccessCoordinator(context) }
    val coordinator = remember(context) { MediaStoreDeleteCoordinator(context) }
    var accessError by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }
    state.cleanupResult?.let { result ->
        BackHandler { viewModel.dismissResult() }
        CleanupResultScreen(result = result, onDone = viewModel::dismissResult)
        return
    }
    if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete empty folders?") }, text = { Text("${state.selected.size} folder(s) selected. Tooliva re-checks that each is still empty. Nothing changes if you cancel.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteSelected(coordinator) }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (state.selected.isNotEmpty()) 100.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Column(modifier = Modifier.padding(vertical = 8.dp)) { Text("Empty folders", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("Only safe, accessible folders that are empty at scan and deletion time are listed.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            item { StorageAccessCard(fullMode = state.accessState.mode == StorageAccessMode.FULL, supported = state.accessState.fullStorageSupported, errorMessage = accessError, onEnableFull = { try { access.allFilesSettingsIntent()?.let(context::startActivity) } catch (_: ActivityNotFoundException) { accessError = "Android did not provide the Full Storage Access settings screen." } }) }
            if (state.accessState.mode != StorageAccessMode.FULL) item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(modifier = Modifier.padding(20.dp)) { Text("Full Storage Access required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Limited Mode cannot safely enumerate shared-storage folders.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            else {
                item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) { Button(onClick = viewModel::scan, enabled = !state.isLoading) { if (state.isLoading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp); Text(if (state.isLoading) "Scanning…" else "Scan empty folders") }; if (state.isLoading) OutlinedButton(onClick = viewModel::cancelScan) { Icon(Icons.Outlined.Cancel, contentDescription = null); Text("Cancel", modifier = Modifier.padding(start = 6.dp)) } } }
                item { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("${state.folders.size} safe empty folder(s)"); TextButton(onClick = viewModel::selectAll, enabled = state.folders.isNotEmpty()) { Text(if (state.allSelected) "Clear all" else "Select all") } } }
                if (!state.hasAnalyzed) item { Text("Run a scan to review folders. Root, protected Android and Tooliva folders are excluded.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (state.hasAnalyzed && !state.isLoading && state.folders.isEmpty()) item { Text("No safe empty folders found.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                items(state.folders, key = { it.path }) { folder ->
                    val selected = folder.path in state.selectedPaths
                    Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = selected, onCheckedChange = { viewModel.toggle(folder.path) })
                            Icon(Icons.Outlined.FolderOpen, contentDescription = null, modifier = Modifier.padding(horizontal = 8.dp))
                            Column(modifier = Modifier.weight(1f)) { Text(folder.path.substringAfterLast('/'), fontWeight = FontWeight.SemiBold); Text(folder.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2) }
                        }
                    }
                }
            }
        }
        if (state.selected.isNotEmpty()) Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), tonalElevation = 6.dp) { Button(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().padding(12.dp)) { Icon(Icons.Outlined.Delete, contentDescription = null); Text("Delete ${state.selected.size} empty folder(s)", modifier = Modifier.padding(start = 8.dp)) } }
    }
}
