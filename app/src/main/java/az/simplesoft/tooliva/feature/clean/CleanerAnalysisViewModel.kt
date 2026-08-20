package az.simplesoft.tooliva.feature.clean

import android.app.Application
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.MediaStoreStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import az.simplesoft.tooliva.core.storage.StorageProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CleanerAnalysisStatus { IDLE, ANALYZING, COMPLETE, CANCELLED, ERROR }

data class CleanerAnalysisUiState(
    val status: CleanerAnalysisStatus = CleanerAnalysisStatus.IDLE,
    val accessState: StorageAccessState = StorageAccessState(
        fullStorageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        allFilesAccessGranted = false,
    ),
    val snapshot: CleanerAnalysisSnapshot = CleanerAnalysisSnapshot(),
    val errorMessage: String? = null,
) {
    val isAnalyzing: Boolean get() = status == CleanerAnalysisStatus.ANALYZING
}

class CleanerAnalysisViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val _uiState = MutableStateFlow(CleanerAnalysisUiState(accessState = accessCoordinator.currentState()))
    val uiState = _uiState.asStateFlow()
    private var scanJob: Job? = null

    fun refreshAccess() {
        val latest = accessCoordinator.currentState()
        _uiState.update { state ->
            if (state.accessState == latest) state else state.copy(accessState = latest, status = CleanerAnalysisStatus.IDLE, snapshot = CleanerAnalysisSnapshot(), errorMessage = null)
        }
    }

    fun analyze() {
        if (_uiState.value.isAnalyzing) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val accumulator = CleanerAnalysisAccumulator(System.currentTimeMillis())
            var lastUiPublishAt = 0L
            fun publishProgressIfDue(force: Boolean = false) {
                val now = SystemClock.uptimeMillis()
                if (force || now - lastUiPublishAt >= UI_UPDATE_INTERVAL_MS) {
                    lastUiPublishAt = now
                    publish(accumulator.progressSnapshot())
                }
            }
            _uiState.update { it.copy(status = CleanerAnalysisStatus.ANALYZING, snapshot = CleanerAnalysisSnapshot(), errorMessage = null) }
            try {
                provider().scan(0L).collect { event ->
                    when (event) {
                        StorageScanEvent.Started -> Unit
                        is StorageScanEvent.EntryFound -> {
                            accumulator.addFile(event.entry)
                            publishProgressIfDue()
                        }
                        is StorageScanEvent.DirectoryVisited -> {
                            accumulator.addDirectory(event.path, event.isEmpty)
                            publishProgressIfDue()
                        }
                        is StorageScanEvent.Progress -> publishProgressIfDue()
                        is StorageScanEvent.Warning -> {
                            accumulator.warning()
                            publishProgressIfDue()
                        }
                        StorageScanEvent.Completed -> Unit
                    }
                }
                val complete = accumulator.snapshot()
                CleanerSessionStore.latest = complete
                _uiState.update { it.copy(status = CleanerAnalysisStatus.COMPLETE, snapshot = complete) }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(status = CleanerAnalysisStatus.CANCELLED, snapshot = accumulator.snapshot(cancelled = true)) }
            } catch (_: SecurityException) {
                _uiState.update {
                    it.copy(
                        status = CleanerAnalysisStatus.ERROR,
                        snapshot = accumulator.snapshot(),
                        errorMessage = if (it.accessState.mode == StorageAccessMode.FULL) {
                            "Full Storage Access was revoked. Grant it again to analyze shared storage."
                        } else {
                            "Media access is unavailable. Grant access or use Full Storage Mode."
                        },
                    )
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(status = CleanerAnalysisStatus.ERROR, snapshot = accumulator.snapshot(), errorMessage = error.message ?: "Unable to analyze accessible storage.") }
            }
        }
    }

    fun cancelAnalyze() { scanJob?.cancel() }

    private fun publish(snapshot: CleanerAnalysisSnapshot) {
        _uiState.update { it.copy(snapshot = snapshot) }
    }

    private fun provider(): StorageProvider = if (_uiState.value.accessState.mode == StorageAccessMode.FULL) {
        FullStorageProvider(getApplication())
    } else {
        MediaStoreStorageProvider(getApplication())
    }

    private companion object {
        const val UI_UPDATE_INTERVAL_MS = 250L
    }
}
