package az.simplesoft.tooliva.feature.clean.largefiles

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.ImmediateDeleteResult
import az.simplesoft.tooliva.core.media.LargeMediaFile
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.MediaStoreLargeFileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LargeFilesUiState(
    val isLoading: Boolean = false,
    val files: List<LargeMediaFile> = emptyList(),
    val selectedUris: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val resultMessage: String? = null,
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
    private var pendingVerificationIds: Set<String> = emptySet()

    fun scan() {
        if (_uiState.value.isLoading) return

        scanJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val files = mutableListOf<LargeMediaFile>()
            try {
                repository.scan().collect { file ->
                    files += file
                    _uiState.update { it.copy(files = files.toList()) }
                }
                val fileIds = files.map { it.uri.toString() }.toSet()
                val verificationIds = pendingVerificationIds
                val verifiedRemovedCount = verificationIds.count { it !in fileIds }
                if (verificationIds.isNotEmpty()) {
                    pendingVerificationIds = emptySet()
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        files = files.sortedByDescending(LargeMediaFile::sizeBytes),
                        selectedUris = it.selectedUris.intersect(fileIds),
                        resultMessage = if (verificationIds.isEmpty()) {
                            it.resultMessage
                        } else {
                            "$verifiedRemovedCount of ${verificationIds.size} item(s) are no longer visible after the system action."
                        },
                    )
                }
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw cancellation
            } catch (error: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Unable to scan media files.",
                    )
                }
            }
        }
    }

    fun cancelScan() {
        scanJob?.cancel()
    }

    fun toggleSelection(uri: String) {
        _uiState.update { state ->
            val selected = if (uri in state.selectedUris) {
                state.selectedUris - uri
            } else {
                state.selectedUris + uri
            }
            state.copy(selectedUris = selected, resultMessage = null)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedUris = emptySet()) }
    }

    fun showNotice(message: String) {
        _uiState.update { it.copy(resultMessage = message) }
    }

    fun onPlatformDeletionFinished(uris: List<Uri>) {
        if (uris.isEmpty()) return
        pendingVerificationIds = uris.map(Uri::toString).toSet()
        clearSelection()
        _uiState.update {
            it.copy(resultMessage = "System confirmation completed. Verifying the updated list…")
        }
        scan()
    }

    fun deleteImmediately(coordinator: MediaStoreDeleteCoordinator, uris: List<Uri>) {
        if (uris.isEmpty()) return
        clearSelection()
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                coordinator.deleteImmediately(uris)
            }
            pendingVerificationIds = uris.map(Uri::toString).toSet()
            applyImmediateDeleteResult(result)
            scan()
        }
    }

    private fun applyImmediateDeleteResult(result: ImmediateDeleteResult) {
        _uiState.update {
            it.copy(
                resultMessage = "${result.deletedCount} of ${result.requestedCount} item(s) deleted. Refreshing the list…",
            )
        }
    }
}
