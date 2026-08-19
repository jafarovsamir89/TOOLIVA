@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package az.simplesoft.tooliva.feature.doctor

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.text.format.Formatter
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.Locale

@Composable
fun HardwareTestsRoute(
    onBack: () -> Unit,
    viewModel: HardwareTestsViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var confirmReset by remember { mutableStateOf(false) }
    BackHandler { if (state.activeTest != null) viewModel.closeTest() else onBack() }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        title = { Text("Reset test results?") },
        text = { Text("All user-confirmed hardware results will return to Not tested.") },
        confirmButton = { TextButton(onClick = { confirmReset = false; viewModel.resetResults() }) { Text("Reset") } },
        dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancel") } },
    )
    if (state.activeTest == null) HardwareTestsOverview(state, onBack, viewModel::openTest, { confirmReset = true })
    else HardwareTestDetail(state, state.activeTest!!, viewModel, onBack)
}

@Composable
private fun HardwareTestsOverview(state: HardwareTestsUiState, onBack: () -> Unit, onOpen: (HardwareTestId) -> Unit, onReset: () -> Unit) {
    Scaffold(topBar = {
        TopAppBar(title = { Text("Hardware Tests", fontWeight = FontWeight.Black) }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card(shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Test one component at a time", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Tooliva never auto-passes a physical test. You decide whether the display, sound, touch or sensor behaved correctly.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${state.completedSupportedCount} / ${state.supportedCount} supported tests completed", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            items(HardwareTestId.values().toList(), key = { it.name }) { id ->
                TestListRow(id, state.results[id] ?: HardwareTestStatus.NOT_TESTED, onClick = { onOpen(id) })
            }
            item { OutlinedButton(onClick = onReset, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Refresh, null); Text("Reset test results", Modifier.padding(start = 8.dp)) } }
        }
    }
}

@Composable
private fun TestListRow(id: HardwareTestId, status: HardwareTestStatus, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(imageVector = statusIcon(status), contentDescription = null, modifier = Modifier.size(24.dp), tint = statusColor(status))
            Text(id.title, Modifier.weight(1f), fontWeight = FontWeight.Bold)
            Text(status.label, color = statusColor(status), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun HardwareTestDetail(state: HardwareTestsUiState, id: HardwareTestId, viewModel: HardwareTestsViewModel, onBack: () -> Unit) {
    val sensorType = sensorTypeFor(id)
    DisposableEffect(id) {
        if (sensorType != null) viewModel.startSensorTest(sensorType)
        onDispose { viewModel.stopSensor(); viewModel.stopMicrophone(); viewModel.stopSpeaker(); viewModel.turnTorch(false) }
    }
    Scaffold(topBar = {
        TopAppBar(title = { Text(id.title, fontWeight = FontWeight.Black) }, navigationIcon = { IconButton(onClick = viewModel::closeTest) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Back") } })
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { TestStatusCard(id, state.results[id] ?: HardwareTestStatus.NOT_TESTED) }
            item {
                when (id) {
                    HardwareTestId.DISPLAY -> DisplayTest(viewModel)
                    HardwareTestId.TOUCHSCREEN -> TouchTest(state, viewModel)
                    HardwareTestId.VIBRATION -> VibrationTest(state, viewModel)
                    HardwareTestId.FLASHLIGHT -> FlashlightTest(state, viewModel)
                    HardwareTestId.SPEAKER -> SpeakerTest(state, viewModel)
                    HardwareTestId.MICROPHONE -> MicrophoneTest(state, viewModel)
                    HardwareTestId.PROXIMITY, HardwareTestId.ACCELEROMETER, HardwareTestId.GYROSCOPE, HardwareTestId.COMPASS -> SensorTest(id, state, viewModel)
                }
            }
            item { Spacer(Modifier.height(20.dp)) }
        }
    }
}

@Composable
private fun TestStatusCard(id: HardwareTestId, status: HardwareTestStatus) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) { Icon(imageVector = statusIcon(status), contentDescription = null, modifier = Modifier.size(30.dp), tint = statusColor(status)); Column { Text(id.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Text(status.label, color = statusColor(status)) } }
    }
}

@Composable
private fun DisplayTest(viewModel: HardwareTestsViewModel) {
    var step by remember { mutableIntStateOf(0) }
    val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.White, Color.Black)
    LaunchedEffect(Unit) { viewModel.setStatus(HardwareTestId.DISPLAY, HardwareTestStatus.RUNNING) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tap the color field to show the next display color. Check for uniform color and visible defects.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(300.dp).background(colors[step], RoundedCornerShape(20.dp)).clickable { if (step < colors.lastIndex) step++ }) { Text("${step + 1} / ${colors.size}", Modifier.align(Alignment.BottomCenter).padding(14.dp), color = if (step == 3) Color.Black else Color.White, fontWeight = FontWeight.Bold) }
        if (step == colors.lastIndex) Text("Sequence complete. Did all colors look correct?")
        TestDecisionButtons(HardwareTestId.DISPLAY, viewModel)
    }
}

@Composable
private fun TouchTest(state: HardwareTestsUiState, viewModel: HardwareTestsViewModel) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(Unit) { viewModel.setStatus(HardwareTestId.TOUCHSCREEN, HardwareTestStatus.RUNNING) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Drag your finger over every area. ${state.touchCoverage.count} / ${state.touchCoverage.total} areas touched.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Box(Modifier.fillMaxWidth().height(360.dp).onSizeChanged { size = it }) {
            Canvas(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { point -> viewModel.touchAt(point.x, point.y, size.width.toFloat(), size.height.toFloat()) } }.pointerInput(Unit) { detectDragGestures(onDragStart = { point -> viewModel.touchAt(point.x, point.y, size.width.toFloat(), size.height.toFloat()) }) { change, _ -> viewModel.touchAt(change.position.x, change.position.y, size.width.toFloat(), size.height.toFloat()); change.consume() } }) {
                val cellWidth = this.size.width / state.touchCoverage.columns
                val cellHeight = this.size.height / state.touchCoverage.rows
                for (row in 0 until state.touchCoverage.rows) for (column in 0 until state.touchCoverage.columns) {
                    val index = row * state.touchCoverage.columns + column
                    drawRect(if (index in state.touchCoverage.touched) Color(0xFF65E6C4) else Color(0xFF29333E), topLeft = Offset(column * cellWidth + 2f, row * cellHeight + 2f), size = androidx.compose.ui.geometry.Size(cellWidth - 4f, cellHeight - 4f))
                }
            }
        }
        OutlinedButton(onClick = viewModel::resetTouch, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Refresh, null); Text("Reset coverage", Modifier.padding(start = 8.dp)) }
        TestDecisionButtons(HardwareTestId.TOUCHSCREEN, viewModel)
    }
}

