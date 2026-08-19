package az.simplesoft.tooliva.feature.doctor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import az.simplesoft.tooliva.core.device.PhoneDoctorProvider
import az.simplesoft.tooliva.core.device.PhoneDoctorSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class PhoneDoctorUiState(
    val snapshot: PhoneDoctorSnapshot? = null,
    val selectedSensorType: Int? = null,
) 

class PhoneDoctorViewModel(application: Application) : AndroidViewModel(application) {
    private val provider = PhoneDoctorProvider(application)
    private val _uiState = MutableStateFlow(PhoneDoctorUiState(snapshot = provider.read()))
    val uiState = _uiState.asStateFlow()

    fun refresh() { _uiState.value = _uiState.value.copy(snapshot = provider.read()) }
    fun selectSensor(type: Int?) { _uiState.value = _uiState.value.copy(selectedSensorType = type) }
}
