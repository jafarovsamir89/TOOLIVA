package az.simplesoft.tooliva.feature.clean.emptyfolders

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.PreparedCleanupDeletion
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import az.simplesoft.tooliva.feature.clean.CleanerAnalysisRules
import az.simplesoft.tooliva.feature.clean.CleanerBucket
import az.simplesoft.tooliva.feature.clean.CleanerSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class EmptyFolderEntry(val path: String, val modifiedAtMillis: Long)
data class EmptyFoldersUiState(
    val isLoading: Boolean = false,
    val hasAnalyzed: Boolean = false,
    val folders: List<EmptyFolderEntry> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val visitedFolders: Long = 0L,
    val errorMessage: String? = null,
    val cleanupResult: CleanupResult? = null,
    val accessState: StorageAccessState = StorageAccessState(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R, false),
) {
    val selected: List<EmptyFolderEntry> get() = folders.filter { it.path in selectedPaths }
    val allSelected: Boolean get() = folders.isNotEmpty() && folders.all { it.path in selectedPaths }
}

class EmptyFoldersViewModel(application: Application) : AndroidViewModel(application) {
    private val access = StorageAccessCoordinator(application)
    private val _uiState = MutableStateFlow(EmptyFoldersUiState(accessState = access.currentState()))
    val uiState = _uiState.asStateFlow()
    private var scanJob: Job? = null

    init {
        val seeded = CleanerSessionStore.latest?.entriesFor(CleanerBucket.EMPTY_FOLDERS).orEmpty()
            .map { EmptyFolderEntry(it.path, it.modifiedAtMillis) }
        if (seeded.isNotEmpty()) _uiState.update { it.copy(hasAnalyzed = true, folders = seeded) }
    }

    fun refreshAccess() { _uiState.update { it.copy(accessState = access.currentState()) } }
    fun toggle(path: String) = _uiState.update { state -> state.copy(selectedPaths = if (path in state.selectedPaths) state.selectedPaths - path else state.selectedPaths + path) }
    fun selectAll() = _uiState.update { state -> state.copy(selectedPaths = if (state.allSelected) emptySet() else state.folders.map { it.path }.toSet()) }
    fun dismissResult() = _uiState.update { it.copy(cleanupResult = null) }

    fun scan() {
        if (_uiState.value.isLoading) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val found = linkedMapOf<String, EmptyFolderEntry>()
            _uiState.update { it.copy(isLoading = true, hasAnalyzed = true, folders = emptyList(), selectedPaths = emptySet(), visitedFolders = 0L, errorMessage = null) }
            try {
                FullStorageProvider(getApplication()).scan(0L).collect { event ->
                    when (event) {
                        StorageScanEvent.Started, StorageScanEvent.Completed -> Unit
                        is StorageScanEvent.DirectoryVisited -> {
                            _uiState.update { it.copy(visitedFolders = it.visitedFolders + 1L) }
                            if (event.isEmpty && CleanerAnalysisRules.isSafeEmptyFolderCandidate(event.path)) found.putIfAbsent(event.path, EmptyFolderEntry(event.path, File(event.path).lastModified()))
                        }
                        is StorageScanEvent.Progress -> _uiState.update { it.copy(visitedFolders = maxOf(it.visitedFolders, event.visitedFiles)) }
                        is StorageScanEvent.Warning -> Unit
                        is StorageScanEvent.EntryFound -> Unit
                    }
                }
                _uiState.update { it.copy(isLoading = false, folders = found.values.sortedBy { folder -> folder.path }) }
            } catch (_: CancellationException) { _uiState.update { it.copy(isLoading = false) } }
            catch (error: Exception) { _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to scan empty folders.") } }
        }
    }
    fun cancelScan() { scanJob?.cancel() }

    fun deleteSelected(coordinator: MediaStoreDeleteCoordinator) {
        val selected = _uiState.value.selected
        if (selected.isEmpty()) return
        viewModelScope.launch {
            val requested = selected.map { CleanupFile(android.net.Uri.fromFile(File(it.path)), 0L) }
            val eligible = selected.filter { folder ->
                val file = File(folder.path)
                file.exists() && file.isDirectory && file.listFiles()?.isEmpty() == true && CleanerAnalysisRules.isSafeEmptyFolderCandidate(folder.path)
            }.map { CleanupFile(android.net.Uri.fromFile(File(it.path)), 0L) }
            val missing = requested.filterNot { item -> eligible.any { it.uri == item.uri } }
            val prepared = PreparedCleanupDeletion(requested = requested, eligible = eligible, missingBeforeAction = missing)
            val result = try {
                withContext(kotlinx.coroutines.Dispatchers.IO) { coordinator.deleteImmediatelyAndVerify(prepared) }
            } catch (_: SecurityException) { CleanupResult.permissionRevoked(prepared) }
            _uiState.update { state -> state.copy(folders = state.folders.filterNot { it.path in state.selectedPaths && eligible.any { item -> item.uri.path == it.path } }, selectedPaths = emptySet(), cleanupResult = result) }
        }
    }
}
