package az.simplesoft.tooliva.feature.clean.duplicates

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.CleanupResultStatus
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.PreparedCleanupDeletion
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageEntry
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

data class ExactDuplicatesUiState(
    val accessState: StorageAccessState = StorageAccessState(
        fullStorageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        allFilesAccessGranted = false,
    ),
    val stage: DuplicateAnalysisStage = DuplicateAnalysisStage.IDLE,
    val groups: List<DuplicateGroup> = emptyList(),
    val selectedPaths: Set<String> = emptySet(),
    val filter: DuplicateTypeFilter = DuplicateTypeFilter.ALL,
    val sortOrder: DuplicateSortOrder = DuplicateSortOrder.MOST_RECOVERABLE,
    val searchQuery: String = "",
    val progress: DuplicateAnalysisProgress = DuplicateAnalysisProgress(DuplicateAnalysisStage.IDLE),
    val errorMessage: String? = null,
    val cleanupResult: CleanupResult? = null,
    val isDeleting: Boolean = false,
) {
    val isAnalyzing: Boolean get() = stage == DuplicateAnalysisStage.METADATA || stage == DuplicateAnalysisStage.HASHING || stage == DuplicateAnalysisStage.VERIFYING

    val visibleGroups: List<DuplicateGroup>
        get() = groups.asSequence()
            .filter { group -> group.entries.any { DuplicateRules.matchesFilter(it, filter) } }
            .filter { group ->
                searchQuery.isBlank() || group.entries.any { entry ->
                    entry.name.contains(searchQuery, ignoreCase = true) || entry.path.contains(searchQuery, ignoreCase = true)
                }
            }
            .sortedWith(
                when (sortOrder) {
                    DuplicateSortOrder.MOST_RECOVERABLE -> compareByDescending(DuplicateGroup::potentialRecoverableBytes)
                    DuplicateSortOrder.LARGEST -> compareByDescending(DuplicateGroup::fileSizeBytes)
                    DuplicateSortOrder.MOST_COPIES -> compareByDescending(DuplicateGroup::copyCount)
                    DuplicateSortOrder.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.entries.minOfOrNull(StorageEntry::name).orEmpty() }
                },
            )
            .toList()

    val selectedEntries: List<StorageEntry>
        get() = groups.flatMap { it.entries }.filter { it.path in selectedPaths }.distinctBy(StorageEntry::path)

    val selectedBytes: Long get() = selectedEntries.sumOf(StorageEntry::sizeBytes)
    val potentialRecoverableBytes: Long get() = groups.sumOf(DuplicateGroup::potentialRecoverableBytes)
    val identicalFileCount: Int get() = groups.sumOf(DuplicateGroup::copyCount)
}

class ExactDuplicatesViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val storage = FullStorageProvider(application)
    private val analyzer = DuplicateAnalyzer(
        storage = storage,
        cache = DuplicateFingerprintCache(File(application.noBackupFilesDir, "duplicate-fingerprints-v1.txt")),
    )
    private val _uiState = MutableStateFlow(ExactDuplicatesUiState(accessState = accessCoordinator.currentState()))
    val uiState = _uiState.asStateFlow()
    private var analysisJob: Job? = null

    fun refreshAccess() {
        val latest = accessCoordinator.currentState()
        _uiState.update { it.copy(accessState = latest) }
        if (!latest.allFilesAccessGranted && _uiState.value.isAnalyzing) cancelAnalysis()
    }

    fun analyze() {
        if (_uiState.value.isAnalyzing || _uiState.value.isDeleting) return
        val latestAccess = accessCoordinator.currentState()
        _uiState.update { it.copy(accessState = latestAccess) }
        if (!latestAccess.allFilesAccessGranted) {
            _uiState.update { it.copy(errorMessage = "Full Storage Access is required to scan shared storage for exact duplicates.") }
            return
        }
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    stage = DuplicateAnalysisStage.METADATA,
                    groups = emptyList(),
                    selectedPaths = emptySet(),
                    errorMessage = null,
                    progress = DuplicateAnalysisProgress(DuplicateAnalysisStage.METADATA),
                )
            }
            try {
                analyzer.analyze().collect { event ->
                    when (event) {
                        DuplicateAnalysisEvent.Started -> Unit
                        is DuplicateAnalysisEvent.Progress -> _uiState.update { it.copy(stage = event.value.stage, progress = event.value) }
                        is DuplicateAnalysisEvent.GroupConfirmed -> _uiState.update { it.copy(groups = it.groups + event.group, progress = it.progress.copy(groupsConfirmed = it.progress.groupsConfirmed + 1)) }
                        is DuplicateAnalysisEvent.Completed -> _uiState.update {
                            it.copy(
                                stage = DuplicateAnalysisStage.COMPLETED,
                                groups = event.summary.groups,
                                progress = it.progress.copy(
                                    stage = DuplicateAnalysisStage.COMPLETED,
                                    filesChecked = event.summary.filesChecked,
                                    candidateFiles = event.summary.candidateFiles,
                                    filesHashed = event.summary.filesHashed,
                                    bytesHashed = event.summary.bytesHashed,
                                    groupsConfirmed = event.summary.groups.size,
                                ),
                            )
                        }
                    }
                }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(stage = DuplicateAnalysisStage.CANCELED, errorMessage = "Analysis canceled. Only fully verified groups from this session are shown.") }
            } catch (_: SecurityException) {
                _uiState.update { it.copy(stage = DuplicateAnalysisStage.ERROR, errorMessage = "Full Storage Access was revoked. Restore access to analyze shared storage.") }
            } catch (_: Exception) {
                _uiState.update { it.copy(stage = DuplicateAnalysisStage.ERROR, errorMessage = "Some storage could not be read. Review the verified results or try again.") }
            }
        }
    }

    fun cancelAnalysis() {
        analysisJob?.cancel()
        _uiState.update { it.copy(stage = DuplicateAnalysisStage.CANCELED, errorMessage = "Analysis canceled. Only fully verified groups from this session are shown.") }
    }

    fun setFilter(filter: DuplicateTypeFilter) { _uiState.update { it.copy(filter = filter) } }
    fun setSortOrder(sortOrder: DuplicateSortOrder) { _uiState.update { it.copy(sortOrder = sortOrder) } }
    fun setSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }
    fun clearError() { _uiState.update { it.copy(errorMessage = null) } }

    fun toggleSelection(group: DuplicateGroup, entry: StorageEntry) {
        _uiState.update { state ->
            val selected = state.selectedPaths
            if (entry.path in selected) {
                state.copy(selectedPaths = selected - entry.path)
            } else if (DuplicateRules.canSelect(entry.path, group.entries.map(StorageEntry::path), selected)) {
                state.copy(selectedPaths = selected + entry.path)
            } else {
                state.copy(errorMessage = "Keep at least one copy of this duplicate group.")
            }
        }
    }

    fun keepThisCopy(group: DuplicateGroup, survivor: StorageEntry) {
        _uiState.update { state ->
            state.copy(selectedPaths = DuplicateRules.keepThisCopy(group.entries.map(StorageEntry::path), survivor.path, state.selectedPaths))
        }
    }

    fun clearSelection() { _uiState.update { it.copy(selectedPaths = emptySet()) } }

    fun requestDelete(coordinator: MediaStoreDeleteCoordinator) {
        if (_uiState.value.isDeleting) return
        if (!accessCoordinator.currentState().allFilesAccessGranted) {
            _uiState.update { it.copy(errorMessage = "Full Storage Access was revoked. Restore access before deleting duplicates.") }
            return
        }
        val selected = _uiState.value.selectedEntries
        if (selected.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
            try {
                val stale = withContext(Dispatchers.IO) {
                    selected.filter { entry ->
                        val file = File(entry.path)
                        !file.isFile || file.length() != entry.sizeBytes || (entry.modifiedAtMillis > 0L && file.lastModified() != entry.modifiedAtMillis)
                    }
                }
                val safetyBlocked = withContext(Dispatchers.IO) {
                    _uiState.value.groups.flatMap { group ->
                        val selectedInGroup = group.entries.filter { it.path in _uiState.value.selectedPaths }
                        val survivorStillExists = group.entries.any { it.path !in _uiState.value.selectedPaths && File(it.path).isFile }
                        if (selectedInGroup.isNotEmpty() && !survivorStillExists) selectedInGroup else emptyList()
                    }.distinctBy(StorageEntry::path)
                }
                val safe = selected.filter { it !in stale && it !in safetyBlocked }
                if (safe.isEmpty()) {
                    _uiState.update { it.copy(isDeleting = false, errorMessage = "Nothing was deleted. Keep at least one current copy in every group.") }
                    return@launch
                }
                val checked = withContext(Dispatchers.IO) {
                    coordinator.prepare(safe.map { CleanupFile(it.ref, it.sizeBytes) })
                }
                val requested = selected.map { CleanupFile(it.ref, it.sizeBytes) }
                val prepared = PreparedCleanupDeletion(
                    requested = requested,
                    eligible = checked.eligible,
                    missingBeforeAction = stale.plus(safetyBlocked).map { CleanupFile(it.ref, it.sizeBytes) }
                        .plus(checked.missingBeforeAction)
                        .distinctBy(CleanupFile::uri),
                )
                val baseResult = withContext(Dispatchers.IO) { coordinator.deleteImmediatelyAndVerify(prepared) }
                val skipped = stale.size + safetyBlocked.size
                val result = if (skipped > 0) {
                    baseResult.copy(
                        status = CleanupResultStatus.PARTIAL,
                        note = "Some files changed or were skipped to keep one copy of each duplicate group.",
                    )
                } else baseResult
                _uiState.update { it.copy(isDeleting = false, cleanupResult = result, selectedPaths = emptySet()) }
            } catch (_: SecurityException) {
                _uiState.update { it.copy(isDeleting = false, errorMessage = "Storage access changed before deletion. No unsafe deletion was claimed.") }
            } catch (_: Exception) {
                _uiState.update { it.copy(isDeleting = false, errorMessage = "The selected duplicates could not be deleted safely.") }
            }
        }
    }

    fun dismissCleanupResult() {
        _uiState.update { state ->
            val updatedGroups = state.groups.mapNotNull { group ->
                val remaining = group.entries.filter { File(it.path).isFile }
                if (remaining.size > 1) group.copy(entries = remaining) else null
            }
            state.copy(groups = updatedGroups, cleanupResult = null, selectedPaths = emptySet())
        }
    }
}
