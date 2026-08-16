package az.simplesoft.tooliva.feature.clean.screenshots

import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Size
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Screenshot
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.ScreenshotMediaFile
import az.simplesoft.tooliva.core.media.hasScreenshotPermission
import az.simplesoft.tooliva.core.media.requiredScreenshotPermission
import az.simplesoft.tooliva.feature.clean.result.CleanupResultScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val screenshotAgeOptions = listOf(30, 90, 365)

@Composable
fun ScreenshotCleanerRoute(viewModel: ScreenshotCleanerViewModel = viewModel()) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var hasAccess by remember { mutableStateOf(hasScreenshotPermission(context)) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val deleteCoordinator = remember(context) { MediaStoreDeleteCoordinator(context) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        hasAccess = hasScreenshotPermission(context)
        if (hasAccess) viewModel.scan()
    }
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

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val access = hasScreenshotPermission(context)
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
            title = { Text("Move selected screenshots to Trash?") },
            text = {
                Text(
                    "${state.selectedFiles.size} screenshot(s), " +
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
                    enabled = state.selectedFiles.isNotEmpty() && !state.isPreparingDelete,
                ) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("Cancel") }
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
                Text("Screenshots", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    "Review screenshots found in your local MediaStore. Nothing is deleted automatically.",
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
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
                        Text("Allow photo access", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Tooliva uses the existing photo permission to find screenshot folders and names on this phone. Images stay on-device and are never deleted automatically.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(onClick = { permissionLauncher.launch(requiredScreenshotPermission()) }) {
                            Text("Allow access")
                        }
                    }
                }
            }
        } else {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    screenshotAgeOptions.forEach { days ->
                        FilterChip(
                            selected = state.ageDays == days,
                            onClick = { viewModel.setAgeDays(days) },
                            label = { Text("$days+ days") },
                        )
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Screenshot, contentDescription = null)
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text("${state.files.size} screenshots", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    "${state.selectedFiles.size} selected · ${Formatter.formatFileSize(context, state.selectedBytes)}",
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (state.files.isNotEmpty()) {
                                TextButton(onClick = viewModel::toggleSelectAll) {
                                    Text(if (state.allSelected) "Clear all" else "Select all")
                                }
                            }
                        }
                        if (state.isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                CircularProgressIndicator()
                                Text("Scanning screenshot buckets…")
                            }
                        }
                    }
                }
            }

            state.errorMessage?.let { message ->
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Screenshot scan needs attention", fontWeight = FontWeight.Bold)
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
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator()
                            Text("Checking screenshots and verifying the result…")
                        }
                    }
                }
            }

            if (!state.isLoading && state.errorMessage == null && state.files.isEmpty()) {
                item {
                    Text(
                        "No screenshots were found in the selected age range.",
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

            items(state.files.chunked(2)) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    row.forEach { file ->
                        ScreenshotCard(
                            file = file,
                            selected = file.uri.toString() in state.selectedUris,
                            onClick = { viewModel.toggleSelection(file.uri.toString()) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ScreenshotCard(
    file: ScreenshotMediaFile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            ) {
                MediaThumbnail(file.uri, file.displayName, Modifier.fillMaxSize())
                Checkbox(
                    checked = selected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(file.displayName, maxLines = 1, fontWeight = FontWeight.SemiBold)
                Text(
                    Formatter.formatFileSize(LocalContext.current, file.sizeBytes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun MediaThumbnail(uri: android.net.Uri, description: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, key1 = uri) {
        value = withContext(Dispatchers.IO) { loadThumbnail(context, uri) }
    }
    if (bitmap == null) {
        Box(modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
            Icon(Icons.Outlined.PhotoLibrary, contentDescription = null)
        }
    } else {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = description,
            modifier = modifier,
            contentScale = ContentScale.Crop,
        )
    }
}

private fun loadThumbnail(context: Context, uri: android.net.Uri): Bitmap? = runCatching {
    val resolver = context.contentResolver
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        resolver.loadThumbnail(uri, Size(480, 480), null)
    } else {
        resolver.openInputStream(uri)?.use { input ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(input, null, bounds)
            val sample = calculateSample(bounds.outWidth, bounds.outHeight, 480)
            resolver.openInputStream(uri)?.use { secondInput ->
                BitmapFactory.decodeStream(
                    secondInput,
                    null,
                    BitmapFactory.Options().apply { inSampleSize = sample },
                )
            }
        }
    }
}.getOrNull()

private fun calculateSample(width: Int, height: Int, target: Int): Int {
    var sample = 1
    while (width / sample > target || height / sample > target) sample *= 2
    return sample
}
