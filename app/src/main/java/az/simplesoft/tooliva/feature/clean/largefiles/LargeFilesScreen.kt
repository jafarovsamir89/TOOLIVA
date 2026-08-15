package az.simplesoft.tooliva.feature.clean.largefiles

import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.media.hasRequiredMediaPermissions
import az.simplesoft.tooliva.core.media.requiredMediaPermissions

@Composable
fun LargeFilesRoute(viewModel: LargeFilesViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasAccess by remember { mutableStateOf(hasRequiredMediaPermissions(context)) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasAccess = hasRequiredMediaPermissions(context)
        if (hasAccess) viewModel.scan()
    }

    LaunchedEffect(hasAccess) {
        if (hasAccess && state.files.isEmpty() && !state.isLoading && state.errorMessage == null) {
            viewModel.scan()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
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

            if (!state.isLoading && state.errorMessage == null && state.files.isEmpty()) {
                item {
                    Text(
                        "No photos or videos larger than 100 MB were found.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 20.dp),
                    )
                }
            }

            items(state.files, key = { it.uri.toString() }) { file ->
                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(file.displayName, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                file.mimeType ?: "Media file",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            Formatter.formatFileSize(context, file.sizeBytes),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}
