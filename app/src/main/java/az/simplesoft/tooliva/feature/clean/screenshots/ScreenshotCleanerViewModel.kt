package az.simplesoft.tooliva.feature.clean.screenshots

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.PendingMediaDelete
import az.simplesoft.tooliva.core.media.PreparedCleanupDeletion
import az.simplesoft.tooliva.core.media.ScreenshotMediaFile
import az.simplesoft.tooliva.core.media.ScreenshotMediaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScreenshotCleanerUiState(
    val ageDays: Int = 30,
    val isLoading: Boolean = false,
    val isPreparingDelete: Boolean = false,
    val files: List<ScreenshotMediaFile> = emptyList(),
    val selectedUris: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val pendingDelete: PendingMediaDelete? = null,
    val cleanupResult: CleanupResult? = null,
) {
    val selectedFiles: List<ScreenshotMediaFile>
        get() = files.filter { it.uri.toString() in selectedUris }

    val selectedBytes: Long
        get() = selectedFiles.sumOf { it.sizeBytes }

    val allSelected: Boolean
        get() = files.isNotEmpty() && selectedUris.size == files.size
}

class ScreenshotCleanerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ScreenshotMediaRepository(application)
    private val _uiState = MutableStateFlow(ScreenshotCleanerUiState())
    val uiState = _uiState.asStateFlow()
    private var scanJob: Job? = null
    private var nextDeleteRequestId = 0L

    fun scan() {
        if (_uiState.value.isLoading || _uiState.value.isPreparingDelete) return
        val ageDays = _uiState.value.ageDays
        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val files = mutableListOf<ScreenshotMediaFile>()
            try {
                repository.scan(ageDays).collect { file ->
                    files += file
                    _uiState.update { it.copy(files = files.toList()) }
                }
                updateScannedFiles(files)
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw cancellation
            } catch (_: SecurityException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Photo access is no longer available.")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Unable to scan screenshots.")
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    fun setAgeDays(days: Int) {
        if (days == _uiState.value.ageDays) return
        scanJob?.cancel()
        _uiState.update {
            it.copy(
                ageDays = days,
                isLoading = false,
                files = emptyList(),
                selectedUris = emptySet(),
                errorMessage = null,
            )
        }
        scan()
    }

    fun toggleSelection(uri: String) {
        _uiState.update { state ->
            val selected = if (uri in state.selectedUris) state.selectedUris - uri else state.selectedUris + uri
            state.copy(selectedUris = selected)
        }
    }

    fun toggleSelectAll() {
        _uiState.update { state ->
            state.copy(selectedUris = if (state.allSelected) emptySet() else state.files.map { it.uri.toString() }.toSet())
        }
    }

    fun onMediaPermissionRevoked() {
        _uiState.update {
            it.copy(errorMessage = "Photo access is no longer available. Please grant access again.")
        }
    }

    fun requestDelete(coordinator: MediaStoreDeleteCoordinator) {
        if (_uiState.value.isPreparingDelete) return
        val selected = _uiState.value.selectedFiles.map { CleanupFile(it.uri, it.sizeBytes) }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingDelete = true, errorMessage = null) }
            try {
                val prepared = withContext(Dispatchers.IO) { coordinator.prepare(selected) }
                if (prepared.eligible.isEmpty()) {
                    finishWithResult(coordinator.noChange(prepared, "The selected screenshots were already gone before cleanup."))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val sender = withContext(Dispatchers.IO) { coordinator.createTrashIntentSender(prepared) }
                    if (sender == null) {
                        finishWithResult(coordinator.noChange(prepared, "No selected screenshot remained available to move to Trash."))
                    } else {
                        nextDeleteRequestId++
                        _uiState.update {
                            it.copy(
                                isPreparingDelete = false,
                                pendingDelete = PendingMediaDelete(nextDeleteRequestId, prepared, sender),
                            )
                        }
                    }
                } else {
                    val result = withContext(Dispatchers.IO) { coordinator.deleteImmediatelyAndVerify(prepared) }
                    finishWithResult(result)
                }
            } catch (_: SecurityException) {
                val prepared = PreparedCleanupDeletion(selected, selected, emptyList())
                finishWithResult(CleanupResult.permissionRevoked(prepared))
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isPreparingDelete = false, errorMessage = error.message ?: "Unable to prepare screenshot cleanup.")
                }
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
        // The deletion coordinator has already verified this operation. Show the receipt
        // immediately and refresh the screenshot grid in the background.
        _uiState.update {
            it.copy(
                isLoading = true,
                isPreparingDelete = false,
                pendingDelete = null,
                files = emptyList(),
                selectedUris = emptySet(),
                cleanupResult = result,
                errorMessage = null,
            )
        }
        refreshAfterCleanup()
    }

    private fun refreshAfterCleanup() {
        viewModelScope.launch {
            try {
                val refreshed = withContext(Dispatchers.IO) {
                    repository.scan(_uiState.value.ageDays).toList().sortedByDescending(ScreenshotMediaFile::sizeBytes)
                }
                val ids = refreshed.map { it.uri.toString() }.toSet()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        files = refreshed,
                        selectedUris = it.selectedUris.intersect(ids),
                    )
                }
            } catch (_: SecurityException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Photo access was revoked while refreshing the list.",
                    )
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Cleanup finished, but the screenshot list could not be refreshed.",
                    )
                }
            }
        }
    }

    private fun updateScannedFiles(files: List<ScreenshotMediaFile>) {
        val sorted = files.sortedByDescending(ScreenshotMediaFile::sizeBytes)
        val ids = sorted.map { it.uri.toString() }.toSet()
        _uiState.update {
            it.copy(
                isLoading = false,
                files = sorted,
                selectedUris = it.selectedUris.intersect(ids),
            )
        }
    }
}
