package az.simplesoft.tooliva.feature.clean.index

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.MediaStoreStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.index.StorageIndexProgress
import az.simplesoft.tooliva.core.storage.index.StorageIndexRepository
import az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StorageIndexUiState(
    val accessState: StorageAccessState = StorageAccessState(
        fullStorageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        allFilesAccessGranted = false,
    ),
    val status: StorageIndexRunStatus = StorageIndexRunStatus.IDLE,
    val filesDiscovered: Long = 0L,
    val foldersVisited: Long = 0L,
    val indexedBytes: Long = 0L,
    val warningCount: Int = 0,
    val currentPath: String? = null,
    val elapsedMillis: Long = 0L,
    val lastScanAtMillis: Long? = null,
    val errorMessage: String? = null,
)

class StorageIndexViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val repository = StorageIndexRepository(application)
    private val _uiState = MutableStateFlow(
        StorageIndexUiState(accessState = accessCoordinator.currentState()),
    )
    val uiState = _uiState.asStateFlow()
    private var scanJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        val accessState = accessCoordinator.currentState()
        val previousAccess = _uiState.value.accessState
        if (previousAccess.mode != accessState.mode && scanJob?.isActive == true) {
            scanJob?.cancel()
        }
        _uiState.update { it.copy(accessState = accessState) }
        viewModelScope.launch {
            val latest = repository.lastSuccessfulScan(accessState.mode)
            _uiState.update { state ->
                if (state.status == StorageIndexRunStatus.SCANNING) {
                    state
                } else {
                    state.copy(
                        status = latest?.let { StorageIndexRunStatus.COMPLETED } ?: StorageIndexRunStatus.IDLE,
                        filesDiscovered = latest?.filesDiscovered ?: 0L,
                        foldersVisited = latest?.foldersVisited ?: 0L,
                        indexedBytes = latest?.indexedBytes ?: 0L,
                        warningCount = latest?.warningCount ?: 0,
                        elapsedMillis = latest?.let { (it.completedAtMillis ?: it.startedAtMillis) - it.startedAtMillis } ?: 0L,
                        lastScanAtMillis = latest?.completedAtMillis,
                        errorMessage = null,
                    )
                }
            }
        }
    }

    fun scan() {
        if (scanJob?.isActive == true) return
        val accessState = accessCoordinator.currentState()
        _uiState.update {
            it.copy(
                accessState = accessState,
                status = StorageIndexRunStatus.SCANNING,
                filesDiscovered = 0L,
                foldersVisited = 0L,
                indexedBytes = 0L,
                warningCount = 0,
                currentPath = null,
                elapsedMillis = 0L,
                errorMessage = null,
            )
        }
        scanJob = viewModelScope.launch {
            try {
                repository.index(provider(accessState.mode), ::applyProgress)
            } catch (_: CancellationException) {
                _uiState.update { it.copy(status = StorageIndexRunStatus.CANCELED, errorMessage = null) }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    private fun provider(mode: StorageAccessMode) = if (mode == StorageAccessMode.FULL) {
        FullStorageProvider(getApplication())
    } else {
        MediaStoreStorageProvider(getApplication())
    }

    private fun applyProgress(progress: StorageIndexProgress) {
        _uiState.update {
            it.copy(
                status = progress.status,
                filesDiscovered = progress.filesDiscovered,
                foldersVisited = progress.foldersVisited,
                indexedBytes = progress.indexedBytes,
                warningCount = progress.warningCount,
                currentPath = progress.currentPath,
                elapsedMillis = progress.elapsedMillis,
                errorMessage = if (progress.status == StorageIndexRunStatus.FAILED) progress.message else null,
                lastScanAtMillis = if (progress.status == StorageIndexRunStatus.COMPLETED) {
                    System.currentTimeMillis()
                } else {
                    it.lastScanAtMillis
                },
            )
        }
    }
}
