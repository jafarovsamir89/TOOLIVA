@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package az.simplesoft.tooliva.feature.doctor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.DeveloperBoard
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import az.simplesoft.tooliva.core.device.BatterySnapshot
import az.simplesoft.tooliva.core.device.DisplaySnapshot
import az.simplesoft.tooliva.core.device.MemorySnapshot
import az.simplesoft.tooliva.core.device.PhoneDoctorSnapshot
import az.simplesoft.tooliva.core.device.SensorSnapshot
import az.simplesoft.tooliva.core.device.StorageVolumeSnapshot
import java.util.Locale

@Composable
fun PhoneDoctorRoute(
    onBack: () -> Unit,
    onHardwareTests: () -> Unit,
    onCheckup: () -> Unit,
    onOpenStorageTool: (String) -> Unit,
    viewModel: PhoneDoctorViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackHandler(onBack = onBack)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refresh() }
    PhoneDoctorScreen(state.snapshot, state.selectedSensorType, onBack, onHardwareTests, onCheckup, onOpenStorageTool, viewModel::selectSensor)
}

@Composable
private fun PhoneDoctorScreen(
    snapshot: PhoneDoctorSnapshot?,
    selectedSensorType: Int?,
    onBack: () -> Unit,
    onHardwareTests: () -> Unit,
    onCheckup: () -> Unit,
    onOpenStorageTool: (String) -> Unit,
    onSelectSensor: (Int?) -> Unit,
) {
    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Phone Doctor", fontWeight = FontWeight.Black) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } },
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (snapshot == null) item { Text("Device information unavailable") }
            else {
                item { HeaderCard(snapshot) }
                item { ActionCard(onHardwareTests, onCheckup) }
                item { DeviceCard(snapshot) }
                item { SystemCard(snapshot) }
                item { MemoryCard(snapshot.memory) }
                item { StorageCard(snapshot.storage, onOpenStorageTool) }
                item { BatteryCard(snapshot.battery) }
                item { SimpleCard("Thermal", Icons.Outlined.Speed) { DetailRow("System thermal status", snapshot.thermal.label) } }
                item { DisplayCard(snapshot.display) }
                item {
                    Text("Sensors · ${snapshot.sensors.size} available", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                if (selectedSensorType != null) {
                    snapshot.sensors.firstOrNull { it.type == selectedSensorType }?.let { sensor ->
                        item { SensorLiveCard(sensor, onClose = { onSelectSensor(null) }) }
                    }
                }
                items(snapshot.sensors, key = { "${it.type}-${it.name}-${it.vendor}" }) { sensor ->
                    SensorRow(sensor, selected = selectedSensorType == sensor.type, onClick = { onSelectSensor(sensor.type) })
                }
                item { Spacer(Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun HeaderCard(snapshot: PhoneDoctorSnapshot) {
    Card(shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.PhoneAndroid, null, Modifier.size(34.dp))
                Column { Text(snapshot.device.model, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black); Text("Android ${snapshot.system.androidVersion}") }
            }
            Text("Real device facts from local Android APIs. No health score or background scan.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ActionCard(onHardwareTests: () -> Unit, onCheckup: () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onHardwareTests, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Sensors, null); Text("Run hardware tests", Modifier.padding(start = 8.dp)) }
            Button(onClick = onCheckup, Modifier.fillMaxWidth()) { Text("Check my phone") }
        }
    }
}

@Composable
private fun DeviceCard(snapshot: PhoneDoctorSnapshot) = SimpleCard("Device", Icons.Outlined.PhoneAndroid) {
    DetailRow("Manufacturer", snapshot.device.manufacturer); DetailRow("Brand", snapshot.device.brand); DetailRow("Model", snapshot.device.model)
    DetailRow("Device / product", "${snapshot.device.device} / ${snapshot.device.product}"); DetailRow("Hardware / board", "${snapshot.device.hardware} / ${snapshot.device.board}")
}

@Composable
private fun SystemCard(snapshot: PhoneDoctorSnapshot) = SimpleCard("Android / system", Icons.Outlined.DeveloperBoard) {
    DetailRow("Android version", snapshot.system.androidVersion); DetailRow("SDK level", snapshot.system.sdkLevel.toString()); DetailRow("Security patch", snapshot.system.securityPatch)
    DetailRow("Build", snapshot.system.buildDisplay); DetailRow("Supported ABIs", snapshot.system.supportedAbis.ifEmpty { listOf("Unavailable") }.joinToString())
}

@Composable
private fun MemoryCard(memory: MemorySnapshot?) = SimpleCard("Memory", Icons.Outlined.Memory) {
    if (memory == null) Text("Memory information unavailable", color = MaterialTheme.colorScheme.onSurfaceVariant)
    else { val c = LocalContext.current; DetailRow("Available memory", formatBytes(c, memory.availableBytes)); DetailRow("Used estimate", formatBytes(c, memory.usedEstimateBytes)); DetailRow("Total memory", formatBytes(c, memory.totalBytes)); DetailRow("Pressure", memory.pressureLabel); DetailRow("Low-memory threshold", formatBytes(c, memory.thresholdBytes)) }
}

@Composable
private fun StorageCard(volumes: List<StorageVolumeSnapshot>, onOpenStorageTool: (String) -> Unit) = SimpleCard("Storage", Icons.Outlined.Storage) {
    val c = LocalContext.current
    volumes.forEach { volume ->
        Text(volume.label, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        DetailRow("Available / total", "${formatBytes(c, volume.availableBytes)} / ${formatBytes(c, volume.totalBytes)}")
        DetailRow("Used", formatBytes(c, volume.usedBytes))
    }
    if (volumes.isEmpty()) Text("Storage information unavailable")
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        TextButton(onClick = { onOpenStorageTool("files") }) { Text("Open Files") }
        TextButton(onClick = { onOpenStorageTool("large-files") }) { Text("Large Files") }
        TextButton(onClick = { onOpenStorageTool("duplicates") }) { Text("Duplicates") }
    }
}

@Composable
private fun BatteryCard(battery: BatterySnapshot) = SimpleCard("Battery", Icons.Outlined.BatteryChargingFull) {
    DetailRow("Level", battery.levelPercent?.let { "$it%" } ?: "Unavailable")
    DetailRow("State", battery.status); DetailRow("Power source", battery.powerSource)
    DetailRow("Temperature", battery.temperatureCelsius?.let { "%.1f °C".format(Locale.US, it) } ?: "Unavailable")
    DetailRow("Voltage", battery.voltageVolts?.let { "%.3f V".format(Locale.US, it) } ?: "Unavailable")
    DetailRow("Technology", battery.technology ?: "Unavailable"); DetailRow("Android battery health", battery.health ?: "Unavailable")
    DetailRow("Current now", battery.currentNowMilliamps?.let { "%.0f mA".format(Locale.US, it) } ?: "Unavailable")
    DetailRow("Current average", battery.currentAverageMilliamps?.let { "%.0f mA".format(Locale.US, it) } ?: "Unavailable")
}

@Composable
private fun DisplayCard(display: DisplaySnapshot) = SimpleCard("Display", Icons.Outlined.PhoneAndroid) {
    DetailRow("Current dimensions", if (display.widthPixels != null && display.heightPixels != null) "${display.widthPixels} × ${display.heightPixels} px" else "Unavailable")
    DetailRow("Density", display.density?.let { "%.2fx".format(Locale.US, it) } ?: "Unavailable")
    DetailRow("Density dpi", display.densityDpi?.toString() ?: "Unavailable")
    DetailRow("Refresh rate", display.refreshRateHz?.let { "%.1f Hz".format(Locale.US, it) } ?: "Unavailable")
}

@Composable
private fun SimpleCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable () -> Unit) {
    Card(shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary); Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
            content()
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f)); Text(value, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f)) }
}

