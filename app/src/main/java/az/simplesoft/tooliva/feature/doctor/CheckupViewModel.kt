package az.simplesoft.tooliva.feature.doctor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.device.PhoneDoctorProvider
import az.simplesoft.tooliva.core.device.PhoneDoctorSnapshot
import az.simplesoft.tooliva.feature.clean.CleanerAnalysisSnapshot
import az.simplesoft.tooliva.feature.clean.CleanerSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

data class CheckupHardwareSummary(val supported: Int, val completed: Int, val failed: Int)

data class CheckupResult(
    val snapshot: PhoneDoctorSnapshot,
    val attentionItems: List<String>,
    val hardware: CheckupHardwareSummary,
    val checkedAtMillis: Long,
    val cleanerSnapshot: CleanerAnalysisSnapshot?,
    val findings: List<CheckupFinding> = emptyList(),
)

data class CheckupUiState(
    val result: CheckupResult? = null,
    val running: Boolean = false,
    val error: Boolean = false,
)

class CheckupViewModel(application: Application) : AndroidViewModel(application) {
    private val provider = PhoneDoctorProvider(application)
    private val store = HardwareTestResultStore(application)
    private val _uiState = MutableStateFlow(CheckupUiState())
    val uiState = _uiState.asStateFlow()

    fun runCheckup() {
        _uiState.value = CheckupUiState(running = true)
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val snapshot = provider.read()
                val results = store.read()
                val supported = HardwareTestId.values().count { id -> (results[id]?.status ?: hardwareCapabilityStatus(getApplication(), id)) != HardwareTestStatus.NOT_SUPPORTED }
                val completed = results.values.count { it.status == HardwareTestStatus.PASSED || it.status == HardwareTestStatus.FAILED }
                val failed = results.values.count { it.status == HardwareTestStatus.FAILED }
                val checkedAt = System.currentTimeMillis()
                val hardwareSummary = CheckupHardwareSummary(supported, completed, failed)
                val cleanerSnapshot = CleanerSessionStore.latest
                _uiState.value = CheckupUiState(
                    result = CheckupResult(
                        snapshot = snapshot,
                        attentionItems = checkupAttentionItems(snapshot, failed),
                        hardware = hardwareSummary,
                        checkedAtMillis = checkedAt,
                        cleanerSnapshot = cleanerSnapshot,
                        findings = evaluateCheckup(snapshot, hardwareSummary, cleanerSnapshot, checkedAt),
                    ),
                )
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Throwable) {
                _uiState.value = CheckupUiState(error = true)
            }
        }
    }
}
