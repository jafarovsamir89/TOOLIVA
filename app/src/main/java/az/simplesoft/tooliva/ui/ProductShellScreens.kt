package az.simplesoft.tooliva.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import az.simplesoft.tooliva.ui.LocalizedIcon as Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import az.simplesoft.tooliva.ui.LocalizedText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import az.simplesoft.tooliva.core.cache.UsageAccessChecker
import az.simplesoft.tooliva.core.notifications.NotificationHistoryRepository
import az.simplesoft.tooliva.core.settings.AppearanceMode
import az.simplesoft.tooliva.core.settings.ToolivaPreferences
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.ui.theme.ToolivaSectionHeader
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import az.simplesoft.tooliva.ui.theme.ToolivaSpacing
import az.simplesoft.tooliva.ui.theme.ToolivaToolTile
import kotlinx.coroutines.launch
import az.simplesoft.tooliva.BuildConfig
import az.simplesoft.tooliva.core.settings.ToolivaLanguage
import az.simplesoft.tooliva.core.settings.ToolivaUserDataStore

data class ToolHubItem(val id: String, val title: String, val subtitle: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val toolHubItems = listOf(
    ToolHubItem("doctor", "Phone Doctor", "Battery, memory, storage and thermal facts", Icons.Outlined.Thermostat),
    ToolHubItem("hardware", "Hardware Tests", "Display, touch, sound and sensors", Icons.Outlined.Build),
    ToolHubItem("optimizer", "Phone Optimizer", "Real memory and system cache actions", Icons.Outlined.Memory),
    ToolHubItem("app-manager", "App Manager", "Review visible installed apps", Icons.Outlined.Apps),
    ToolHubItem("notification-history", "Notification History", "Local notification timeline", Icons.Outlined.Notifications),
    ToolHubItem("storage-map", "Storage Map", "Understand folder space usage", Icons.Outlined.Storage),
    ToolHubItem("clean/duplicates", "Exact Duplicates", "Compare identical files safely", Icons.Outlined.CleaningServices),
    ToolHubItem("recycle-bin", "Recycle Bin", "Restore or permanently remove Android Trash items", Icons.Outlined.Recycling),
    ToolHubItem("external-sources", "External Sources", "SD, USB and cloud folders via Android", Icons.Outlined.Storage),
    ToolHubItem("scan-history", "Scan History", "See local storage changes over time", Icons.Outlined.Storage),
)

@Composable
fun ToolsRoute(onOpenTool: (String) -> Unit) {
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
            item { ToolivaSectionHeader("Tools", subtitle = "Real device and storage utilities") }
            items(toolHubItems, key = { it.id }) { item -> ToolivaToolTile(item.title, item.icon, subtitle = item.subtitle) { onOpenTool(item.id) } }
        }
    }
}

@Composable
fun MoreRoute(onSettings: () -> Unit) {
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
            item { ToolivaSectionHeader("More", subtitle = "Preferences, privacy and app information") }
            item { ToolivaToolTile("Settings", Icons.Outlined.Settings, subtitle = "Appearance, access and preferences", onClick = onSettings) }
            item { ToolivaToolTile("Privacy & Security", Icons.Outlined.PrivacyTip, subtitle = "Local processing and permission summary") { onSettings() } }
            item { ToolivaToolTile("About Tooliva", Icons.Outlined.Info, subtitle = "Version ${BuildConfig.VERSION_NAME}") { onSettings() } }
        }
    }
}

@Composable
fun SettingsRoute(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember(context) { ToolivaPreferences(context) }
    val userData = remember(context) { ToolivaUserDataStore(context) }
    val storageAccess = remember(context) { StorageAccessCoordinator(context) }
    val usageAccess = remember(context) { UsageAccessChecker(context) }
    val notificationAccess = remember(context) { NotificationHistoryRepository(context) }
    var appearance by remember { mutableStateOf(AppearanceMode.SYSTEM) }
    var language by remember { mutableStateOf(ToolivaLanguage.ENGLISH) }
    var storageState by remember { mutableStateOf(storageAccess.currentState()) }
    var usageGranted by remember { mutableStateOf(usageAccess.isGranted()) }
    var notificationGranted by remember { mutableStateOf(notificationAccess.isAccessGranted()) }
    var showPrivacy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { preferences.appearance.collect { appearance = it } }
    LaunchedEffect(Unit) { userData.language.collect { language = it } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        storageState = storageAccess.currentState()
        usageGranted = usageAccess.isGranted()
        notificationGranted = notificationAccess.isAccessGranted()
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text("Privacy & Security") },
            text = { Text("Tooliva's core utilities process accessible data locally on this device. Notification History stays local and is controlled by Android Notification Access. Tooliva does not upload user files, notification text or vault content.") },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text("Done") } },
        )
    }
    Scaffold { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.lg)) {
            item { ToolivaSectionHeader("Settings", subtitle = "Make Tooliva fit the way you use your phone") }
            item {
                SettingsCard("Appearance", "Choose how Tooliva looks") {
                    Row(horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.sm)) {
                        AppearanceMode.entries.forEach { mode ->
                            FilterChip(selected = appearance == mode, onClick = { appearance = mode; scope.launch { preferences.setAppearance(mode) } }, label = { Text(mode.label()) })
                        }
                    }
                }
            }
            item {
                SettingsCard("Language", "Choose the app language") {
                    Row(horizontalArrangement = Arrangement.spacedBy(ToolivaSpacing.sm)) {
                        ToolivaLanguage.entries.forEach { option ->
                            FilterChip(selected = language == option, onClick = { language = option; scope.launch { userData.setLanguage(option) } }, label = { Text(option.label) })
                        }
                    }
                }
            }
            item {
                SettingsCard("Access", "Current Android special-access status") {
                    AccessRow("Full Storage Access", if (storageState.mode == StorageAccessMode.FULL) "Enabled" else "Not enabled") {
                        try { storageAccess.allFilesSettingsIntent()?.let(context::startActivity) } catch (_: ActivityNotFoundException) { }
                    }
                    AccessRow("Usage Access", if (usageGranted) "Enabled" else "Disabled") {
                        try { context.startActivity(usageAccess.settingsIntent()) } catch (_: ActivityNotFoundException) { }
                    }
                    AccessRow("Notification History Access", if (notificationGranted) "Enabled" else "Disabled") {
                        try { context.startActivity(notificationAccess.accessIntent()) } catch (_: ActivityNotFoundException) { }
                    }
                }
            }
            item { SettingsCard("Privacy & Security", "Factual local-processing summary") { OutlinedButton(onClick = { showPrivacy = true }) { Text("Read summary") } } }
            item { SettingsCard("About Tooliva", "Version ${BuildConfig.VERSION_NAME}") { Text("Tooliva is an offline-first Android utility. No Pro, ads or billing are enabled in this build.", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
            item { Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Done") } }
        }
    }
}

@Composable
private fun SettingsCard(title: String, subtitle: String, content: @Composable () -> Unit) {
    Card(shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(modifier = Modifier.padding(ToolivaSpacing.xl), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            content()
        }
    }
}

@Composable
private fun AccessRow(title: String, status: String, onManage: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) { Text(title, fontWeight = FontWeight.SemiBold); Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
        TextButton(onClick = onManage) { Text("Manage") }
    }
}

private fun AppearanceMode.label(): String = when (this) {
    AppearanceMode.SYSTEM -> "System"
    AppearanceMode.DARK -> "Dark"
    AppearanceMode.LIGHT -> "Light"
}
