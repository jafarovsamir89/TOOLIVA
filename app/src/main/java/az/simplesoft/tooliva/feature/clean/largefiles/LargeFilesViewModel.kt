package az.simplesoft.tooliva.feature.clean.largefiles

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.LargeMediaFile
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.PendingMediaDelete
import az.simplesoft.tooliva.core.media.PreparedCleanupDeletion
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import az.simplesoft.tooliva.core.storage.index.IndexedStorageEntry
import az.simplesoft.tooliva.core.storage.index.StorageIndexQuery
import az.simplesoft.tooliva.core.storage.index.StorageIndexRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LargeFilesUiState(
    val isLoading: Boolean = false,
    val isPreparingDelete: Boolean = false,
    val files: List<LargeMediaFile> = emptyList(),
    val selectedUris: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val pendingDelete: PendingMediaDelete? = null,
    val cleanupResult: CleanupResult? = null,
    val accessState: StorageAccessState = StorageAccessState(
        fullStorageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        allFilesAccessGranted = false,
    ),
    val thresholdBytes: Long = LargeFilesUiState.MIN_LARGE_FILE_BYTES,
    val categoryFilter: StorageCategory = StorageCategory.ALL,
    val sortOrder: StorageSortOrder = StorageSortOrder.SIZE,
    val searchQuery: String = "",
    val visitedFiles: Long = 0L,
) {
    val visibleFiles: List<LargeMediaFile>
        get() = files
            .asSequence()
            .filter { it.sizeBytes >= thresholdBytes }
            .filter { categoryFilter == StorageCategory.ALL || it.category == categoryFilter }
            .filter {
                searchQuery.isBlank() ||
                    it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.path.orEmpty().contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(
                when (sortOrder) {
                    StorageSortOrder.SIZE -> compareByDescending(LargeMediaFile::sizeBytes)
                    StorageSortOrder.NEWEST -> compareByDescending(LargeMediaFile::modifiedEpochSeconds)
                    StorageSortOrder.OLDEST -> compareBy(LargeMediaFile::modifiedEpochSeconds)
                    StorageSortOrder.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER, LargeMediaFile::displayName)
                },
            )
            .toList()

    val selectedFiles: List<LargeMediaFile>
        get() = files.filter { it.uri.toString() in selectedUris }

    val selectedBytes: Long
        get() = selectedFiles.sumOf { it.sizeBytes }

    val allVisibleSelected: Boolean
        get() = visibleFiles.isNotEmpty() && visibleFiles.all { it.uri.toString() in selectedUris }

    companion object {
        const val MIN_LARGE_FILE_BYTES: Long = 100L * 1024L * 1024L
    }
}

class LargeFilesViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val indexRepository = StorageIndexRepository(application)
    private val _uiState = MutableStateFlow(
        LargeFilesUiState(accessState = accessCoordinator.currentState()),
    )
    val uiState = _uiState.asStateFlow()
    private var scanJob: Job? = null
    private var nextDeleteRequestId = 0L

    fun refreshAccess() {
        val latest = accessCoordinator.currentState()
        _uiState.update { state ->
            if (state.accessState == latest) state else state.copy(accessState = latest, files = emptyList(), selectedUris = emptySet())
        }
    }

    fun scan() {
        if (_uiState.value.isLoading || _uiState.value.isPreparingDelete) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, files = emptyList(), visitedFiles = 0L) }
            try {
                loadIndexIntoState(showMissingIndexError = true)
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw cancellation
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to read the storage index.") }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    fun setThreshold(bytes: Long) {
        _uiState.update { it.copy(thresholdBytes = bytes) }
        reloadIndex()
    }

    fun setCategory(category: StorageCategory) {
        _uiState.update { it.copy(categoryFilter = category) }
        reloadIndex()
    }

    fun setSortOrder(order: StorageSortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        reloadIndex()
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        reloadIndex()
    }

    fun toggleSelection(uri: String) {
        _uiState.update { state ->
            val selected = if (uri in state.selectedUris) state.selectedUris - uri else state.selectedUris + uri
            state.copy(selectedUris = selected)
        }
    }

    fun toggleSelectAllVisible() {
        _uiState.update { state ->
            val visibleIds = state.visibleFiles.map { it.uri.toString() }.toSet()
            val selected = if (state.allVisibleSelected) state.selectedUris - visibleIds else state.selectedUris + visibleIds
            state.copy(selectedUris = selected)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedUris = emptySet()) }
    }

    fun onAccessRevoked() {
        refreshAccess()
        _uiState.update { it.copy(errorMessage = "Full Storage Access is unavailable. Tooliva is using Limited Mode.") }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun requestDelete(coordinator: MediaStoreDeleteCoordinator, directDelete: Boolean) {
        if (_uiState.value.isPreparingDelete) return
        val selected = _uiState.value.selectedFiles.map { CleanupFile(it.uri, it.sizeBytes) }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingDelete = true, errorMessage = null) }
            try {
                val prepared = withContext(Dispatchers.IO) { coordinator.prepare(selected) }
                if (prepared.eligible.isEmpty()) {
                    finishWithResult(coordinator.noChange(prepared, "The selected files were already gone before cleanup."))
                } else if (directDelete || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    val result = withContext(Dispatchers.IO) { coordinator.deleteImmediatelyAndVerify(prepared) }
                    finishWithResult(result)
                } else {
                    val sender = withContext(Dispatchers.IO) { coordinator.createTrashIntentSender(prepared) }
                    if (sender == null) {
                        finishWithResult(coordinator.noChange(prepared, "Android could not create a Trash request for the selected items."))
                    } else {
                        nextDeleteRequestId++
                        _uiState.update {
                            it.copy(
                                isPreparingDelete = false,
                                pendingDelete = PendingMediaDelete(nextDeleteRequestId, prepared, sender),
                            )
                        }
                    }
                }
            } catch (_: SecurityException) {
                val prepared = PreparedCleanupDeletion(selected, selected, emptyList())
                finishWithResult(CleanupResult.permissionRevoked(prepared))
            } catch (error: Exception) {
                _uiState.update { it.copy(isPreparingDelete = false, errorMessage = error.message ?: "Unable to prepare cleanup.") }
            }
        }
    }

    fun onSystemDeleteResult(
        requestId: Long,
        approved: Boolean,
        coordinator: MediaStoreDeleteCoordinator,
    ) {
        val pending = _uiState.value.pendingDelete ?: return
        if (pending.requestId != requestId) return

        _uiState.update { it.copy(pendingDelete = null, isPreparingDelete = true) }
        viewModelScope.launch {
            val result = if (approved) {
                runCatching { withContext(Dispatchers.IO) { coordinator.verifyTrash(pending.prepared) } }
                    .getOrElse { error ->
                        if (error is SecurityException) CleanupResult.permissionRevoked(pending.prepared)
                        else coordinator.noChange(pending.prepared, error.message ?: "Unable to verify the Trash result.")
                    }
            } else {
                CleanupResult.canceled(pending.prepared)
            }
            finishWithResult(result)
        }
    }

    fun dismissCleanupResult() {
        _uiState.update { it.copy(cleanupResult = null) }
    }

    private fun finishWithResult(result: CleanupResult) {
        // The deletion coordinator has already verified this operation. Do not make the
        // user wait for a second full-storage scan before showing that confirmed result.
        _uiState.update {
            it.copy(
                isLoading = true,
                isPreparingDelete = false,
                pendingDelete = null,
                files = emptyList(),
                selectedUris = emptySet(),
                visitedFiles = 0L,
                cleanupResult = result,
                errorMessage = null,
            )
        }
        refreshAfterCleanup(result)
    }

    private fun refreshAfterCleanup(result: CleanupResult) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    indexRepository.removeEntriesByRefs(
                        accessMode = _uiState.value.accessState.mode,
                        refs = result.removedRefs,
                    )
                }
                loadIndexIntoState(showMissingIndexError = false)
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Cleanup finished, but the index could not be refreshed.") }
            }
        }
    }

    private fun reloadIndex() {
        if (_uiState.value.isPreparingDelete || _uiState.value.isLoading) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            runCatching { loadIndexIntoState(showMissingIndexError = false) }
                .onFailure { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to read the storage index.") } }
        }
    }

    private suspend fun loadIndexIntoState(showMissingIndexError: Boolean) {
        val snapshot = _uiState.value
        val query = StorageIndexQuery(
            accessMode = snapshot.accessState.mode,
            minimumSizeBytes = snapshot.thresholdBytes,
            category = snapshot.categoryFilter.takeUnless { it == StorageCategory.ALL },
            searchQuery = snapshot.searchQuery,
            sortOrder = snapshot.sortOrder,
        )
        val indexed = withContext(Dispatchers.IO) { indexRepository.query(query) }
        val hasIndex = withContext(Dispatchers.IO) { indexRepository.lastSuccessfulScan(snapshot.accessState.mode) != null }
        val files = indexed.map { it.toLargeMediaFile() }
        val ids = files.map { it.uri.toString() }.toSet()
        _uiState.update {
            it.copy(
                isLoading = false,
                files = files,
                selectedUris = it.selectedUris.intersect(ids),
                errorMessage = if (showMissingIndexError && !hasIndex) {
                    "No storage index is ready. Open Clean and tap Scan Storage first."
                } else {
                    null
                },
            )
        }
    }

    private fun IndexedStorageEntry.toLargeMediaFile() = LargeMediaFile(
        uri = ref,
        displayName = displayName,
        sizeBytes = sizeBytes,
        mimeType = mimeType,
        modifiedEpochSeconds = modifiedTimeMillis / 1000L,
        category = category,
        path = path,
    )
}
