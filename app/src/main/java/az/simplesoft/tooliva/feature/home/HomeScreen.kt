package az.simplesoft.tooliva.feature.home

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

data class HomeTool(
    val id: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
)

private val primaryTools = listOf(
    HomeTool("clean", "Clean storage", "Large files, screenshots & duplicates", Icons.Outlined.CleaningServices),
    HomeTool("protect", "Protect", "App Lock, Vault & privacy tools", Icons.Outlined.Security),
    HomeTool("notifications", "Notification history", "Find notifications you dismissed", Icons.Outlined.Notifications),
    HomeTool("doctor", "Phone Doctor", "Battery, thermal, memory & sensors", Icons.Outlined.HealthAndSafety),
    HomeTool("hardware", "Hardware Tests", "Display, touch, sound, vibration & sensors", Icons.Outlined.Build),
    HomeTool("optimizer", "Phone Optimizer", "Real memory metrics & temporary cache", Icons.Outlined.Memory),
    HomeTool("files", "Files", "Browse, search and manage shared storage", Icons.Outlined.Folder),
    HomeTool("qr", "QR & quick tools", "Scanner, network, compass & more", Icons.Outlined.QrCodeScanner),
)

@Composable
fun HomeRoute(
    onCheckup: () -> Unit,
    onOpenTool: (String) -> Unit,
    onSettings: () -> Unit = {},
    viewModel: HomeViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(
        state = state,
        onCheckup = onCheckup,
        onOpenTool = onOpenTool,
        onSettings = onSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    state: HomeUiState,
    onCheckup: () -> Unit,
    onOpenTool: (String) -> Unit,
    onSettings: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TOOLIVA", fontWeight = FontWeight.Black)
                        Text(
                            "Your Android toolbox",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                DeviceOverviewCard(state)
            }
            item {
                CheckupCard(onCheckup)
            }
            item {
                Text(
                    text = "Tools",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                )
            }
            items(primaryTools, key = { it.id }) { tool ->
                ToolCard(tool = tool, onClick = { onOpenTool(tool.id) })
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun DeviceOverviewCard(state: HomeUiState) {
    val context = LocalContext.current
    val used = Formatter.formatFileSize(context, state.storageUsedBytes)
    val total = Formatter.formatFileSize(context, state.storageTotalBytes)
    val free = Formatter.formatFileSize(context, state.storageAvailableBytes)
    val percent = (state.storageUsedFraction * 100).toInt()

    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(108.dp)) {
                    CircularProgressIndicator(
                        progress = { state.storageUsedFraction },
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 10.dp,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("$percent%", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                        Text("used", style = MaterialTheme.typography.labelMedium)
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Storage, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  Storage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Text("$used of $total")
                    Text(
                        "$free available",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                MiniStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.BatteryChargingFull,
                    title = "Battery",
                    value = state.batteryPercent?.let { "$it%" } ?: "—",
                )
                MiniStat(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Memory,
                    title = "Thermal",
                    value = state.thermalLabel,
                )
            }
        }
    }
}

@Composable
private fun MiniStat(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Column {
                Text(title, style = MaterialTheme.typography.labelMedium)
                Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CheckupCard(onCheckup: () -> Unit) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(44.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.HealthAndSafety, contentDescription = null, modifier = Modifier.size(30.dp))
                }
                Column(modifier = Modifier.padding(start = 10.dp)) {
                    Text("Phone Checkup", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
                    Text("Storage, battery, sensors and useful insights")
                }
            }
            Button(onClick = onCheckup, modifier = Modifier.fillMaxWidth()) {
                Text("CHECK MY PHONE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ToolCard(tool: HomeTool, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Card(
                shape = CircleShape,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Icon(tool.icon, contentDescription = null, modifier = Modifier.size(24.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(tool.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    tool.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onClick) {
                Text("Open")
            }
        }
    }
}
