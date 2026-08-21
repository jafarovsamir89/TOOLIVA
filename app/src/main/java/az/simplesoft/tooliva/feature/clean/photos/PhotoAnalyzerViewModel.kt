package az.simplesoft.tooliva.feature.clean.photos

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.PendingMediaDelete
import az.simplesoft.tooliva.core.media.PreparedCleanupDeletion
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PhotoAnalyzerUiState(
    val filter: PhotoAnalysisKind? = null,
    val isLoading: Boolean = false,
    val progress: PhotoAnalysisProgress = PhotoAnalysisProgress(),
    val items: List<PhotoAnalysisItem> = emptyList(),
    val selectedUris: Set<String> = emptySet(),
    val previewItem: PhotoAnalysisItem? = null,
    val pendingDelete: PendingMediaDelete? = null,
    val cleanupResult: CleanupResult? = null,
    val error: String? = null,
) {
    val visibleItems: List<PhotoAnalysisItem> get() = filter?.let { value -> items.filter { it.kind == value } } ?: items
    val selectedItems: List<PhotoAnalysisItem> get() = visibleItems.filter { it.uri.toString() in selectedUris }
    val selectedBytes: Long get() = selectedItems.sumOf { it.sizeBytes.coerceAtLeast(0L) }
    val allSelected: Boolean get() = visibleItems.isNotEmpty() && visibleItems.all { it.uri.toString() in selectedUris }
}

class PhotoAnalyzerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PhotoAnalyzerRepository(application)
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val state = MutableStateFlow(PhotoAnalyzerUiState())
    val uiState = state.asStateFlow()
    private var scanJob: Job? = null
    private var requestId = 0L

    fun scan() {
        if (state.value.isLoading || state.value.pendingDelete != null) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            state.update { it.copy(isLoading = true, progress = PhotoAnalysisProgress(), items = emptyList(), selectedUris = emptySet(), error = null) }
            try {
                repository.analyze(accessCoordinator.currentState().mode).collect { event ->
                    state.update { current -> current.copy(progress = event.progress, items = event.items) }
                }
                state.update { it.copy(isLoading = false) }
            } catch (_: CancellationException) {
                state.update { it.copy(isLoading = false) }
            } catch (error: SecurityException) {
                state.update { it.copy(isLoading = false, error = "Photo access was revoked. Grant access again before analyzing media.") }
            } catch (error: Exception) {
                state.update { it.copy(isLoading = false, error = error.message ?: "Unable to analyze photos and videos.") }
            }
        }
    }

    fun cancelScan() { scanJob?.cancel() }
    fun setFilter(filter: PhotoAnalysisKind?) = state.update { it.copy(filter = filter, selectedUris = emptySet()) }
    fun toggleSelection(item: PhotoAnalysisItem) = state.update { current ->
        val key = item.uri.toString()
        current.copy(selectedUris = if (key in current.selectedUris) current.selectedUris - key else current.selectedUris + key)
    }
    fun toggleSelectAll() = state.update { current -> current.copy(selectedUris = if (current.allSelected) emptySet() else current.selectedUris + current.visibleItems.map { it.uri.toString() }) }
    fun showPreview(item: PhotoAnalysisItem) = state.update { it.copy(previewItem = item) }
    fun dismissPreview() = state.update { it.copy(previewItem = null) }

    fun requestDelete(coordinator: MediaStoreDeleteCoordinator) {
        val selected = state.value.selectedItems.map { CleanupFile(it.uri, it.sizeBytes) }
        if (selected.isEmpty()) return
        viewModelScope.launch {
            state.update { it.copy(error = null) }
            try {
                val prepared = withContext(Dispatchers.IO) { coordinator.prepare(selected) }
                if (prepared.eligible.isEmpty()) {
                    finish(coordinator.noChange(prepared, "The selected media was already gone before cleanup."))
                } else if (accessCoordinator.currentState().mode == StorageAccessMode.FULL || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    finish(withContext(Dispatchers.IO) { coordinator.deleteImmediatelyAndVerify(prepared) })
                } else {
                    val sender = withContext(Dispatchers.IO) { coordinator.createTrashIntentSender(prepared) }
                    if (sender == null) finish(coordinator.noChange(prepared, "The selected media is not available for Android Trash."))
                    else {
                        requestId++
                        state.update { it.copy(pendingDelete = PendingMediaDelete(requestId, prepared, sender)) }
                    }
                }
            } catch (_: SecurityException) {
                finish(CleanupResult.permissionRevoked(PreparedCleanupDeletion(selected, selected, emptyList())))
            } catch (error: Exception) {
                state.update { it.copy(error = error.message ?: "Unable to prepare media cleanup.") }
            }
        }
    }

    fun onSystemDeleteResult(approved: Boolean, coordinator: MediaStoreDeleteCoordinator) {
        val pending = state.value.pendingDelete ?: return
        state.update { it.copy(pendingDelete = null) }
        viewModelScope.launch {
            val result = if (approved) runCatching { withContext(Dispatchers.IO) { coordinator.verifyTrash(pending.prepared) } }.getOrElse {
                if (it is SecurityException) CleanupResult.permissionRevoked(pending.prepared) else coordinator.noChange(pending.prepared, it.message ?: "Unable to verify the Trash result.")
            } else CleanupResult.canceled(pending.prepared)
            finish(result)
        }
    }

    fun dismissCleanupResult() = state.update { it.copy(cleanupResult = null) }

    private fun finish(result: CleanupResult) {
        state.update { it.copy(cleanupResult = result, selectedUris = emptySet()) }
        scan()
    }
}
