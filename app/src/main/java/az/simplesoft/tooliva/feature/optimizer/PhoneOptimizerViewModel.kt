package az.simplesoft.tooliva.feature.optimizer

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import az.simplesoft.tooliva.core.cache.CacheCleanupAvailability
import az.simplesoft.tooliva.core.cache.CacheCleanupResult
import az.simplesoft.tooliva.core.cache.CacheCleanupSupport
import az.simplesoft.tooliva.core.cache.SystemCacheCleanupLauncher
import az.simplesoft.tooliva.core.device.DeviceSnapshot
import az.simplesoft.tooliva.core.device.DeviceSnapshotProvider
import az.simplesoft.tooliva.core.device.MemorySnapshot
import az.simplesoft.tooliva.core.device.MemorySnapshotProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class PhoneOptimizerUiState(
    val availability: CacheCleanupAvailability = CacheCleanupAvailability.UNSUPPORTED,
    val memory: MemorySnapshot? = null,
    val storage: DeviceSnapshot? = null,
    val beforeMemory: MemorySnapshot? = null,
    val beforeStorage: DeviceSnapshot? = null,
    val afterMemory: MemorySnapshot? = null,
    val afterStorage: DeviceSnapshot? = null,
    val awaitingSystemResult: Boolean = false,
    val result: CacheCleanupResult? = null,
    val launchError: String? = null,
)

class PhoneOptimizerViewModel(application: Application) : AndroidViewModel(application) {
    private val launcher = SystemCacheCleanupLauncher(application)
    private val memoryProvider = MemorySnapshotProvider(application)
    private val deviceProvider = DeviceSnapshotProvider(application)
    private val _uiState = MutableStateFlow(PhoneOptimizerUiState())
    val uiState = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        val memory = memoryProvider.read()
        val storage = deviceProvider.read()
        _uiState.update {
            it.copy(
                availability = launcher.availability(),
                memory = memory,
                storage = storage,
                launchError = null,
            )
        }
    }

    fun actionIntent() = launcher.launchIntent()

    fun beginOptimize(): Boolean {
        val state = _uiState.value
        if (!CacheCleanupSupport.canBeginLaunch(state.availability, state.awaitingSystemResult)) return false
        _uiState.update {
            it.copy(
                awaitingSystemResult = true,
                beforeMemory = memoryProvider.read(),
                beforeStorage = deviceProvider.read(),
                result = null,
                launchError = null,
            )
        }
        return true
    }

    fun onSystemResult(resultCode: Int) {
        _uiState.update {
            it.copy(
                awaitingSystemResult = false,
                result = CacheCleanupSupport.mapResult(resultCode),
                afterMemory = memoryProvider.read(),
                afterStorage = deviceProvider.read(),
            )
        }
    }

    fun launchFailed(message: String) {
        _uiState.update { it.copy(awaitingSystemResult = false, result = CacheCleanupResult.FAILED, launchError = message) }
    }

    fun clearResult() {
        _uiState.update { it.copy(result = null, launchError = null) }
        refresh()
    }

    fun mapResultForTest(resultCode: Int): CacheCleanupResult = CacheCleanupSupport.mapResult(resultCode)
}
