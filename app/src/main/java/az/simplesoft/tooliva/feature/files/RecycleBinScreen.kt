package az.simplesoft.tooliva.feature.files

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import az.simplesoft.tooliva.ui.LocalizedIcon as Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import az.simplesoft.tooliva.ui.LocalizedText as Text
import androidx.compose.material3.TextButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.files.RecycleBinRepository
import az.simplesoft.tooliva.core.files.TrashedItem
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import az.simplesoft.tooliva.ui.theme.ToolivaSpacing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@Composable
fun RecycleBinRoute(onBack: () -> Unit, viewModel: RecycleBinViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.refresh() }
    RecycleBinScreen(state, onBack, viewModel::restore, viewModel::deleteForever)
}

data class RecycleBinUiState(val isLoading: Boolean = false, val items: List<TrashedItem> = emptyList(), val message: String? = null)

@Composable
private fun RecycleBinScreen(state: RecycleBinUiState, onBack: () -> Unit, onRestore: (TrashedItem) -> Unit, onDelete: (TrashedItem) -> Unit) {
    val context = LocalContext.current
    var deleteCandidate by remember { mutableStateOf<TrashedItem?>(null) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(ToolivaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Recycle Bin", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("Only Android-controlled MediaStore Trash items appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant) }; TextButton(onClick = onBack) { Text("Back") } } }
        item { Text("Android controls the retention period and expiry date. Tooliva never permanently deletes Trash items automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary) }
        if (state.isLoading) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator() } }
        else if (state.items.isEmpty()) item { Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) { Icon(Icons.Outlined.Recycling, null); Text("Trash is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) } }
        else items(state.items, key = { it.uri.toString() }) { item ->
            Card(shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Recycling, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f).padding(start = 10.dp)) { Text(item.name, maxLines = 1, fontWeight = FontWeight.SemiBold); Text(Formatter.formatFileSize(context, item.sizeBytes)); item.expiresAtMillis?.let { Text("Expires ${DateFormat.getDateInstance(DateFormat.SHORT).format(Date(it))}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) } }; IconButton(onClick = { onRestore(item) }) { Icon(Icons.Outlined.Restore, "Restore") }; IconButton(onClick = { deleteCandidate = item }) { Icon(Icons.Outlined.DeleteForever, "Delete permanently") } }
            }
        }
        state.message?.let { item { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
    deleteCandidate?.let { candidate ->
        AlertDialog(onDismissRequest = { deleteCandidate = null }, title = { Text("Delete permanently?") }, text = { Text("${candidate.name} will be removed from Android Trash and cannot be restored.") }, confirmButton = { TextButton(onClick = { deleteCandidate = null; onDelete(candidate) }) { Text("Delete permanently") } }, dismissButton = { TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") } })
    }
}

class RecycleBinViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val repository = RecycleBinRepository(application)
    private val _uiState = MutableStateFlow(RecycleBinUiState())
    val uiState = _uiState.asStateFlow()
    fun refresh() { viewModelScope.launch { _uiState.value = _uiState.value.copy(isLoading = true); _uiState.value = RecycleBinUiState(items = runCatching { repository.read() }.getOrElse { emptyList() }, message = null) } }
    fun restore(item: TrashedItem) { viewModelScope.launch { val ok = runCatching { repository.restore(item) }.getOrDefault(false); _uiState.value = _uiState.value.copy(message = if (ok) "Restored ${item.name}." else "Android could not restore this item."); refresh() } }
    fun deleteForever(item: TrashedItem) { viewModelScope.launch { val ok = runCatching { repository.permanentlyDelete(item) }.getOrDefault(false); _uiState.value = _uiState.value.copy(message = if (ok) "Permanently deleted ${item.name}." else "Android could not delete this item."); refresh() } }
}
