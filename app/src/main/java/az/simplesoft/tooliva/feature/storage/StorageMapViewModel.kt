package az.simplesoft.tooliva.feature.storage

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.CleanupResultStatus
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageMapAnalyzer
import az.simplesoft.tooliva.core.storage.StorageMapEvent
import az.simplesoft.tooliva.core.storage.StorageMapNode
import az.simplesoft.tooliva.core.storage.StorageMapResult
import az.simplesoft.tooliva.feature.files.FileOperationCoordinator
import az.simplesoft.tooliva.feature.files.FileOperationEvent
import az.simplesoft.tooliva.feature.files.FileOperationKind
import az.simplesoft.tooliva.feature.files.FileOperationRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class StorageMapPhase { IDLE, LOADING, COMPLETE }
enum class StorageMapView { MAP, SUNBURST, LIST }

data class StorageMapUiState(
    val accessState: StorageAccessState = StorageAccessState(false, false),
    val phase: StorageMapPhase = StorageMapPhase.IDLE,
    val view: StorageMapView = StorageMapView.MAP,
    val result: StorageMapResult? = null,
    val currentPath: String? = null,
    val filesChecked: Long = 0L,
    val foldersFound: Long = 0L,
    val bytesCounted: Long = 0L,
    val warningCount: Long = 0L,
    val isStale: Boolean = false,
    val errorMessage: String? = null,
    val detailsNode: StorageMapNode? = null,
    val deleteNode: StorageMapNode? = null,
    val cleanupResult: CleanupResult? = null,
)

class StorageMapViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val analyzer = StorageMapAnalyzer(application)
    private val operations = FileOperationCoordinator(application)
    private val _uiState = MutableStateFlow(StorageMapUiState(accessState = accessCoordinator.currentState()))
    val uiState = _uiState.asStateFlow()
    private var analysisJob: Job? = null
    private var deleteJob: Job? = null

    fun refreshAccess() {
        val access = accessCoordinator.currentState()
        _uiState.update { it.copy(accessState = access, errorMessage = if (access.mode != StorageAccessMode.FULL && it.phase == StorageMapPhase.LOADING) "Full Storage Access was revoked. Analysis stopped." else it.errorMessage) }
    }

    fun analyze() {
        if (_uiState.value.phase == StorageMapPhase.LOADING) return
        if (accessCoordinator.currentState().mode != StorageAccessMode.FULL) {
            _uiState.update { it.copy(errorMessage = "Full Storage Access is required to analyze folders.") }
            return
        }
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _uiState.update { it.copy(phase = StorageMapPhase.LOADING, result = null, currentPath = null, filesChecked = 0L, foldersFound = 0L, bytesCounted = 0L, warningCount = 0L, isStale = false, errorMessage = null) }
            try {
                analyzer.analyze().collect { event ->
                    when (event) {
                        StorageMapEvent.Started -> Unit
                        is StorageMapEvent.Progress -> _uiState.update { it.copy(filesChecked = event.filesChecked, foldersFound = event.foldersFound, bytesCounted = event.bytesCounted) }
                        is StorageMapEvent.Warning -> _uiState.update { it.copy(warningCount = it.warningCount + 1L) }
                        is StorageMapEvent.Completed -> _uiState.update { it.copy(phase = StorageMapPhase.COMPLETE, result = event.result, filesChecked = event.result.filesChecked, foldersFound = event.result.foldersFound, bytesCounted = event.result.bytesCounted) }
                    }
                }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(phase = StorageMapPhase.IDLE) }
            } catch (error: Exception) {
                _uiState.update { it.copy(phase = StorageMapPhase.IDLE, errorMessage = error.message ?: "Unable to analyze storage.") }
            }
        }
    }

    fun cancelAnalyze() { analysisJob?.cancel() }
    fun setView(view: StorageMapView) = _uiState.update { it.copy(view = view) }
    fun openNode(node: StorageMapNode) = _uiState.update { it.copy(currentPath = node.path) }
    fun goUp(onRoot: () -> Unit = {}) {
        val state = _uiState.value
        val current = state.result?.find(state.currentPath ?: return) ?: return
        val root = state.result.roots.firstOrNull { it.path == current.path }
        if (root != null) {
            _uiState.update { it.copy(currentPath = null) }
            onRoot()
        } else {
            val parent = File(current.path).parentFile?.absolutePath
            _uiState.update { it.copy(currentPath = state.result.find(parent.orEmpty())?.path) }
        }
    }

    fun showDetails(node: StorageMapNode) = _uiState.update { it.copy(detailsNode = node) }
    fun dismissDetails() = _uiState.update { it.copy(detailsNode = null) }
    fun requestDelete(node: StorageMapNode) = _uiState.update { it.copy(deleteNode = node) }
    fun dismissDelete() = _uiState.update { it.copy(deleteNode = null) }

    fun confirmDelete(node: StorageMapNode) {
        _uiState.update { it.copy(deleteNode = null) }
        deleteJob?.cancel()
        deleteJob = viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    var operation: az.simplesoft.tooliva.feature.files.FileOperationResult? = null
                    operations.execute(FileOperationRequest(FileOperationKind.DELETE, listOf(File(node.path)))).collect { event ->
                        if (event is FileOperationEvent.Finished) operation = event.result
                    }
                    operation
                }
            }.getOrNull()
            val receipt = result?.cleanupResult ?: result?.toCleanupResult(node)
            _uiState.update { it.copy(cleanupResult = receipt, isStale = true, errorMessage = if (receipt == null) "The folder operation could not be verified." else null) }
        }
    }

    fun dismissCleanupResult() = _uiState.update { it.copy(cleanupResult = null) }

    private fun az.simplesoft.tooliva.feature.files.FileOperationResult.toCleanupResult(node: StorageMapNode): CleanupResult {
        val status = when {
            canceled -> CleanupResultStatus.CANCELED
            completedItems == 0 && failedItems == 0 -> CleanupResultStatus.NO_CHANGE
            failedItems > 0 || skippedItems > 0 -> CleanupResultStatus.PARTIAL
            else -> CleanupResultStatus.COMPLETED
        }
        return CleanupResult(
            status = status,
            requestedCount = node.fileCount.toInt().coerceAtLeast(1),
            requestedBytes = node.totalBytes,
            removedFromActiveCount = completedItems,
            removedFromActiveBytes = completedBytes,
            trashedCount = 0,
            trashedBytes = 0,
            freedCount = completedItems,
            freedBytes = completedBytes,
            missingBeforeCount = skippedItems,
            missingBeforeBytes = 0L,
            failedCount = failedItems,
            failedBytes = 0L,
            unchangedCount = failedItems + skippedItems,
            unchangedBytes = 0L,
            note = errors.takeIf { it.isNotEmpty() }?.joinToString(" "),
        )
    }
}
