package az.simplesoft.tooliva.feature.clean.downloads

import android.app.Application
import android.os.Build
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.media.PendingMediaDelete
import az.simplesoft.tooliva.core.media.PreparedCleanupDeletion
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageProvider
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import az.simplesoft.tooliva.core.storage.StorageScanScope
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import az.simplesoft.tooliva.core.storage.storageEntryComparator
import az.simplesoft.tooliva.feature.clean.CleanerBucket
import az.simplesoft.tooliva.feature.clean.CleanerSessionStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class DownloadsCategoryFilter {
    ALL,
    APK,
    ARCHIVE,
    DOCUMENT,
    MEDIA,
    OTHER,
}

data class DownloadsAgeFilter(val days: Int, val label: String)

data class DownloadsSizeFilter(val bytes: Long, val label: String)

data class DownloadsSummary(
    val category: DownloadsCategoryFilter,
    val count: Int,
    val bytes: Long,
)

data class DownloadsAnalyzerUiState(
    val isLoading: Boolean = false,
    val isPreparingDelete: Boolean = false,
    val hasAnalyzed: Boolean = false,
    val files: List<StorageEntry> = emptyList(),
    val selectedRefs: Set<String> = emptySet(),
    val errorMessage: String? = null,
    val pendingDelete: PendingMediaDelete? = null,
    val cleanupResult: CleanupResult? = null,
    val accessState: StorageAccessState = StorageAccessState(
        fullStorageSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R,
        allFilesAccessGranted = false,
    ),
    val categoryFilter: DownloadsCategoryFilter = DownloadsCategoryFilter.ALL,
    val ageFilter: DownloadsAgeFilter? = null,
    val sizeFilter: DownloadsSizeFilter? = null,
    val sortOrder: StorageSortOrder = StorageSortOrder.SIZE,
    val searchQuery: String = "",
    val visitedFiles: Long = 0L,
    val nowMillis: Long = System.currentTimeMillis(),
) {
    val visibleFiles: List<StorageEntry>
        get() = files
            .asSequence()
            .filter { categoryFilter.matches(it) }
            .filter { ageFilter == null || DownloadsAnalyzerRules.isOlderThan(it.modifiedAtMillis, nowMillis, ageFilter.days) }
            .filter { sizeFilter == null || it.sizeBytes >= sizeFilter.bytes }
            .filter {
                searchQuery.isBlank() ||
                    it.name.contains(searchQuery, ignoreCase = true) ||
                    it.path.contains(searchQuery, ignoreCase = true)
            }
            .sortedWith(storageEntryComparator(sortOrder))
            .toList()

    val selectedFiles: List<StorageEntry> = files.filter { it.ref.toString() in selectedRefs }
    val selectedBytes: Long = selectedFiles.sumOf(StorageEntry::sizeBytes)
    val allVisibleSelected: Boolean
        get() = visibleFiles.isNotEmpty() && visibleFiles.all { it.ref.toString() in selectedRefs }

    val summaries: List<DownloadsSummary>
        get() = DownloadsCategoryFilter.entries.map { category ->
            val matching = files.filter(category::matches)
            DownloadsSummary(category, matching.size, matching.sumOf(StorageEntry::sizeBytes))
        }
}

object DownloadsAnalyzerRules {
    const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun isOlderThan(modifiedAtMillis: Long, nowMillis: Long, days: Int): Boolean =
        nowMillis - modifiedAtMillis >= days * DAY_MILLIS

    fun isLargeEnough(sizeBytes: Long, thresholdBytes: Long): Boolean = sizeBytes >= thresholdBytes

    fun analyzerCategory(entry: StorageEntry): DownloadsCategoryFilter = when (entry.category) {
        StorageCategory.APK -> DownloadsCategoryFilter.APK
        StorageCategory.ARCHIVE -> DownloadsCategoryFilter.ARCHIVE
        StorageCategory.DOCUMENT -> DownloadsCategoryFilter.DOCUMENT
        StorageCategory.IMAGE, StorageCategory.VIDEO, StorageCategory.AUDIO -> DownloadsCategoryFilter.MEDIA
        StorageCategory.DOWNLOAD, StorageCategory.OTHER, StorageCategory.ALL -> DownloadsCategoryFilter.OTHER
    }
}

private fun DownloadsCategoryFilter.matches(entry: StorageEntry): Boolean = when (this) {
    DownloadsCategoryFilter.ALL -> true
    else -> DownloadsAnalyzerRules.analyzerCategory(entry) == this
}

internal class DownloadsScanAccumulator {
    private val files = mutableListOf<StorageEntry>()

    fun add(entry: StorageEntry) {
        if (!entry.isDirectory) files += entry
    }

    fun snapshot(): List<StorageEntry> = files.toList()
}

class DownloadsAnalyzerViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val _uiState = MutableStateFlow(
        DownloadsAnalyzerUiState(accessState = accessCoordinator.currentState()),
    )
    val uiState = _uiState.asStateFlow()
    private var scanJob: Job? = null
    private var nextDeleteRequestId = 0L

    init {
        val seeded = CleanerSessionStore.latest?.let { snapshot ->
            (snapshot.entriesFor(CleanerBucket.DOWNLOADS) + snapshot.entriesFor(CleanerBucket.APK_INSTALLERS) + snapshot.entriesFor(CleanerBucket.ARCHIVES) + snapshot.entriesFor(CleanerBucket.DOCUMENTS) + snapshot.entriesFor(CleanerBucket.IMAGES) + snapshot.entriesFor(CleanerBucket.VIDEOS) + snapshot.entriesFor(CleanerBucket.AUDIO))
                .distinctBy { it.ref.toString() }
        }.orEmpty()
        if (seeded.isNotEmpty()) _uiState.update { it.copy(hasAnalyzed = true, files = seeded, nowMillis = System.currentTimeMillis()) }
    }

    fun refreshAccess() {
        val latest = accessCoordinator.currentState()
        _uiState.update { state ->
            if (state.accessState == latest) state else state.copy(
                accessState = latest,
                hasAnalyzed = false,
                files = emptyList(),
                selectedRefs = emptySet(),
                errorMessage = null,
            )
        }
    }

    fun analyze() {
        if (_uiState.value.isLoading || _uiState.value.isPreparingDelete) return
        if (_uiState.value.accessState.mode != StorageAccessMode.FULL) {
            _uiState.update { it.copy(errorMessage = "Full Storage Access is required for automatic Downloads analysis.") }
            return
        }
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    hasAnalyzed = true,
                    files = emptyList(),
                    selectedRefs = emptySet(),
                    errorMessage = null,
                    visitedFiles = 0L,
                )
            }
            try {
                val accumulator = DownloadsScanAccumulator()
                var lastUiPublishAt = 0L
                var visitedFiles = 0L
                fun publishProgress(force: Boolean = false) {
                    val now = SystemClock.uptimeMillis()
                    if (force || now - lastUiPublishAt >= UI_UPDATE_INTERVAL_MS) {
                        lastUiPublishAt = now
                        _uiState.update { it.copy(files = accumulator.snapshot(), visitedFiles = visitedFiles) }
                    }
                }
                provider().scan(0L, StorageScanScope.DOWNLOADS).collect { event ->
                    when (event) {
                        StorageScanEvent.Started -> Unit
                        is StorageScanEvent.EntryFound -> {
                            accumulator.add(event.entry)
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
                updateScannedFiles(accumulator.snapshot())
            } catch (cancellation: CancellationException) {
                _uiState.update { it.copy(isLoading = false) }
                throw cancellation
            } catch (security: SecurityException) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Full Storage Access was revoked. Grant it again to analyze Downloads.",
                    )
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to analyze Downloads.") }
            }
        }
    }

    fun cancelAnalyze() = scanJob?.cancel()

    fun setCategoryFilter(filter: DownloadsCategoryFilter) = _uiState.update { it.copy(categoryFilter = filter) }
    fun setAgeFilter(filter: DownloadsAgeFilter?) = _uiState.update { it.copy(ageFilter = filter) }
    fun setSizeFilter(filter: DownloadsSizeFilter?) = _uiState.update { it.copy(sizeFilter = filter) }
    fun setSortOrder(order: StorageSortOrder) = _uiState.update { it.copy(sortOrder = order) }
    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun toggleSelection(ref: String) = _uiState.update { state ->
        val selected = if (ref in state.selectedRefs) state.selectedRefs - ref else state.selectedRefs + ref
        state.copy(selectedRefs = selected)
    }

    fun toggleSelectAllVisible() = _uiState.update { state ->
        val visible = state.visibleFiles.map { it.ref.toString() }.toSet()
        val selected = if (state.allVisibleSelected) state.selectedRefs - visible else state.selectedRefs + visible
        state.copy(selectedRefs = selected)
    }

    fun showError(message: String) = _uiState.update { it.copy(errorMessage = message) }

    fun requestDelete(coordinator: MediaStoreDeleteCoordinator) {
        if (_uiState.value.isPreparingDelete) return
        val selected = _uiState.value.selectedFiles.map { CleanupFile(it.ref, it.sizeBytes) }
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
                        _uiState.update {
                            it.copy(
                                isPreparingDelete = false,
                                pendingDelete = PendingMediaDelete(nextDeleteRequestId, prepared, sender),
                            )
                        }
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
            } else {
                CleanupResult.canceled(pending.prepared)
            }
            finishWithResult(result)
        }
    }

    fun dismissCleanupResult() = _uiState.update { it.copy(cleanupResult = null) }

    private fun provider(): StorageProvider = FullStorageProvider(getApplication())

    private fun finishWithResult(result: CleanupResult) {
        _uiState.update {
            it.copy(
                isLoading = true,
                isPreparingDelete = false,
                pendingDelete = null,
                files = emptyList(),
                selectedRefs = emptySet(),
                cleanupResult = result,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                val accumulator = DownloadsScanAccumulator()
                provider().scan(0L, StorageScanScope.DOWNLOADS).collect { event ->
                    if (event is StorageScanEvent.EntryFound) accumulator.add(event.entry)
                }
                updateScannedFiles(accumulator.snapshot())
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Cleanup finished, but Downloads could not be refreshed.") }
            }
        }
    }

    private fun updateScannedFiles(files: List<StorageEntry>) {
        val ids = files.map { it.ref.toString() }.toSet()
        _uiState.update {
            it.copy(
                isLoading = false,
                files = files,
                selectedRefs = it.selectedRefs.intersect(ids),
            )
        }
    }

    private companion object {
        const val UI_UPDATE_INTERVAL_MS = 250L
    }
}