@Composable
private fun SensorRow(sensor: SensorSnapshot, selected: Boolean, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(sensor.name, fontWeight = FontWeight.Bold); Text("${sensor.typeLabel} · ${sensor.group.label} · ${sensor.vendor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text("Range ${sensor.maxRange} · Resolution ${sensor.resolution} · Power ${sensor.powerMa} mA", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun SensorLiveCard(sensor: SensorSnapshot, onClose: () -> Unit) {
    val context = LocalContext.current
    var values by remember(sensor.type) { mutableStateOf(emptyList<Float>()) }
    DisposableEffect(sensor.type) {
        val manager = context.getSystemService(SensorManager::class.java)
        val source = manager?.getDefaultSensor(sensor.type)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) { values = event.values.toList() }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        if (manager != null && source != null) manager.registerListener(listener, source, SensorManager.SENSOR_DELAY_UI)
        onDispose { manager?.unregisterListener(listener) }
    }
    SimpleCard("Live values · ${sensor.typeLabel}", Icons.Outlined.Sensors) {
        Text(if (values.isEmpty()) "Waiting for sensor events…" else values.mapIndexed { index, value -> "${('x'.code + index).toChar()} %.3f".format(Locale.US, value) }.joinToString("   "), style = MaterialTheme.typography.titleMedium)
        Text("Sensor is active only while this detail is visible.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = onClose) { Text("Close live values") }
    }
}

private fun formatBytes(context: android.content.Context, bytes: Long): String = Formatter.formatFileSize(context, bytes.coerceAtLeast(0L))
