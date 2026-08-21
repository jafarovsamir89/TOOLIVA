package az.simplesoft.tooliva.feature.clean.recommendations

import android.app.Application
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.cleanup.CleanupCandidate
import az.simplesoft.tooliva.core.cleanup.CleanupReasonId
import az.simplesoft.tooliva.core.cleanup.CleanupRecommendationRules
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.PendingMediaDelete
import az.simplesoft.tooliva.core.media.PreparedCleanupDeletion
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import az.simplesoft.tooliva.core.storage.StorageScanScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class RecommendationSortOrder {
    OLDEST,
    NEWEST,
    LARGEST,
    SMALLEST,
    NAME,
}

data class RecommendationAgeFilter(val days: Int, val label: String)

data class RecommendationSummary(
    val reasonId: CleanupReasonId,
    val title: String,
    val count: Int,
    val bytes: Long,
)

data class CleanupRecommendationsUiState(
    val isLoading: Boolean = false,
    val isPreparingDelete: Boolean = false,
    val hasAnalyzed: Boolean = false,
    val entries: List<StorageEntry> = emptyList(),
    val candidates: List<CleanupCandidate> = emptyList(),
    val selectedRefs: Set<String> = emptySet(),
    val reasonFilter: CleanupReasonId? = null,
    val ageFilter: RecommendationAgeFilter = RecommendationAgeFilter(180, "180+ days"),
    val sortOrder: RecommendationSortOrder = RecommendationSortOrder.OLDEST,
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val pendingDelete: PendingMediaDelete? = null,
    val cleanupResult: CleanupResult? = null,
    val accessState: StorageAccessState = StorageAccessState(
        fullStorageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        allFilesAccessGranted = false,
    ),
    val visitedFiles: Long = 0L,
    val nowMillis: Long = System.currentTimeMillis(),
) {
    val visibleCandidates: List<CleanupCandidate>
        get() = candidates
            .asSequence()
            .filter { reasonFilter == null || it.reason.id == reasonFilter }
            .filter {
                searchQuery.isBlank() ||
                    it.entry.name.contains(searchQuery, ignoreCase = true) ||
                    it.entry.path.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(recommendationComparator(sortOrder))
            .toList()

    val selectedCandidates: List<CleanupCandidate>
        get() = candidates.filter { it.entry.ref.toString() in selectedRefs }
    val selectedBytes: Long
        get() = selectedCandidates.sumOf { it.entry.sizeBytes }
    val allVisibleSelected: Boolean
        get() = visibleCandidates.isNotEmpty() && visibleCandidates.all { it.entry.ref.toString() in selectedRefs }
    val summaries: List<RecommendationSummary>
        get() = CleanupReasonId.entries.map { id ->
            val matching = candidates.filter { it.reason.id == id }
            RecommendationSummary(id, matching.firstOrNull()?.reason?.title ?: id.title(), matching.size, matching.sumOf { it.entry.sizeBytes })
        }
}

private fun CleanupReasonId.title(): String = when (this) {
    CleanupReasonId.OLD_APK_INSTALLER -> "Old APK installers"
    CleanupReasonId.OLD_DOWNLOAD -> "Old Downloads"
    CleanupReasonId.RESIDUAL_TEMP -> "Temporary download fragments"
}

private fun recommendationComparator(order: RecommendationSortOrder): Comparator<CleanupCandidate> = Comparator { left, right ->
    val primary = when (order) {
        RecommendationSortOrder.OLDEST -> left.entry.modifiedAtMillis.compareTo(right.entry.modifiedAtMillis)
        RecommendationSortOrder.NEWEST -> right.entry.modifiedAtMillis.compareTo(left.entry.modifiedAtMillis)
        RecommendationSortOrder.LARGEST -> right.entry.sizeBytes.compareTo(left.entry.sizeBytes)
        RecommendationSortOrder.SMALLEST -> left.entry.sizeBytes.compareTo(right.entry.sizeBytes)
        RecommendationSortOrder.NAME -> left.entry.name.compareTo(right.entry.name, ignoreCase = true)
    }
    primary.takeIf { it != 0 }
        ?: left.entry.name.compareTo(right.entry.name, ignoreCase = true).takeIf { it != 0 }
        ?: left.entry.path.compareTo(right.entry.path, ignoreCase = true)
}

internal class CleanupCandidateAccumulator {
    private val candidates = linkedMapOf<String, CleanupCandidate>()

    fun add(entry: StorageEntry, thresholdDays: Int, nowMillis: Long) {
        val candidate = CleanupRecommendationRules.candidateFor(entry, thresholdDays, nowMillis) ?: return
        candidates.putIfAbsent(entry.ref.toString(), candidate)
    }

    fun snapshot(): List<CleanupCandidate> = candidates.values.toList()
}

class CleanupRecommendationsViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val _uiState = MutableStateFlow(
        CleanupRecommendationsUiState(accessState = accessCoordinator.currentState()),
    )
    val uiState = _uiState.asStateFlow()
    private var scanJob: Job? = null
    private var nextDeleteRequestId = 0L

    fun refreshAccess() {
        val latest = accessCoordinator.currentState()
        _uiState.update { state ->
            if (state.accessState == latest) state else state.copy(
                accessState = latest,
                hasAnalyzed = false,
                entries = emptyList(),
                candidates = emptyList(),
                selectedRefs = emptySet(),
                errorMessage = null,
            )
        }
    }

    fun analyze() {
        if (_uiState.value.isLoading || _uiState.value.isPreparingDelete) return
        if (_uiState.value.accessState.mode != StorageAccessMode.FULL) {
            _uiState.update { it.copy(errorMessage = "Full Storage Access is required for automatic cleanup recommendations.") }
            return
        }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            val now = System.currentTimeMillis()
            val threshold = _uiState.value.ageFilter.days
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasAnalyzed = true,
                    entries = emptyList(),
                    candidates = emptyList(),
                    selectedRefs = emptySet(),
                    errorMessage = null,
                    visitedFiles = 0L,
                    nowMillis = now,
                )
            }
            try {
                collectRecommendations(threshold, now)
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw cancellation
            } catch (_: SecurityException) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Full Storage Access was revoked. Grant it again to analyze recommendations.") }
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to analyze cleanup recommendations.") }
            }
        }
    }

    fun cancelAnalyze() = scanJob?.cancel()

    fun setAgeFilter(filter: RecommendationAgeFilter) {
        _uiState.update { state ->
            val updated = state.copy(ageFilter = filter)
            if (!state.hasAnalyzed || state.isLoading) updated
            else updated.copy(candidates = candidatesFor(state.entries, filter.days, state.nowMillis), selectedRefs = emptySet())
        }
    }

    fun setReasonFilter(filter: CleanupReasonId?) = _uiState.update { it.copy(reasonFilter = filter) }
    fun setSortOrder(order: RecommendationSortOrder) = _uiState.update { it.copy(sortOrder = order) }
    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun toggleSelection(ref: String) = _uiState.update { state ->
        val selected = if (ref in state.selectedRefs) state.selectedRefs - ref else state.selectedRefs + ref
        state.copy(selectedRefs = selected)
    }

    fun toggleSelectAllVisible() = _uiState.update { state ->
        val visible = state.visibleCandidates.map { it.entry.ref.toString() }.toSet()
        val selected = if (state.allVisibleSelected) state.selectedRefs - visible else state.selectedRefs + visible
        state.copy(selectedRefs = selected)
    }

    fun showError(message: String) = _uiState.update { it.copy(errorMessage = message) }

    fun requestDelete(coordinator: MediaStoreDeleteCoordinator) {
        if (_uiState.value.isPreparingDelete) return
        val selected = _uiState.value.selectedCandidates.map { CleanupFile(it.entry.ref, it.entry.sizeBytes) }
        if (selected.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isPreparingDelete = true, errorMessage = null) }
            try {
                val prepared = withContext(Dispatchers.IO) { coordinator.prepare(selected) }
                if (prepared.eligible.isEmpty()) {
                    finishWithResult(coordinator.noChange(prepared, "The selected files were already gone before cleanup."))
                } else if (prepared.eligible.any { it.uri.scheme == "file" } || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                    finishWithResult(withContext(Dispatchers.IO) { coordinator.deleteImmediatelyAndVerify(prepared) })
                } else {
                    val sender = withContext(Dispatchers.IO) { coordinator.createTrashIntentSender(prepared) }
                    if (sender == null) {
                        finishWithResult(coordinator.noChange(prepared, "Android could not create a Trash request for the selected items."))
                    } else {
                        nextDeleteRequestId++
                        _uiState.update { it.copy(isPreparingDelete = false, pendingDelete = PendingMediaDelete(nextDeleteRequestId, prepared, sender)) }
                    }
                }
            } catch (_: SecurityException) {
                finishWithResult(CleanupResult.permissionRevoked(PreparedCleanupDeletion(selected, selected, emptyList())))
            } catch (error: Exception) {
                _uiState.update { it.copy(isPreparingDelete = false, errorMessage = error.message ?: "Unable to prepare cleanup.") }
            }
        }
    }

    fun onSystemDeleteResult(requestId: Long, approved: Boolean, coordinator: MediaStoreDeleteCoordinator) {
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
            } else CleanupResult.canceled(pending.prepared)
            finishWithResult(result)
        }
    }

    fun dismissCleanupResult() = _uiState.update { it.copy(cleanupResult = null) }

    private suspend fun collectRecommendations(thresholdDays: Int, nowMillis: Long) {
        val entries = mutableListOf<StorageEntry>()
        val accumulator = CleanupCandidateAccumulator()
        var lastUiPublishAt = 0L
        var visitedFiles = 0L
        fun publishProgress(force: Boolean = false) {
            val now = SystemClock.uptimeMillis()
            if (force || now - lastUiPublishAt >= UI_UPDATE_INTERVAL_MS) {
                lastUiPublishAt = now
                _uiState.update { it.copy(entries = entries.toList(), candidates = accumulator.snapshot(), visitedFiles = visitedFiles) }
            }
        }
        FullStorageProvider(getApplication()).scan(0L, StorageScanScope.DOWNLOADS).collect { event ->
            when (event) {
                StorageScanEvent.Started -> Unit
                is StorageScanEvent.EntryFound -> {
                    entries += event.entry
                    accumulator.add(event.entry, thresholdDays, nowMillis)
                    publishProgress()
                }
                is StorageScanEvent.DirectoryVisited -> Unit
                is StorageScanEvent.Progress -> {
                    visitedFiles = event.visitedFiles
                    publishProgress()
                }
                is StorageScanEvent.Warning -> Unit
                StorageScanEvent.Completed -> Unit
            }
        }
        updateScanned(entries, accumulator.snapshot())
    }

    private fun candidatesFor(entries: List<StorageEntry>, thresholdDays: Int, nowMillis: Long): List<CleanupCandidate> {
        val accumulator = CleanupCandidateAccumulator()
        entries.forEach { accumulator.add(it, thresholdDays, nowMillis) }
        return accumulator.snapshot()
    }

    private fun finishWithResult(result: CleanupResult) {
        _uiState.update { it.copy(isLoading = true, isPreparingDelete = false, pendingDelete = null, entries = emptyList(), candidates = emptyList(), selectedRefs = emptySet(), cleanupResult = result, errorMessage = null) }
        viewModelScope.launch {
            try {
                collectRecommendations(_uiState.value.ageFilter.days, System.currentTimeMillis())
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Cleanup finished, but recommendations could not be refreshed.") }
            }
        }
    }

    private companion object {
        const val UI_UPDATE_INTERVAL_MS = 250L
    }

    private fun updateScanned(entries: List<StorageEntry>, candidates: List<CleanupCandidate>) {
        val ids = candidates.map { it.entry.ref.toString() }.toSet()
        _uiState.update { it.copy(isLoading = false, entries = entries, candidates = candidates, selectedRefs = it.selectedRefs.intersect(ids)) }
    }
}