@Composable
private fun VibrationTest(state: HardwareTestsUiState, viewModel: HardwareTestsViewModel) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Press the button for a short vibration. Nothing starts automatically.", color = MaterialTheme.colorScheme.onSurfaceVariant); Button(onClick = viewModel::runVibration, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Vibration, null); Text("Test vibration", Modifier.padding(start = 8.dp)) }; TestDecisionButtons(HardwareTestId.VIBRATION, viewModel) } }

@Composable
private fun FlashlightTest(state: HardwareTestsUiState, viewModel: HardwareTestsViewModel) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Turn the torch on, confirm it visually, then turn it off.", color = MaterialTheme.colorScheme.onSurfaceVariant); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = { viewModel.turnTorch(true) }, Modifier.weight(1f)) { Icon(Icons.Outlined.FlashOn, null); Text("Turn on") }; OutlinedButton(onClick = { viewModel.turnTorch(false) }, Modifier.weight(1f)) { Text("Turn off") } }; Text(if (state.torchOn) "Torch is on" else "Torch is off"); TestDecisionButtons(HardwareTestId.FLASHLIGHT, viewModel) } }

@Composable
private fun SpeakerTest(state: HardwareTestsUiState, viewModel: HardwareTestsViewModel) { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("A short neutral 440 Hz tone is generated locally. Tooliva does not change system volume.", color = MaterialTheme.colorScheme.onSurfaceVariant); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = viewModel::playSpeaker, Modifier.weight(1f)) { Icon(Icons.Outlined.VolumeUp, null); Text("Play test sound") }; OutlinedButton(onClick = viewModel::stopSpeaker, Modifier.weight(1f)) { Text("Stop") } }; TestDecisionButtons(HardwareTestId.SPEAKER, viewModel) } }

