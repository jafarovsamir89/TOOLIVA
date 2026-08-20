@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package az.simplesoft.tooliva.feature.notifications

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.ImageView
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.notifications.NotificationHistoryEntity
import az.simplesoft.tooliva.core.notifications.NotificationHistoryRange
import az.simplesoft.tooliva.core.notifications.NotificationRetention
import java.util.Date

@Composable
fun NotificationHistoryRoute(
    onBack: () -> Unit,
    viewModel: NotificationHistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDisclosure by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    var showClearAll by remember { mutableStateOf(false) }
    var excludePackage by remember { mutableStateOf<String?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshAccess() }
    BackHandler { if (state.detailsId != null) viewModel.closeDetails() else onBack() }

    val details = viewModel.details()
    if (details != null) {
        NotificationDetailsScreen(details, onBack = viewModel::closeDetails, onPin = { viewModel.togglePinned(details) }, onDelete = { viewModel.deleteEntry(details); viewModel.closeDetails() })
        return
    }

    NotificationHistoryScreen(
        state = state,
        onBack = onBack,
        onEnable = { showDisclosure = true },
        onSearch = viewModel::setQuery,
        onRange = viewModel::setRange,
        onPackage = viewModel::setPackageFilter,
        onOpenDetails = viewModel::openDetails,
        onPin = viewModel::togglePinned,
        onDelete = viewModel::deleteEntry,
        onShowSettings = { showSettings = !showSettings },
        showSettings = showSettings,
        onPaused = viewModel::setPaused,
        onIncludeOngoing = viewModel::setIncludeOngoing,
        onRetention = viewModel::setRetention,
        onExclude = { excludePackage = it },
        onInclude = viewModel::includePackage,
        onClearAll = { showClearAll = true },
    )

    if (showDisclosure) {
        AlertDialog(
            onDismissRequest = { showDisclosure = false },
            title = { Text("Notification History") },
            text = { Text("To save a private history of notifications, Tooliva needs Android Notification Access. When enabled, Tooliva can receive notification content Android provides, including the sending app, title, text and time. History stays on this device and is not uploaded or used for advertising. You can exclude apps and delete history at any time.") },
            confirmButton = {
                TextButton(onClick = {
                    showDisclosure = false
                    startSettings(context, viewModel.accessIntent())
                }) { Text("Continue to Android settings") }
            },
            dismissButton = { TextButton(onClick = { showDisclosure = false }) { Text("Cancel") } },
        )
    }
    if (showClearAll) {
        AlertDialog(
            onDismissRequest = { showClearAll = false },
            title = { Text("Clear notification history?") },
            text = { Text("Delete all notification history stored on this device? Pinned notifications are included.") },
            confirmButton = { TextButton(onClick = { showClearAll = false; viewModel.clearAll() }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { showClearAll = false }) { Text("Cancel") } },
        )
    }
    excludePackage?.let { packageName ->
        AlertDialog(
            onDismissRequest = { excludePackage = null },
            title = { Text("Exclude this app?") },
            text = { Text("Future notifications from $packageName will not be saved. Keep existing history or delete it too?") },
            confirmButton = { TextButton(onClick = { excludePackage = null; viewModel.excludePackage(packageName, true) }) { Text("Delete existing") } },
            dismissButton = { TextButton(onClick = { excludePackage = null; viewModel.excludePackage(packageName, false) }) { Text("Keep existing") } },
        )
    }
}

@Composable
private fun NotificationHistoryScreen(
    state: NotificationHistoryUiState,
    onBack: () -> Unit,
    onEnable: () -> Unit,
    onSearch: (String) -> Unit,
    onRange: (NotificationHistoryRange) -> Unit,
    onPackage: (String?) -> Unit,
    onOpenDetails: (Long) -> Unit,
    onPin: (NotificationHistoryEntity) -> Unit,
    onDelete: (NotificationHistoryEntity) -> Unit,
    onShowSettings: () -> Unit,
    showSettings: Boolean,
    onPaused: (Boolean) -> Unit,
    onIncludeOngoing: (Boolean) -> Unit,
    onRetention: (NotificationRetention) -> Unit,
    onExclude: (String) -> Unit,
    onInclude: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    val context = LocalContext.current
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Notification History", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } },
            actions = { IconButton(onClick = onShowSettings) { Icon(Icons.Outlined.Settings, "Settings") } },
        )
    }) { innerPadding ->
        LazyColumn(Modifier.fillMaxSize().padding(innerPadding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!state.accessGranted) item { AccessCard(onEnable) }
            if (state.paused) item { Text("History paused — new notifications are not being saved.", color = MaterialTheme.colorScheme.tertiary) }
            item {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(Icons.Outlined.Notifications, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                        Column(Modifier.weight(1f)) { Text("${state.entries.size} notifications", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text("${state.appCounts.size} apps in this view", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    }
                }
            }
            item { OutlinedTextField(state.query, onSearch, Modifier.fillMaxWidth(), singleLine = true, leadingIcon = { Icon(Icons.Outlined.Search, null) }, placeholder = { Text("Search app, title or text") }, label = { Text("Search locally") }) }
            item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NotificationHistoryRange.entries.forEach { range -> FilterChip(selected = state.range == range, onClick = { onRange(range) }, label = { Text(range.label) }) }
                }
            }
            if (state.appCounts.isNotEmpty()) item {
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = state.packageFilter == null, onClick = { onPackage(null) }, label = { Text("All apps") })
                    state.appCounts.take(8).forEach { app -> FilterChip(selected = state.packageFilter == app.packageName, onClick = { onPackage(app.packageName) }, label = { Text("${app.appLabelSnapshot} ${app.count}") }) }
                }
            }
            if (showSettings) item { NotificationSettingsCard(state, onPaused, onIncludeOngoing, onRetention, onExclude, onInclude, onClearAll) }
            if (state.entries.isEmpty()) item { EmptyHistoryCard(state.accessGranted) }
            else items(state.entries, key = { it.id }) { entry -> NotificationRow(entry, onOpenDetails, onPin, onDelete, context) }
        }
    }
}

