package az.simplesoft.tooliva.feature.clean.index

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.index.StorageCategorySummary
import az.simplesoft.tooliva.core.storage.index.StorageIndexCoordinator
import az.simplesoft.tooliva.core.storage.index.StorageIndexCoordinatorState
import az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus
import az.simplesoft.tooliva.core.storage.index.StorageIndexScanPhase
import az.simplesoft.tooliva.core.storage.index.StorageIndexRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StorageIndexUiState(
    val accessState: StorageAccessState = StorageAccessState(
        fullStorageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        allFilesAccessGranted = false,
    ),
    val status: StorageIndexRunStatus = StorageIndexRunStatus.IDLE,
    val phase: StorageIndexScanPhase = StorageIndexScanPhase.IDLE,
    val filesDiscovered: Long = 0L,
    val foldersVisited: Long = 0L,
    val indexedBytes: Long = 0L,
    val warningCount: Int = 0,
    val elapsedMillis: Long = 0L,
    val fastScanElapsedMillis: Long? = null,
    val firstResultCount: Int = 0,
    val lastScanAtMillis: Long? = null,
    val errorMessage: String? = null,
    val categorySummaries: List<StorageCategorySummary> = emptyList(),
)

class StorageIndexViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val repository = StorageIndexRepository(application)
    private val coordinator = StorageIndexCoordinator.getInstance(application)
    private val _uiState = MutableStateFlow(
        StorageIndexUiState(accessState = accessCoordinator.currentState()),
    )
    val uiState = _uiState.asStateFlow()
    private var lastSummaryFiles = -1L
    private var lastSummaryPhase = StorageIndexScanPhase.IDLE
    private var summaryJob: Job? = null

    init {
        viewModelScope.launch {
            coordinator.state.collectLatest { coordinatorState ->
                applyCoordinatorState(coordinatorState)
            }
        }
        refresh()
    }

    fun refresh() {
        val accessState = accessCoordinator.currentState()
        val previousAccess = _uiState.value.accessState
        if (previousAccess.mode != accessState.mode && coordinator.state.value.phase != StorageIndexScanPhase.IDLE) {
            coordinator.cancel()
        }
        _uiState.update { it.copy(accessState = accessState) }
        viewModelScope.launch { refreshCachedState(accessState.mode) }
    }

    fun scan() {
        val accessState = accessCoordinator.currentState()
        _uiState.update { it.copy(accessState = accessState, errorMessage = null) }
        coordinator.start(accessState.mode)
    }

    fun cancelScan() {
        coordinator.cancel()
    }

    private suspend fun refreshCachedState(mode: StorageAccessMode) {
        if (coordinator.state.value.phase != StorageIndexScanPhase.IDLE && coordinator.state.value.accessMode == mode) return
        val latest = repository.lastSuccessfulScan(mode)
        val summaries = repository.categorySummaries(mode, MIN_LARGE_FILE_BYTES)
        _uiState.update { state ->
            state.copy(
                status = latest?.let { StorageIndexRunStatus.COMPLETED } ?: StorageIndexRunStatus.IDLE,
                phase = StorageIndexScanPhase.IDLE,
                filesDiscovered = latest?.filesDiscovered ?: 0L,
                foldersVisited = latest?.foldersVisited ?: 0L,
                indexedBytes = latest?.indexedBytes ?: 0L,
                warningCount = latest?.warningCount ?: 0,
                elapsedMillis = latest?.let { (it.completedAtMillis ?: it.startedAtMillis) - it.startedAtMillis } ?: 0L,
                lastScanAtMillis = latest?.completedAtMillis,
                errorMessage = null,
                categorySummaries = summaries,
            )
        }
    }

    private fun applyCoordinatorState(coordinatorState: StorageIndexCoordinatorState) {
        val currentMode = _uiState.value.accessState.mode
        if (coordinatorState.accessMode != null && coordinatorState.accessMode != currentMode) return
        _uiState.update {
            it.copy(
                status = coordinatorState.status,
                phase = coordinatorState.phase,
                filesDiscovered = coordinatorState.filesDiscovered,
                foldersVisited = coordinatorState.foldersVisited,
                indexedBytes = coordinatorState.indexedBytes,
                warningCount = coordinatorState.warningCount,
                elapsedMillis = coordinatorState.elapsedMillis,
                fastScanElapsedMillis = coordinatorState.fastScanElapsedMillis,
                firstResultCount = coordinatorState.firstResultCount,
                errorMessage = if (coordinatorState.status == StorageIndexRunStatus.FAILED) coordinatorState.message else null,
                lastScanAtMillis = if (coordinatorState.status == StorageIndexRunStatus.COMPLETED) {
                    System.currentTimeMillis()
                } else {
                    it.lastScanAtMillis
                },
            )
        }
        val phaseChanged = coordinatorState.phase != lastSummaryPhase
        val enoughNewFiles = coordinatorState.filesDiscovered - lastSummaryFiles >= SUMMARY_REFRESH_INTERVAL
        if (phaseChanged || enoughNewFiles) {
            lastSummaryFiles = coordinatorState.filesDiscovered
            lastSummaryPhase = coordinatorState.phase
            summaryJob?.cancel()
            summaryJob = viewModelScope.launch {
                val summaries = repository.categorySummaries(currentMode, MIN_LARGE_FILE_BYTES)
                _uiState.update { it.copy(categorySummaries = summaries) }
            }
        }
        if (coordinatorState.phase == StorageIndexScanPhase.IDLE && coordinatorState.status == StorageIndexRunStatus.COMPLETED) {
            viewModelScope.launch { refreshCachedState(currentMode) }
        }
    }

    companion object {
        private const val MIN_LARGE_FILE_BYTES = 100L * 1024L * 1024L
        private const val SUMMARY_REFRESH_INTERVAL = 512L
    }
}
