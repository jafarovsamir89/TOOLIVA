package az.simplesoft.tooliva.feature.doctor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.device.PhoneDoctorProvider
import az.simplesoft.tooliva.core.device.PhoneDoctorSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class CheckupHardwareSummary(val supported: Int, val completed: Int, val failed: Int)

data class CheckupResult(
    val snapshot: PhoneDoctorSnapshot,
    val attentionItems: List<String>,
    val hardware: CheckupHardwareSummary,
)

data class CheckupUiState(val result: CheckupResult? = null, val running: Boolean = false)

class CheckupViewModel(application: Application) : AndroidViewModel(application) {
    private val provider = PhoneDoctorProvider(application)
    private val store = HardwareTestResultStore(application)
    private val _uiState = MutableStateFlow(CheckupUiState())
    val uiState = _uiState.asStateFlow()

    fun runCheckup() {
        _uiState.value = CheckupUiState(running = true)
        viewModelScope.launch(Dispatchers.Default) {
            val snapshot = provider.read()
            val results = store.read()
            val supported = HardwareTestId.values().count { id -> (results[id]?.status ?: hardwareCapabilityStatus(getApplication(), id)) != HardwareTestStatus.NOT_SUPPORTED }
            val completed = results.values.count { it.status == HardwareTestStatus.PASSED || it.status == HardwareTestStatus.FAILED }
            val failed = results.values.count { it.status == HardwareTestStatus.FAILED }
            val attention = buildList {
                if (snapshot.memory?.lowMemory == true) add("Memory pressure is high")
                if (snapshot.thermal.status != null && snapshot.thermal.status != android.os.PowerManager.THERMAL_STATUS_NONE) add("Thermal status: ${snapshot.thermal.label}")
                if (snapshot.battery.health in setOf("Overheat", "Dead", "Over voltage", "Unspecified failure")) add("Android reports a battery health warning")
                if (failed > 0) add("$failed hardware test${if (failed == 1) "" else "s"} reported a problem")
            }
            _uiState.value = CheckupUiState(CheckupResult(snapshot, attention, CheckupHardwareSummary(supported, completed, failed)))
        }
    }
}
