package az.simplesoft.tooliva.feature.clean.oldfiles

import android.content.ActivityNotFoundException
import android.text.format.Formatter
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
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
import az.simplesoft.tooliva.core.storage.tryOpen
import az.simplesoft.tooliva.feature.clean.StorageAccessCard
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen

private val oldAgeOptions = listOf(OldFilesAge(30, "30+ days"), OldFilesAge(90, "90+ days"), OldFilesAge(180, "180+ days"), OldFilesAge(365, "365+ days"))

@Composable
fun OldFilesRoute(viewModel: OldFilesViewModel = viewModel()) {
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
    if (confirmDelete) {
        AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Delete selected old files?") }, text = { Text("${state.selectedEntries.size} item(s), ${Formatter.formatFileSize(context, state.selectedBytes)} selected. Nothing changes if you cancel.") }, confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.deleteSelected(coordinator) }) { Text("Delete") } }, dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = if (state.selectedEntries.isNotEmpty()) 100.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Column(modifier = Modifier.padding(vertical = 8.dp)) { Text("Old files", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("Conservative age and scope filters. Nothing is selected automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            item { StorageAccessCard(fullMode = state.accessState.mode == StorageAccessMode.FULL, supported = state.accessState.fullStorageSupported, errorMessage = accessError, onEnableFull = { try { access.allFilesSettingsIntent()?.let(context::startActivity) } catch (_: ActivityNotFoundException) { accessError = "Android did not provide the Full Storage Access settings screen." } }) }
            if (state.accessState.mode != StorageAccessMode.FULL) {
                item { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Full Storage Access required", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text("Old Files is limited to shared-storage paths that Tooliva can genuinely inspect.", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
            } else {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = viewModel::scan, enabled = !state.isLoading) { if (state.isLoading) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp); Text(if (state.isLoading) "Scanning…" else "Scan old files") }
                        if (state.isLoading) OutlinedButton(onClick = viewModel::cancelScan) { Icon(Icons.Outlined.Cancel, contentDescription = null); Text("Cancel", modifier = Modifier.padding(start = 6.dp)) }
                    }
                }
                item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(OldFilesScope.entries) { scope -> FilterChip(selected = state.scope == scope, onClick = { viewModel.setScope(scope) }, label = { Text(scope.label) }) } } }
                item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(oldAgeOptions) { age -> FilterChip(selected = state.age == age, onClick = { viewModel.setAge(age) }, label = { Text(age.label) }) } } }
                item { LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { items(OldFilesSort.entries) { sort -> FilterChip(selected = state.sort == sort, onClick = { viewModel.setSort(sort) }, label = { Text(sort.label) }) } } }
                item { OutlinedTextField(value = state.search, onValueChange = viewModel::setSearch, modifier = Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }, placeholder = { Text("Search name or path") }) }
                item { Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text("${state.visibleEntries.size} matching file(s)"); TextButton(onClick = viewModel::selectAll, enabled = state.visibleEntries.isNotEmpty()) { Text(if (state.allVisibleSelected) "Clear all" else "Select all") } } }
                if (!state.hasAnalyzed) item { Text("Choose a scope and age, then scan to build the list.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                state.errorMessage?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
                items(state.visibleEntries, key = { it.ref.toString() }) { entry ->
                    val selected = entry.ref.toString() in state.selectedRefs
                    Card(colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Checkbox(checked = selected, onCheckedChange = { viewModel.toggle(entry.ref.toString()) })
                            Icon(Icons.Outlined.InsertDriveFile, contentDescription = null)
                            Column(modifier = Modifier.weight(1f)) { Text(entry.name, fontWeight = FontWeight.SemiBold, maxLines = 1); Text("Reason: old file in ${state.scope.label}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary); Text(entry.path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1); Text(Formatter.formatFileSize(context, entry.sizeBytes), style = MaterialTheme.typography.bodySmall) }
                            IconButton(onClick = { runCatching { context.tryOpen(entry) }.onFailure { viewModel.setSearch(state.search) } }) { Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open ${entry.name}") }
                        }
                    }
                }
            }
        }
        if (state.selectedEntries.isNotEmpty()) Surface(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(), tonalElevation = 6.dp) { Button(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth().padding(12.dp)) { Icon(Icons.Outlined.Delete, contentDescription = null); Text("Delete ${state.selectedEntries.size} · ${Formatter.formatFileSize(context, state.selectedBytes)}", modifier = Modifier.padding(start = 8.dp)) } }
    }
}
