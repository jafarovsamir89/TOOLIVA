package az.simplesoft.tooliva.feature.files

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.InsertDriveFile
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import az.simplesoft.tooliva.core.files.SafSource
import az.simplesoft.tooliva.core.files.SafSourceStore
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import az.simplesoft.tooliva.ui.theme.ToolivaSpacing

@Composable
fun SafSourcesRoute(onBack: () -> Unit, viewModel: SafSourcesViewModel = viewModel()) {
    val context = LocalContext.current
    val sources by viewModel.sources.collectAsStateWithLifecycle(initialValue = emptyList())
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
            viewModel.add(SafSource(uri.toString(), DocumentFile.fromTreeUri(context, uri)?.name ?: "External source"))
        }
    }
    var openSource by remember { mutableStateOf<SafSource?>(null) }
    if (openSource != null) {
        SafSourceBrowser(openSource!!, onBack = { openSource = null })
        return
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(ToolivaSpacing.lg), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("External sources", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black); Text("SD card, USB OTG and installed cloud providers through Android", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
        item { Button(onClick = { picker.launch(null) }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Cloud, null); Text("Add storage or cloud folder", Modifier.padding(start = 8.dp)) } }
        if (sources.isEmpty()) item { Text("No external source added yet. Android will show available SD, USB and cloud providers in the picker.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(sources, key = { it.uri }) { source ->
            Card(onClick = { openSource = source }, shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Outlined.Folder, null, tint = MaterialTheme.colorScheme.primary); Column(Modifier.weight(1f).padding(start = 12.dp)) { Text(source.label, fontWeight = FontWeight.Bold); Text("User-selected access", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }; IconButton(onClick = { viewModel.remove(source.uri) }) { Icon(Icons.Outlined.Delete, "Remove") } }
            }
        }
    }
}

@Composable
private fun SafSourceBrowser(source: SafSource, onBack: () -> Unit) {
    val context = LocalContext.current
    var uriStack by remember(source.uri) { mutableStateOf(listOf(Uri.parse(source.uri))) }
    val currentUri = uriStack.last()
    val document = remember(currentUri) { DocumentFile.fromTreeUri(context, currentUri) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(ToolivaSpacing.lg), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(source.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); TextButton(onClick = { if (uriStack.size > 1) uriStack = uriStack.dropLast(1) else onBack() }) { Text(if (uriStack.size > 1) "Up" else "Back") } } }
        item { Text("Android controls which folders and actions this provider exposes.", color = MaterialTheme.colorScheme.onSurfaceVariant) }
        items(document?.listFiles()?.sortedWith(compareByDescending<DocumentFile> { it.isDirectory }.thenBy { it.name.orEmpty().lowercase() }).orEmpty().toList(), key = { it.uri.toString() }) { file ->
            Card(shape = ToolivaShapes.medium, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), onClick = { if (file.isDirectory) uriStack = uriStack + DocumentsContract.buildTreeDocumentUri(file.uri.authority, DocumentsContract.getDocumentId(file.uri)) else openDocument(context, file.uri) }) {
                Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Icon(if (file.isDirectory) Icons.Outlined.Folder else Icons.Outlined.InsertDriveFile, null); Text(file.name ?: "Unnamed", modifier = Modifier.padding(start = 10.dp), maxLines = 1) }
            }
        }
    }
}

private fun openDocument(context: Context, uri: Uri) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, context.contentResolver.getType(uri) ?: "*/*"); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }) }
}

class SafSourcesViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val store = SafSourceStore(application)
    val sources = store.sources
    fun add(source: SafSource) { viewModelScope.launch { store.add(source) } }
    fun remove(uri: String) { viewModelScope.launch { store.remove(uri) } }
}