@Composable
private fun AccessCard(onEnable: () -> Unit) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Notification Access is off", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Tooliva can only save notifications received after you explicitly enable access. Existing Android notifications cannot be recovered.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onEnable) { Text("Enable notification history") }
        }
    }
}

@Composable
private fun NotificationSettingsCard(
    state: NotificationHistoryUiState,
    onPaused: (Boolean) -> Unit,
    onIncludeOngoing: (Boolean) -> Unit,
    onRetention: (NotificationRetention) -> Unit,
    onExclude: (String) -> Unit,
    onInclude: (String) -> Unit,
    onClearAll: () -> Unit,
) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("History settings", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            SettingSwitch("Pause history", state.paused, onPaused)
            SettingSwitch("Include ongoing notifications", state.includeOngoing, onIncludeOngoing)
            Text("Keep history for", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) { NotificationRetention.entries.forEach { option -> FilterChip(selected = state.retention == option, onClick = { onRetention(option) }, label = { Text(option.label) }) } }
            Text("Excluded apps", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (state.excludedPackages.isEmpty()) Text("None", color = MaterialTheme.colorScheme.onSurfaceVariant)
            else state.excludedPackages.forEach { packageName -> Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(packageName, Modifier.weight(1f)); TextButton(onClick = { onInclude(packageName) }) { Text("Include") } } }
            val availableApps = state.appCounts.filterNot { it.packageName in state.excludedPackages }.take(8)
            if (availableApps.isNotEmpty()) {
                Text("Exclude an app", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                availableApps.forEach { app ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text("${app.appLabelSnapshot} · ${app.count}", Modifier.weight(1f))
                        TextButton(onClick = { onExclude(app.packageName) }) { Text("Exclude") }
                    }
                }
            }
            Text("You can exclude banking, password-manager or private messaging apps if you do not want their notifications stored.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClearAll) { Icon(Icons.Outlined.DeleteOutline, null); Text("Clear all history", Modifier.padding(start = 6.dp)) }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) { Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text(label, Modifier.weight(1f)); Switch(checked, onChange) } }

@Composable
private fun NotificationRow(entry: NotificationHistoryEntity, onOpen: (Long) -> Unit, onPin: (NotificationHistoryEntity) -> Unit, onDelete: (NotificationHistoryEntity) -> Unit, context: Context) {
    Card(Modifier.fillMaxWidth().clickable { onOpen(entry.id) }, shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            AppIcon(entry.packageName, Modifier.size(42.dp), context)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) { Text(entry.appLabelSnapshot, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)); Text(DateFormat.format("HH:mm", Date(entry.postedAtMillis)).toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text(entry.title ?: "Notification", maxLines = 1)
                Text(entry.text ?: entry.bigText ?: "No preview text", maxLines = 2, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
            }
            if (entry.isPinned) Icon(Icons.Outlined.PushPin, "Pinned", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NotificationDetailsScreen(entry: NotificationHistoryEntity, onBack: () -> Unit, onPin: () -> Unit, onDelete: () -> Unit) {
    Scaffold(topBar = { TopAppBar(title = { Text("Notification details", fontWeight = FontWeight.Bold) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") } }) }) { innerPadding ->
        LazyColumn(Modifier.fillMaxSize().padding(innerPadding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item { Text(entry.appLabelSnapshot, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text(entry.packageName, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            item { DetailCard("Received", DateFormat.format("yyyy-MM-dd HH:mm", Date(entry.postedAtMillis)).toString()); entry.removedAtMillis?.let { DetailCard("Removed", DateFormat.format("yyyy-MM-dd HH:mm", Date(it)).toString()) } }
            item { DetailCard("Title", entry.title ?: "Unavailable"); DetailCard("Text", entry.text ?: "Unavailable"); entry.bigText?.let { DetailCard("Expanded text", it) }; entry.subText?.let { DetailCard("Subtext", it) }; entry.category?.let { DetailCard("Category", it) }; entry.channelId?.let { DetailCard("Channel", it) } }
            item { Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedButton(onClick = onPin) { Icon(Icons.Outlined.PushPin, null); Text(if (entry.isPinned) "Unpin" else "Pin", Modifier.padding(start = 6.dp)) }; Button(onClick = onDelete) { Icon(Icons.Outlined.DeleteOutline, null); Text("Delete", Modifier.padding(start = 6.dp)) } } }
        }
    }
}

@Composable private fun DetailCard(label: String, value: String) { Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) { Column(Modifier.padding(16.dp)) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, modifier = Modifier.padding(top = 4.dp)) } } }

@Composable
private fun EmptyHistoryCard(accessGranted: Boolean) { Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Outlined.Info, null); Text(if (accessGranted) "No saved notifications yet" else "No saved history", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(if (accessGranted) "New notifications will appear after Android delivers them to Tooliva." else "Grant Notification Access to save future notifications.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }

@Composable
private fun AppIcon(packageName: String, modifier: Modifier, context: Context) { AndroidView(modifier = modifier, factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_INSIDE } }, update = { it.setImageDrawable(runCatching { context.packageManager.getApplicationIcon(packageName) }.getOrNull() ?: context.packageManager.defaultActivityIcon) }) }

private fun startSettings(context: Context, intent: Intent) { runCatching { context.startActivity(intent) } }
