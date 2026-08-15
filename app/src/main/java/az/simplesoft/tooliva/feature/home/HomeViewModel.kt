package az.simplesoft.tooliva.feature.home

import android.app.Application
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import az.simplesoft.tooliva.core.device.DeviceSnapshotProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeUiState(
    val storageTotalBytes: Long = 0,
    val storageUsedBytes: Long = 0,
    val storageAvailableBytes: Long = 0,
    val storageUsedFraction: Float = 0f,
    val batteryPercent: Int? = null,
    val thermalLabel: String = "Unavailable",
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val provider = DeviceSnapshotProvider(application)

    private val _uiState = MutableStateFlow(readState())
    val uiState = _uiState.asStateFlow()

    fun refresh() {
        _uiState.value = readState()
    }

    private fun readState(): HomeUiState {
        val snapshot = provider.read()
        return HomeUiState(
            storageTotalBytes = snapshot.storageTotalBytes,
            storageUsedBytes = snapshot.storageUsedBytes,
            storageAvailableBytes = snapshot.storageAvailableBytes,
            storageUsedFraction = snapshot.storageUsedFraction,
            batteryPercent = snapshot.batteryPercent,
            thermalLabel = thermalLabel(snapshot.thermalStatus),
        )
    }

    private fun thermalLabel(status: Int?): String = when (status) {
        PowerManager.THERMAL_STATUS_NONE -> "Normal"
        PowerManager.THERMAL_STATUS_LIGHT -> "Slightly warm"
        PowerManager.THERMAL_STATUS_MODERATE -> "Warm"
        PowerManager.THERMAL_STATUS_SEVERE -> "Hot"
        PowerManager.THERMAL_STATUS_CRITICAL -> "Critical"
        PowerManager.THERMAL_STATUS_EMERGENCY -> "Emergency"
        PowerManager.THERMAL_STATUS_SHUTDOWN -> "Shutdown risk"
        null -> "Unavailable"
        else -> "Unknown"
    }
}
