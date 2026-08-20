package az.simplesoft.tooliva.feature.doctor

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.os.VibrationEffect
import android.os.VibrationAttributes
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class HardwareTestsUiState(
    val results: Map<HardwareTestId, HardwareTestStatus> = HardwareTestId.values().associateWith { HardwareTestStatus.NOT_TESTED },
    val activeTest: HardwareTestId? = null,
    val touchCoverage: TouchCoverage = TouchCoverage(),
    val sensorSignals: Map<Int, SensorSignal> = emptyMap(),
    val proximityComplete: Boolean = false,
    val torchOn: Boolean = false,
    val microphoneLevel: Float = 0f,
    val microphoneSignalDetected: Boolean = false,
    val microphonePermissionDenied: Boolean = false,
    val hapticFeedbackEnabled: Boolean? = null,
    val errorMessage: String? = null,
) {
    val supportedCount: Int get() = results.count { it.value != HardwareTestStatus.NOT_SUPPORTED }
    val completedSupportedCount: Int get() = results.count { (id, status) -> status != HardwareTestStatus.NOT_SUPPORTED && status in setOf(HardwareTestStatus.PASSED, HardwareTestStatus.FAILED) }
}

class HardwareTestsViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val appContext = application.applicationContext
    private val sensorManager = appContext.getSystemService(SensorManager::class.java)
    private val cameraManager = appContext.getSystemService(CameraManager::class.java)
    private val resultStore = HardwareTestResultStore(appContext)
    private var activeSensor: Sensor? = null
    private var proximityTracker = ProximitySequenceTracker()
    private var audioRecord: AudioRecord? = null
    private var microphoneJob: Job? = null
    private var audioTrack: AudioTrack? = null
    private var torchCameraId: String? = findTorchCameraId()

    private val _uiState = MutableStateFlow(
        HardwareTestsUiState(
            results = HardwareTestId.values().associateWith { id ->
                resultStore.read()[id]?.status ?: hardwareCapabilityStatus(appContext, id)
            },
        ),
    )
    val uiState = _uiState.asStateFlow()

    fun openTest(id: HardwareTestId) {
        stopSensor()
        stopMicrophone()
        stopSpeaker()
        if (hardwareCapabilityStatus(appContext, id) == HardwareTestStatus.NOT_SUPPORTED) {
            setStatus(id, HardwareTestStatus.NOT_SUPPORTED)
        }
        _uiState.update {
            it.copy(
                activeTest = id,
                touchCoverage = if (id == HardwareTestId.TOUCHSCREEN) TouchCoverage() else it.touchCoverage,
                hapticFeedbackEnabled = if (id == HardwareTestId.VIBRATION) systemHapticFeedbackEnabled() else null,
                errorMessage = null,
            )
        }
    }

    fun closeTest() {
        stopSensor()
        stopMicrophone()
        stopSpeaker()
        turnTorch(false)
        _uiState.update { it.copy(activeTest = null, torchOn = false, microphoneLevel = 0f) }
    }

    fun setStatus(id: HardwareTestId, status: HardwareTestStatus) {
        if (status == HardwareTestStatus.PASSED || status == HardwareTestStatus.FAILED) resultStore.write(id, status)
        _uiState.update { it.copy(results = it.results + (id to status)) }
    }

    fun resetResults() {
        stopSensor()
        stopMicrophone()
        stopSpeaker()
        turnTorch(false)
        resultStore.reset()
        _uiState.update { it.copy(results = HardwareTestId.values().associateWith { id -> hardwareCapabilityStatus(appContext, id) }, activeTest = null, touchCoverage = TouchCoverage(), torchOn = false) }
    }

    fun touchAt(x: Float, y: Float, width: Float, height: Float) {
        _uiState.update { it.copy(touchCoverage = it.touchCoverage.touchPoint(x, y, width, height)) }
    }

    fun resetTouch() = _uiState.update { it.copy(touchCoverage = it.touchCoverage.reset()) }

    fun runVibration() {
        val hapticsEnabled = systemHapticFeedbackEnabled()
        _uiState.update { it.copy(hapticFeedbackEnabled = hapticsEnabled, errorMessage = null) }
        if (!hapticsEnabled) {
            _uiState.update { it.copy(errorMessage = "System touch vibration is disabled. Enable Touch feedback in Android settings, then retry.") }
            return
        }
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            appContext.getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else appContext.getSystemService(Vibrator::class.java)
        if (vibrator?.hasVibrator() != true) {
            setStatus(HardwareTestId.VIBRATION, HardwareTestStatus.NOT_SUPPORTED)
            return
        }
        setStatus(HardwareTestId.VIBRATION, HardwareTestStatus.RUNNING)
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val effect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && vibrator.areEffectsSupported(VibrationEffect.EFFECT_HEAVY_CLICK).firstOrNull() != Vibrator.VIBRATION_EFFECT_SUPPORT_NO) {
                    VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
                } else {
                    VibrationEffect.createWaveform(longArrayOf(0L, 90L, 60L, 260L, 90L), intArrayOf(0, 180, 0, 255, 0), -1)
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val attributes = VibrationAttributes.createForUsage(VibrationAttributes.USAGE_HARDWARE_FEEDBACK)
                    vibrator.vibrate(effect, attributes)
                } else {
                    vibrator.vibrate(effect)
                }
            } else @Suppress("DEPRECATION") vibrator.vibrate(420L)
        }.onFailure { _uiState.update { it.copy(errorMessage = "The vibration test could not start.") } }
    }

    fun turnTorch(enabled: Boolean) {
        val id = torchCameraId
        if (id == null) {
            setStatus(HardwareTestId.FLASHLIGHT, HardwareTestStatus.NOT_SUPPORTED)
            return
        }
        runCatching { cameraManager?.setTorchMode(id, enabled) }
            .onSuccess { _uiState.update { it.copy(torchOn = enabled, errorMessage = null) } }
            .onFailure {
                _uiState.update { it.copy(torchOn = false, errorMessage = "Android could not control the flashlight.") }
                setStatus(HardwareTestId.FLASHLIGHT, HardwareTestStatus.ERROR)
            }
    }

    fun playSpeaker() {
        stopSpeaker()
        setStatus(HardwareTestId.SPEAKER, HardwareTestStatus.RUNNING)
        val sampleRate = 44_100
        val durationMs = 500
        val sampleCount = sampleRate * durationMs / 1000
        val samples = ShortArray(sampleCount) { index ->
            (kotlin.math.sin(2.0 * Math.PI * 440.0 * index / sampleRate) * Short.MAX_VALUE * 0.15).toInt().toShort()
        }
        runCatching {
            val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sampleRate).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                .setAudioFormat(format)
                .setBufferSizeInBytes(samples.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
                .also { track -> track.write(samples, 0, samples.size); track.play() }
        }.onFailure { _uiState.update { it.copy(errorMessage = "The speaker test could not start.") } }
    }

    fun stopSpeaker() {
        runCatching { audioTrack?.stop() }
        audioTrack?.release()
        audioTrack = null
    }

    fun startMicrophone() {
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            _uiState.update { it.copy(microphonePermissionDenied = true) }
            setStatus(HardwareTestId.MICROPHONE, HardwareTestStatus.PERMISSION_REQUIRED)
            return
        }
        stopMicrophone()
        val minBuffer = AudioRecord.getMinBufferSize(44_100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (minBuffer <= 0) {
            setStatus(HardwareTestId.MICROPHONE, HardwareTestStatus.ERROR)
            return
        }
        runCatching {
            audioRecord = AudioRecord(MediaRecorder.AudioSource.MIC, 44_100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer.coerceAtLeast(4096))
            audioRecord?.startRecording()
            setStatus(HardwareTestId.MICROPHONE, HardwareTestStatus.RUNNING)
            microphoneJob = viewModelScope.launch(Dispatchers.Default) {
                val buffer = ShortArray(2048)
                while (isActive) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        val level = microphoneAmplitude(buffer, read)
                        _uiState.update { it.copy(microphoneLevel = level, microphoneSignalDetected = it.microphoneSignalDetected || level > 0.02f) }
                    }
                }
            }
        }.onFailure {
            stopMicrophone()
            setStatus(HardwareTestId.MICROPHONE, HardwareTestStatus.ERROR)
        }
    }

    fun stopMicrophone() {
        microphoneJob?.cancel()
        microphoneJob = null
        runCatching { audioRecord?.stop() }
        audioRecord?.release()
        audioRecord = null
        _uiState.update { it.copy(microphoneLevel = 0f) }
    }

    fun startSensorTest(type: Int) {
        stopSensor()
        val sensor = sensorManager?.getDefaultSensor(type)
        if (sensor == null) {
            val id = when (type) {
                Sensor.TYPE_PROXIMITY -> HardwareTestId.PROXIMITY
                Sensor.TYPE_ACCELEROMETER -> HardwareTestId.ACCELEROMETER
                Sensor.TYPE_GYROSCOPE -> HardwareTestId.GYROSCOPE
                Sensor.TYPE_MAGNETIC_FIELD -> HardwareTestId.COMPASS
                else -> null
            }
            if (id != null) setStatus(id, HardwareTestStatus.NOT_SUPPORTED)
            return
        }
        activeSensor = sensor
        proximityTracker = ProximitySequenceTracker()
        _uiState.update { it.copy(sensorSignals = it.sensorSignals + (type to SensorSignal()), proximityComplete = false) }
        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        when (type) {
            Sensor.TYPE_PROXIMITY -> setStatus(HardwareTestId.PROXIMITY, HardwareTestStatus.RUNNING)
            Sensor.TYPE_ACCELEROMETER -> setStatus(HardwareTestId.ACCELEROMETER, HardwareTestStatus.RUNNING)
            Sensor.TYPE_GYROSCOPE -> setStatus(HardwareTestId.GYROSCOPE, HardwareTestStatus.RUNNING)
            Sensor.TYPE_MAGNETIC_FIELD -> setStatus(HardwareTestId.COMPASS, HardwareTestStatus.RUNNING)
        }
    }

    fun stopSensor() {
        sensorManager?.unregisterListener(this)
        activeSensor = null
    }

    override fun onSensorChanged(event: SensorEvent) {
        val values = event.values.toList()
        val old = _uiState.value.sensorSignals[event.sensor.type]?.values.orEmpty()
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) proximityTracker.onNear(event.values.firstOrNull()?.let { it < event.sensor.maximumRange } == true)
        _uiState.update { it.copy(sensorSignals = it.sensorSignals + (event.sensor.type to SensorSignal(values, sensorValuesChanged(old, values))), proximityComplete = proximityTracker.completed) }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    private fun findTorchCameraId(): String? = runCatching {
        cameraManager?.cameraIdList?.firstOrNull { id -> cameraManager.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
    }.getOrNull()

    private fun systemHapticFeedbackEnabled(): Boolean = runCatching {
        Settings.System.getInt(
            appContext.contentResolver,
            Settings.System.HAPTIC_FEEDBACK_ENABLED,
            1,
        ) == 1
    }.getOrDefault(true)

    override fun onCleared() {
        stopSensor()
        stopMicrophone()
        stopSpeaker()
        turnTorch(false)
        super.onCleared()
    }
}
