package az.simplesoft.tooliva.feature.home

import android.app.Application
import android.os.PowerManager
import androidx.lifecycle.AndroidViewModel
import az.simplesoft.tooliva.core.device.DeviceSnapshotProvider
import az.simplesoft.tooliva.core.device.MemorySnapshotProvider
import az.simplesoft.tooliva.core.settings.ScanHistoryRecord
import az.simplesoft.tooliva.core.settings.ToolivaUserDataStore
import az.simplesoft.tooliva.feature.clean.CleanerAnalysisSnapshot
import az.simplesoft.tooliva.feature.clean.CleanerSessionStore
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class HomeUiState(
    val storageTotalBytes: Long = 0,
    val storageUsedBytes: Long = 0,
    val storageAvailableBytes: Long = 0,
    val storageUsedFraction: Float = 0f,
    val batteryPercent: Int? = null,
    val thermalLabel: String = "Unavailable",
    val memoryPressure: String = "Unavailable",
    val actionPlan: CleanerAnalysisSnapshot? = null,
    val scanHistory: List<ScanHistoryRecord> = emptyList(),
)

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val provider = DeviceSnapshotProvider(application)
    private val memoryProvider = MemorySnapshotProvider(application)
    private val userData = ToolivaUserDataStore(application)

    private val _uiState = MutableStateFlow(readState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            launch { CleanerSessionStore.latestFlow.collectLatest { latest -> _uiState.value = _uiState.value.copy(actionPlan = latest) } }
            launch { userData.scanHistory.collectLatest { history -> _uiState.value = _uiState.value.copy(scanHistory = history) } }
        }
    }

    fun refresh() {
        _uiState.value = readState()
    }

    private fun readState(): HomeUiState {
        val snapshot = provider.read()
        val memory = memoryProvider.read()
        return HomeUiState(
            storageTotalBytes = snapshot.storageTotalBytes,
            storageUsedBytes = snapshot.storageUsedBytes,
            storageAvailableBytes = snapshot.storageAvailableBytes,
            storageUsedFraction = snapshot.storageUsedFraction,
            batteryPercent = snapshot.batteryPercent,
            thermalLabel = thermalLabel(snapshot.thermalStatus),
            memoryPressure = memory?.pressureLabel ?: "Unavailable",
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