@Composable
private fun MicrophoneTest(state: HardwareTestsUiState, viewModel: HardwareTestsViewModel) {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted -> if (granted) viewModel.startMicrophone() else viewModel.setStatus(HardwareTestId.MICROPHONE, HardwareTestStatus.PERMISSION_REQUIRED) }
    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Tooliva uses the microphone only during this user-started test to show a live input level. Audio is not saved, transcribed or uploaded.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(onClick = { if (granted) viewModel.startMicrophone() else launcher.launch(Manifest.permission.RECORD_AUDIO) }, Modifier.fillMaxWidth()) { Icon(Icons.Outlined.Mic, null); Text(if (state.microphoneLevel > 0f) "Listening…" else "Start microphone test", Modifier.padding(start = 8.dp)) }
        Text("Input level", fontWeight = FontWeight.Bold)
        CircularProgressIndicator(progress = { state.microphoneLevel }, Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary)
        Text(if (state.microphoneSignalDetected) "Signal detected" else "Speak near the phone to check for a changing signal.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        TestDecisionButtons(HardwareTestId.MICROPHONE, viewModel)
    }
}

@Composable
private fun SensorTest(id: HardwareTestId, state: HardwareTestsUiState, viewModel: HardwareTestsViewModel) {
    val type = sensorTypeFor(id)
    val signal = type?.let { state.sensorSignals[it] }
    val values = signal?.values.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(sensorInstruction(id), color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (type == null || state.results[id] == HardwareTestStatus.NOT_SUPPORTED) Text("This hardware is not supported on this device.")
        else {
            Text(if (values.isEmpty()) "Waiting for sensor events…" else values.mapIndexed { index, value -> "${('x'.code + index).toChar()} %.3f".format(Locale.US, value) }.joinToString("   "), style = MaterialTheme.typography.titleLarge)
            if (id == HardwareTestId.COMPASS && values.size >= 2) Text("Heading %.0f°".format(Locale.US, compassHeadingDegrees(values[0], values[1])), color = MaterialTheme.colorScheme.primary)
            if (id == HardwareTestId.PROXIMITY) Text(if (state.proximityComplete) "Far → Near → Far sequence detected" else "Cover and uncover the top of the phone.")
            else Text(if (signal?.changed == true) "Signal detected" else "Move the phone to check for changing values.")
            TestDecisionButtons(id, viewModel)
        }
    }
}

@Composable
private fun TestDecisionButtons(id: HardwareTestId, viewModel: HardwareTestsViewModel) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { Button(onClick = { viewModel.setStatus(id, HardwareTestStatus.PASSED) }, Modifier.weight(1f)) { Icon(Icons.Outlined.CheckCircle, null); Text("Pass", Modifier.padding(start = 6.dp)) }; OutlinedButton(onClick = { viewModel.setStatus(id, HardwareTestStatus.FAILED) }, Modifier.weight(1f)) { Icon(Icons.Outlined.ErrorOutline, null); Text("Problem", Modifier.padding(start = 6.dp)) } }
}

private fun sensorTypeFor(id: HardwareTestId): Int? = when (id) {
    HardwareTestId.PROXIMITY -> Sensor.TYPE_PROXIMITY
    HardwareTestId.ACCELEROMETER -> Sensor.TYPE_ACCELEROMETER
    HardwareTestId.GYROSCOPE -> Sensor.TYPE_GYROSCOPE
    HardwareTestId.COMPASS -> Sensor.TYPE_MAGNETIC_FIELD
    else -> null
}

private fun sensorInstruction(id: HardwareTestId): String = when (id) {
    HardwareTestId.PROXIMITY -> "Cover the top of the phone, then uncover it."
    HardwareTestId.ACCELEROMETER -> "Tilt the phone in different directions and watch x/y/z."
    HardwareTestId.GYROSCOPE -> "Rotate the phone and watch angular motion values."
    HardwareTestId.COMPASS -> "Rotate the phone slowly. Move it in a figure-eight if readings seem unstable."
    else -> ""
}

private fun statusIcon(status: HardwareTestStatus) = when (status) {
    HardwareTestStatus.PASSED -> Icons.Outlined.CheckCircle
    HardwareTestStatus.FAILED, HardwareTestStatus.ERROR -> Icons.Outlined.ErrorOutline
    HardwareTestStatus.RUNNING -> Icons.Outlined.PlayArrow
    else -> Icons.Outlined.Sensors
}

@Composable
private fun statusColor(status: HardwareTestStatus) = when (status) {
    HardwareTestStatus.PASSED -> MaterialTheme.colorScheme.primary
    HardwareTestStatus.FAILED, HardwareTestStatus.ERROR -> MaterialTheme.colorScheme.error
    HardwareTestStatus.PERMISSION_REQUIRED -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}
