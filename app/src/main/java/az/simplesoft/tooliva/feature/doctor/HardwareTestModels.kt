package az.simplesoft.tooliva.feature.doctor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Vibrator
import android.os.VibratorManager
import kotlin.math.abs
import kotlin.math.sqrt

enum class HardwareTestId(val title: String) {
    DISPLAY("Display"),
    TOUCHSCREEN("Touchscreen"),
    VIBRATION("Vibration"),
    FLASHLIGHT("Flashlight"),
    SPEAKER("Speaker"),
    MICROPHONE("Microphone"),
    PROXIMITY("Proximity sensor"),
    ACCELEROMETER("Accelerometer"),
    GYROSCOPE("Gyroscope"),
    COMPASS("Compass / magnetic sensor"),
}

enum class HardwareTestStatus(val label: String) {
    NOT_TESTED("Not tested"),
    RUNNING("Running"),
    PASSED("Passed"),
    FAILED("Problem reported"),
    NOT_SUPPORTED("Not supported"),
    PERMISSION_REQUIRED("Permission required"),
    ERROR("Error"),
}

fun hardwareCapabilityStatus(context: Context, id: HardwareTestId): HardwareTestStatus {
    val app = context.applicationContext
    return when (id) {
        HardwareTestId.VIBRATION -> {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) app.getSystemService(VibratorManager::class.java)?.defaultVibrator else app.getSystemService(Vibrator::class.java)
            if (vibrator?.hasVibrator() == true) HardwareTestStatus.NOT_TESTED else HardwareTestStatus.NOT_SUPPORTED
        }
        HardwareTestId.FLASHLIGHT -> {
            val camera = app.getSystemService(CameraManager::class.java)
            val available = runCatching { camera?.cameraIdList?.any { id -> camera.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true } == true }.getOrDefault(false)
            if (available) HardwareTestStatus.NOT_TESTED else HardwareTestStatus.NOT_SUPPORTED
        }
        HardwareTestId.PROXIMITY -> sensorCapability(app, Sensor.TYPE_PROXIMITY)
        HardwareTestId.ACCELEROMETER -> sensorCapability(app, Sensor.TYPE_ACCELEROMETER)
        HardwareTestId.GYROSCOPE -> sensorCapability(app, Sensor.TYPE_GYROSCOPE)
        HardwareTestId.COMPASS -> sensorCapability(app, Sensor.TYPE_MAGNETIC_FIELD)
        HardwareTestId.MICROPHONE -> if (app.packageManager.hasSystemFeature("android.hardware.microphone")) HardwareTestStatus.NOT_TESTED else HardwareTestStatus.NOT_SUPPORTED
        else -> HardwareTestStatus.NOT_TESTED
    }
}

private fun sensorCapability(context: Context, type: Int): HardwareTestStatus =
    if (context.getSystemService(SensorManager::class.java)?.getDefaultSensor(type) != null) HardwareTestStatus.NOT_TESTED else HardwareTestStatus.NOT_SUPPORTED

data class TouchCoverage(val columns: Int = 8, val rows: Int = 12, val touched: Set<Int> = emptySet()) {
    val total: Int get() = columns * rows
    val count: Int get() = touched.size
    val complete: Boolean get() = count == total

    fun touchCell(column: Int, row: Int): TouchCoverage {
        if (column !in 0 until columns || row !in 0 until rows) return this
        return copy(touched = touched + (row * columns + column))
    }

    fun touchPoint(x: Float, y: Float, width: Float, height: Float): TouchCoverage {
        if (width <= 0f || height <= 0f) return this
        val column = (x / width * columns).toInt().coerceIn(0, columns - 1)
        val row = (y / height * rows).toInt().coerceIn(0, rows - 1)
        return touchCell(column, row)
    }

    fun reset(): TouchCoverage = copy(touched = emptySet())
}

class ProximitySequenceTracker {
    private var sawFar = false
    private var sawNear = false
    private var returnedFar = false

    fun onNear(near: Boolean) {
        if (near) {
            if (sawFar) sawNear = true
        } else if (sawNear) {
            returnedFar = true
        } else {
            sawFar = true
        }
    }

    val completed: Boolean get() = sawFar && sawNear && returnedFar
}

data class SensorSignal(val values: List<Float> = emptyList(), val changed: Boolean = false)

fun microphoneAmplitude(samples: ShortArray, readCount: Int = samples.size): Float {
    val count = readCount.coerceIn(0, samples.size)
    if (count == 0) return 0f
    var sum = 0.0
    for (index in 0 until count) {
        val normalized = samples[index] / 32768.0
        sum += normalized * normalized
    }
    return sqrt(sum / count).toFloat().coerceIn(0f, 1f)
}

fun sensorValuesChanged(previous: List<Float>, current: List<Float>, epsilon: Float = 0.01f): Boolean =
    previous.size != current.size || previous.zip(current).any { (old, new) -> abs(old - new) > epsilon }

fun compassHeadingDegrees(magneticX: Float, magneticY: Float): Float =
    (Math.toDegrees(kotlin.math.atan2(magneticY.toDouble(), magneticX.toDouble())).toFloat() + 360f) % 360f

data class HardwareTestResultRecord(val status: HardwareTestStatus, val timestampMillis: Long)
