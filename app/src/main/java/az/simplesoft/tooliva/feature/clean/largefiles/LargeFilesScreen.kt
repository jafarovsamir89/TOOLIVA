package az.simplesoft.tooliva.feature.clean.largefiles

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.BackHandler
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.hasRequiredMediaPermissions
import az.simplesoft.tooliva.core.media.requiredMediaPermissions
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen

@Composable
fun LargeFilesRoute(viewModel: LargeFilesViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasAccess by remember { mutableStateOf(hasRequiredMediaPermissions(context)) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val deleteCoordinator = remember(context) { MediaStoreDeleteCoordinator(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasAccess = hasRequiredMediaPermissions(context)
        if (hasAccess) viewModel.scan()
    }
    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult(),
    ) { result ->
        val requestId = state.pendingDelete?.requestId
        if (requestId != null) {
            viewModel.onSystemDeleteResult(
                requestId = requestId,
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
        val access = hasRequiredMediaPermissions(context)
        hasAccess = access
        if (!access) viewModel.onMediaPermissionRevoked()
    }

    LaunchedEffect(hasAccess) {
        if (hasAccess && state.files.isEmpty() && !state.isLoading && state.errorMessage == null) {
            viewModel.scan()
        }
    }

    val cleanupResult = state.cleanupResult
    if (cleanupResult != null) {
        BackHandler { viewModel.dismissCleanupResult() }
        CleanupResultScreen(
            result = cleanupResult,
            onDone = viewModel::dismissCleanupResult,
        )
        return
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Move selected files to Trash?") },
            text = {
                Text(
                    "${state.selectedFiles.size} item(s), " +
                        "${Formatter.formatFileSize(context, state.selectedBytes)} selected. " +
                        "Android will ask for final confirmation. Nothing is changed if you cancel.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        viewModel.requestDelete(deleteCoordinator)
                    },
                    enabled = state.selectedFiles.isNotEmpty(),
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Large files", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    "Find photos and videos larger than 100 MB. Nothing leaves your phone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (!hasAccess) {
            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Text("Allow photo and video access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Tooliva needs access to your shared photo and video library to find large media across the phone. Files are analyzed locally and are never deleted automatically.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { permissionLauncher.launch(requiredMediaPermissions()) }) {
                            Text("Allow access")
                        }
                    }
                }
            }
        } else {
            item {
                val totalBytes = state.files.sumOf { it.sizeBytes }
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(Icons.Outlined.FolderOpen, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${state.files.size} large files", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(
                                Formatter.formatFileSize(context, totalBytes),
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        if (state.isLoading) CircularProgressIndicator()
                    }
                }
            }

            state.errorMessage?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Scan failed", fontWeight = FontWeight.Bold)
                            Text(message)
                            Button(onClick = viewModel::scan, modifier = Modifier.padding(top = 8.dp)) {
                                Text("Try again")
                            }
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

            if (state.isPreparingDelete) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Checking the selected files and verifying the result…")
                        }
                    }
                }
            }

            if (!state.isLoading && state.errorMessage == null && state.files.isEmpty()) {
                item {
                    Text(
                        "No photos or videos larger than 100 MB were found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            }

            if (state.selectedFiles.isNotEmpty()) {
                item {
                    Button(
                        onClick = { showDeleteConfirmation = true },
                        enabled = !state.isPreparingDelete,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                        Text(
                            "Move ${state.selectedFiles.size} to Trash · ${Formatter.formatFileSize(context, state.selectedBytes)}",
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            items(state.files, key = { it.uri.toString() }) { file ->
                val isSelected = file.uri.toString() in state.selectedUris
                Card(
                    onClick = { viewModel.toggleSelection(file.uri.toString()) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { viewModel.toggleSelection(file.uri.toString()) },
                        )
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                file.mimeType ?: "Media file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(Formatter.formatFileSize(context, file.sizeBytes), fontWeight = FontWeight.Bold)
                        IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(file.uri, file.mimeType ?: "application/octet-stream")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            try {
                                context.startActivity(intent)
                            } catch (_: ActivityNotFoundException) {
                                viewModel.showError("No app can open this media file.")
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Outlined.OpenInNew, contentDescription = "Open ${file.displayName}")
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
            }
        }
    }
}
