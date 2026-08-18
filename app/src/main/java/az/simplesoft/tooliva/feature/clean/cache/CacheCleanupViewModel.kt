package az.simplesoft.tooliva.feature.clean.cache

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import az.simplesoft.tooliva.core.cache.CacheCleanupAvailability
import az.simplesoft.tooliva.core.cache.CacheCleanupResult
import az.simplesoft.tooliva.core.cache.CacheCleanupSupport
import az.simplesoft.tooliva.core.cache.SystemCacheCleanupLauncher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CacheCleanupUiState(
    val availability: CacheCleanupAvailability = CacheCleanupAvailability.UNSUPPORTED,
    val awaitingSystemResult: Boolean = false,
    val result: CacheCleanupResult? = null,
    val launchError: String? = null,
)

class CacheCleanupViewModel(application: Application) : AndroidViewModel(application) {
    private val launcher = SystemCacheCleanupLauncher(application)
    private val _uiState = MutableStateFlow(CacheCleanupUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        val availability = launcher.availability()
        _uiState.update { state ->
            state.copy(
                availability = availability,
                launchError = null,
            )
        }
    }

    fun actionIntent() = launcher.launchIntent()

    fun beginLaunch(): Boolean {
        val state = _uiState.value
        if (!CacheCleanupSupport.canBeginLaunch(state.availability, state.awaitingSystemResult)) return false
        _uiState.update { it.copy(awaitingSystemResult = true, result = null, launchError = null) }
        return true
    }

    fun launchFailed(message: String) {
        _uiState.update {
            it.copy(
                awaitingSystemResult = false,
                result = CacheCleanupResult.FAILED,
                launchError = message,
            )
        }
    }

    fun onSystemResult(resultCode: Int) {
        _uiState.update {
            it.copy(
                awaitingSystemResult = false,
                result = CacheCleanupSupport.mapResult(resultCode),
                launchError = null,
            )
        }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null, launchError = null) }
    }
}
