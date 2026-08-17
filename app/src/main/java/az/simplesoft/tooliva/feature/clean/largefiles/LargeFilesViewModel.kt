package az.simplesoft.tooliva.feature.clean.largefiles

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.PendingMediaDelete
import az.simplesoft.tooliva.core.media.PreparedCleanupDeletion
import az.simplesoft.tooliva.core.media.LargeMediaFile
import az.simplesoft.tooliva.core.media.MediaStoreLargeFileRepository
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

data class LargeFilesUiState(
    val isLoading: Boolean = false,
    val isPreparingDelete: Boolean = false,
    val files: List<LargeMediaFile> = emptyList(),
    val selectedUris: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val pendingDelete: PendingMediaDelete? = null,
    val cleanupResult: CleanupResult? = null,
) {
    val selectedFiles: List<LargeMediaFile>
        get() = files.filter { it.uri.toString() in selectedUris }

    val selectedBytes: Long
        get() = selectedFiles.sumOf { it.sizeBytes }
}

class LargeFilesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = MediaStoreLargeFileRepository(application)

    private val _uiState = MutableStateFlow(LargeFilesUiState())
    val uiState = _uiState.asStateFlow()
    private var scanJob: Job? = null
    private var nextDeleteRequestId = 0L

    fun scan() {
        if (_uiState.value.isLoading || _uiState.value.isPreparingDelete) return

        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val files = mutableListOf<LargeMediaFile>()
            try {
                repository.scan().collect { file ->
                    files += file
                    _uiState.update { it.copy(files = files.toList()) }
                }
                updateScannedFiles(files)
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw cancellation
            } catch (security: SecurityException) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = "Photo and video access is no longer available.")
                }
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = error.message ?: "Unable to scan media files.")
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    fun toggleSelection(uri: String) {
        _uiState.update { state ->
            val selected = if (uri in state.selectedUris) state.selectedUris - uri else state.selectedUris + uri
            state.copy(selectedUris = selected)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedUris = emptySet()) }
    }

    fun onMediaPermissionRevoked() {
        _uiState.update {
            it.copy(errorMessage = "Photo and video access is no longer available. Please grant access again.")
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
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
                    finishWithResult(coordinator.noChange(prepared, "The selected files were already gone before cleanup."))
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val sender = withContext(Dispatchers.IO) { coordinator.createTrashIntentSender(prepared) }
                    if (sender == null) {
                        finishWithResult(coordinator.noChange(prepared, "No eligible file remained to move to Trash."))
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
                    it.copy(isPreparingDelete = false, errorMessage = error.message ?: "Unable to prepare cleanup.")
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

    private suspend fun finishWithResult(result: CleanupResult) {
        try {
            val refreshed = withContext(Dispatchers.IO) { repository.scan().toList().sortedByDescending(LargeMediaFile::sizeBytes) }
            val ids = refreshed.map { it.uri.toString() }.toSet()
            _uiState.update {
                it.copy(
                    isLoading = false,
                    isPreparingDelete = false,
                    files = refreshed,
                    selectedUris = it.selectedUris.intersect(ids),
                    cleanupResult = result,
                    errorMessage = null,
                )
            }
        } catch (_: SecurityException) {
            _uiState.update {
                it.copy(
                    isPreparingDelete = false,
                    cleanupResult = result.copy(
                        status = az.simplesoft.tooliva.core.media.CleanupResultStatus.PERMISSION_REVOKED,
                        note = "Media access was revoked while refreshing the list. The result could not be fully rechecked.",
                    ),
                    errorMessage = "Photo and video access is no longer available.",
                )
            }
        } catch (error: Exception) {
            _uiState.update {
                it.copy(
                    isPreparingDelete = false,
                    cleanupResult = result,
                    errorMessage = error.message ?: "Cleanup finished, but the list could not be refreshed.",
                )
            }
        }
    }

    private fun updateScannedFiles(files: List<LargeMediaFile>) {
        val sorted = files.sortedByDescending(LargeMediaFile::sizeBytes)
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
