package az.simplesoft.tooliva.feature.appmanager

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.appmanager.AppFilter
import az.simplesoft.tooliva.core.appmanager.AppItem
import az.simplesoft.tooliva.core.appmanager.AppManagerSnapshot
import az.simplesoft.tooliva.core.appmanager.AppSort
import az.simplesoft.tooliva.core.appmanager.AppStorageInfo
import az.simplesoft.tooliva.core.appmanager.AppUsageInfo
import az.simplesoft.tooliva.core.appmanager.daysSinceLastUse
import az.simplesoft.tooliva.core.appmanager.isRemovable
import java.text.DateFormat
import java.util.Date

@Composable
fun AppManagerRoute(
    onBack: () -> Unit,
    viewModel: AppManagerViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var actionError by remember { mutableStateOf<String?>(null) }
    val uninstallLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.onUninstallReturned()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }

    val pendingUninstall = state.pendingUninstallPackage
    if (pendingUninstall != null) {
        val intent = remember(pendingUninstall) {
            Intent(Intent.ACTION_DELETE, Uri.parse("package:$pendingUninstall"))
        }
        LaunchedEffect(intent) {
            try {
                uninstallLauncher.launch(intent)
            } catch (_: ActivityNotFoundException) {
                actionError = "Android did not provide an uninstall screen."
                viewModel.onUninstallReturned()
            } catch (_: SecurityException) {
                actionError = "Android did not allow the uninstall request."
                viewModel.onUninstallReturned()
            }
        }
    }

    BackHandler {
        if (state.detailsPackage != null) viewModel.closeDetails() else onBack()
    }

    val details = viewModel.currentDetails()
    if (state.detailsPackage != null && details != null) {
        AppDetailsScreen(
            item = details,
            usageAccessGranted = state.usageAccessGranted,
            onBack = viewModel::closeDetails,
            onOpen = {
                launchExternal(context, viewModel.launchIntent(details.packageName)) { actionError = it }
            },
            onAppInfo = {
                launchExternal(context, viewModel.appInfoIntent(details.packageName)) { actionError = it }
            },
            onUninstall = { viewModel.beginUninstall(details.packageName) },
            actionError = actionError,
        )
        return
    }

    AppManagerScreen(
        state = state,
        items = viewModel.filteredItems(),
        onBack = onBack,
        onRefresh = viewModel::refresh,
        onSearch = viewModel::setSearchQuery,
        onFilter = viewModel::setFilter,
        onRarelyDays = viewModel::setRarelyUsedDays,
        onSort = viewModel::setSort,
        onUsageAccess = {
            launchExternal(context, viewModel.usageAccessIntent()) { actionError = it }
        },
        onOpenDetails = viewModel::openDetails,
        onToggleSelection = viewModel::toggleSelection,
        onSelectAll = { viewModel.selectAll(viewModel.filteredItems()) },
        onClearSelection = viewModel::clearSelection,
        onUninstallSelected = viewModel::beginUninstallSelected,
        actionError = actionError,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppManagerScreen(
    state: AppManagerSnapshot,
    items: List<AppItem>,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onSearch: (String) -> Unit,
    onFilter: (AppFilter) -> Unit,
    onRarelyDays: (Int) -> Unit,
    onSort: (AppSort) -> Unit,
    onUsageAccess: () -> Unit,
    onOpenDetails: (String) -> Unit,
    onToggleSelection: (AppItem) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onUninstallSelected: () -> Unit,
    actionError: String?,
) {
    var sortMenuOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Manager", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
                actions = { IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Refresh") } },
            )
        },
        bottomBar = {
            if (state.selectedPackages.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text("${state.selectedPackages.size} selected", modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                        TextButton(onClick = onClearSelection) { Text("Clear") }
                        Button(onClick = onUninstallSelected) { Icon(Icons.Outlined.DeleteOutline, null); Text("Uninstall", Modifier.padding(start = 6.dp)) }
                    }
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SummaryCard(
                    items = state.items,
                    isLoading = state.isLoading,
                    isEnriching = state.isEnriching,
                )
            }
            if (!state.usageAccessGranted) {
                item {
                    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Outlined.Info, null, tint = MaterialTheme.colorScheme.primary)
                                Text("Usage Access is optional", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                            Text("Grant it to see Android's last-used times, rarely-used review and app storage statistics. Tooliva does not read app content.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            OutlinedButton(onClick = onUsageAccess) { Icon(Icons.Outlined.Settings, null); Text("Open Usage Access", Modifier.padding(start = 8.dp)) }
                        }
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    placeholder = { Text("Search app or package") },
                    label = { Text("Search") },
                )
            }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AppFilter.entries.forEach { filter ->
                        FilterChip(selected = state.filter == filter, onClick = { onFilter(filter) }, label = { Text(filter.title) })
                    }
                }
            }
            if (state.filter == AppFilter.RARELY_USED) {
                item {
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Older than", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        listOf(30, 90, 180).forEach { days ->
                            FilterChip(selected = state.rarelyUsedDays == days, onClick = { onRarelyDays(days) }, label = { Text("$days days") })
                        }
                    }
                }
            }
            item {
                Box {
                    OutlinedButton(onClick = { sortMenuOpen = true }) { Text("Sort: ${state.sort.title}") }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        AppSort.entries.forEach { sort ->
                            DropdownMenuItem(text = { Text(sort.title) }, onClick = { sortMenuOpen = false; onSort(sort) })
                        }
                    }
                }
            }
            state.error?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            state.message?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.primary) } }
            actionError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            if (state.isLoading) {
                item { LoadingCard() }
            } else if (items.isEmpty()) {
                item { EmptyCard(state.filter, state.searchQuery) }
            } else {
                items(items, key = { it.packageName }) { item ->
                    AppRow(
                        item = item,
                        selected = item.packageName in state.selectedPackages,
                        onClick = { onOpenDetails(item.packageName) },
                        onToggle = { onToggleSelection(item) },
                    )
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("${items.size} shown", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = onSelectAll) { Text("Select all removable") }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(items: List<AppItem>, isLoading: Boolean, isEnriching: Boolean) {
    Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Apps, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Column {
                    Text("Installed apps", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(if (isLoading) "Reading visible apps…" else "${items.size} visible to Tooliva", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (!isLoading) {
                Text("${items.count { !it.isSystem }} user · ${items.count { it.isSystem }} system", color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Android may limit package visibility. This list is not presented as a complete device inventory.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (isEnriching) Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Text("Loading Android storage and usage details…", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun AppRow(item: AppItem, selected: Boolean, onClick: () -> Unit, onToggle: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AppIcon(item.packageName, Modifier.size(48.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.label, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(item.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                Text(storageLabel(context, item), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            if (item.isRemovable()) Checkbox(checked = selected, onCheckedChange = { onToggle() })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppDetailsScreen(
    item: AppItem,
    usageAccessGranted: Boolean,
    onBack: () -> Unit,
    onOpen: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    actionError: String?,
) {
    val context = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("App details", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
        )
    }) { innerPadding ->
        LazyColumn(Modifier.fillMaxSize().padding(innerPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    AppIcon(item.packageName, Modifier.size(64.dp))
                    Column(Modifier.weight(1f)) {
                        Text(item.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                        Text(item.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(if (item.isSystem) "System app" else "User app", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            item {
                InfoCard("App information") {
                    DetailRow("Version", item.versionName ?: "Unavailable")
                    DetailRow("Installed", formatDate(item.firstInstallTime))
                    DetailRow("Updated", formatDate(item.lastUpdateTime))
                    DetailRow("Status", if (item.isEnabled) "Enabled" else "Disabled")
                    DetailRow("Launchable", if (item.isLaunchable) "Yes" else "No")
                }
            }
            item {
                InfoCard("Storage") {
                    when (val storage = item.storage) {
                        AppStorageInfo.NotLoaded -> Text("Calculating…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        is AppStorageInfo.Unavailable -> Text(storageUnavailableLabel(storage), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        is AppStorageInfo.Available -> {
                            DetailRow("Total (App + Data)", formatBytes(context, storage.totalBytes))
                            DetailRow("App", formatBytes(context, storage.appBytes))
                            DetailRow("Data (includes cache)", formatBytes(context, storage.dataBytes))
                            DetailRow("Cache (subset of Data)", formatBytes(context, storage.cacheBytes))
                        }
                    }
                }
            }
            item {
                InfoCard("Usage") {
                    if (!usageAccessGranted) Text("Usage Access required for last-used information.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else when (val usage = item.usage) {
                        AppUsageInfo.NotLoaded -> Text("Calculating…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        AppUsageInfo.Unavailable -> Text("Usage information unavailable.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        is AppUsageInfo.Available -> Text(usage.lastTimeUsed?.let { "Last used ${formatDate(it)}" } ?: "No recent usage recorded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            actionError?.let { message -> item { Text(message, color = MaterialTheme.colorScheme.error) } }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (item.isLaunchable) FilledTonalButton(onClick = onOpen, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.OpenInNew, null); Text("Open", Modifier.padding(start = 8.dp)) }
                    OutlinedButton(onClick = onAppInfo, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Info, null); Text("Android App info", Modifier.padding(start = 8.dp)) }
                    if (item.isRemovable()) Button(onClick = onUninstall, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Outlined.DeleteOutline, null); Text("Uninstall", Modifier.padding(start = 8.dp)) }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(title: String, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
    }
}

@Composable
private fun AppIcon(packageName: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    AndroidView(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } },
        update = { imageView ->
            imageView.setImageDrawable(runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull() ?: context.packageManager.defaultActivityIcon)
        },
    )
}

@Composable
private fun LoadingCard() {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.fillMaxWidth().padding(22.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
            Text("Loading visible apps…")
        }
    }
}

@Composable
private fun EmptyCard(filter: AppFilter, query: String) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(if (query.isBlank()) "No apps in this view" else "No matching apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(if (filter == AppFilter.RARELY_USED) "Apps without a real last-used timestamp are not classified as rarely used." else "Try another filter or search term.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun storageLabel(context: Context, item: AppItem): String = when (val storage = item.storage) {
    AppStorageInfo.NotLoaded -> "Storage: calculating…"
    is AppStorageInfo.Unavailable -> "Storage: unavailable"
    is AppStorageInfo.Available -> "Storage: ${formatBytes(context, storage.totalBytes)}"
}

private fun storageUnavailableLabel(storage: AppStorageInfo.Unavailable): String = when (storage.reason) {
    az.simplesoft.tooliva.core.appmanager.StorageUnavailableReason.USAGE_ACCESS_REQUIRED -> "Usage Access is required for Android storage statistics."
    az.simplesoft.tooliva.core.appmanager.StorageUnavailableReason.PACKAGE_DISAPPEARED -> "The app is no longer installed."
    else -> "Storage statistics unavailable on this device."
}

private fun formatDate(millis: Long): String = if (millis <= 0L) "Unavailable" else DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(millis))
private fun formatBytes(context: Context, bytes: Long): String = Formatter.formatFileSize(context, bytes.coerceAtLeast(0L))

private fun launchExternal(context: Context, intent: Intent?, onError: (String) -> Unit) {
    if (intent == null) {
        onError("Android did not provide this action for the selected app.")
        return
    }
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        onError("Android did not provide this screen.")
    } catch (_: SecurityException) {
        onError("Android did not allow this action.")
    }
}
