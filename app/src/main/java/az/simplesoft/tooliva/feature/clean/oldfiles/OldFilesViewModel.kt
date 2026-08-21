package az.simplesoft.tooliva.feature.clean.oldfiles

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.cleanup.CleanupRecommendationRules
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import az.simplesoft.tooliva.core.storage.storageEntryComparator
import az.simplesoft.tooliva.core.media.ScreenshotClassifier
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

enum class OldFilesScope(val label: String) { DOWNLOADS("Downloads"), APK("APK"), ARCHIVES("Archives"), SCREENSHOTS("Screenshots") }
enum class OldFilesSort(val label: String) { OLDEST("Oldest"), LARGEST("Largest"), NEWEST("Newest"), NAME("Name") }
data class OldFilesAge(val days: Int, val label: String)

data class OldFilesUiState(
    val isLoading: Boolean = false,
    val hasAnalyzed: Boolean = false,
    val entries: List<StorageEntry> = emptyList(),
    val selectedRefs: Set<String> = emptySet(),
    val scope: OldFilesScope = OldFilesScope.DOWNLOADS,
    val age: OldFilesAge = OldFilesAge(180, "180+ days"),
    val sort: OldFilesSort = OldFilesSort.OLDEST,
    val search: String = "",
    val visitedFiles: Long = 0L,
    val errorMessage: String? = null,
    val cleanupResult: CleanupResult? = null,
    val accessState: StorageAccessState = StorageAccessState(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R, false),
) {
    val visibleEntries: List<StorageEntry> get() = entries.asSequence()
        .filter { OldFilesRules.inScope(it, scope) }
        .filter { CleanupRecommendationRules.isKnownOld(it.modifiedAtMillis, age.days, System.currentTimeMillis()) }
        .filter { search.isBlank() || it.name.contains(search, true) || it.path.contains(search, true) }
        .sortedWith(storageEntryComparator(sort.toStorageSortOrder())).toList()
    val selectedEntries: List<StorageEntry> get() = entries.filter { it.ref.toString() in selectedRefs }
    val selectedBytes: Long get() = selectedEntries.sumOf { it.sizeBytes }
    val allVisibleSelected: Boolean get() = visibleEntries.isNotEmpty() && visibleEntries.all { it.ref.toString() in selectedRefs }
}

private fun OldFilesSort.toStorageSortOrder(): az.simplesoft.tooliva.core.storage.StorageSortOrder = when (this) {
    OldFilesSort.OLDEST -> az.simplesoft.tooliva.core.storage.StorageSortOrder.OLDEST
    OldFilesSort.LARGEST -> az.simplesoft.tooliva.core.storage.StorageSortOrder.SIZE
    OldFilesSort.NEWEST -> az.simplesoft.tooliva.core.storage.StorageSortOrder.NEWEST
    OldFilesSort.NAME -> az.simplesoft.tooliva.core.storage.StorageSortOrder.NAME
}

object OldFilesRules {
    fun inScope(entry: StorageEntry, scope: OldFilesScope): Boolean = inScope(entry.path, entry.name, entry.category, scope)
    fun inScope(path: String, name: String, category: StorageCategory, scope: OldFilesScope): Boolean = when (scope) {
        OldFilesScope.DOWNLOADS -> CleanupRecommendationRules.isDownloadPath(path)
        OldFilesScope.APK -> category == StorageCategory.APK
        OldFilesScope.ARCHIVES -> category == StorageCategory.ARCHIVE
        OldFilesScope.SCREENSHOTS -> category == StorageCategory.IMAGE && ScreenshotClassifier.isScreenshotCandidate(name, path, null)
    }
}

class OldFilesViewModel(application: Application) : AndroidViewModel(application) {
    private val access = StorageAccessCoordinator(application)
    private val _uiState = MutableStateFlow(OldFilesUiState(accessState = access.currentState()))
    val uiState = _uiState.asStateFlow()
    private var job: Job? = null

    init {
        val seeded = CleanerSessionStore.latest?.let { snapshot ->
            (snapshot.entriesFor(CleanerBucket.DOWNLOADS) + snapshot.entriesFor(CleanerBucket.APK_INSTALLERS) + snapshot.entriesFor(CleanerBucket.ARCHIVES) + snapshot.entriesFor(CleanerBucket.SCREENSHOTS) + snapshot.entriesFor(CleanerBucket.OLD_FILES))
                .distinctBy { it.ref.toString() }
        }.orEmpty()
        if (seeded.isNotEmpty()) _uiState.update { it.copy(hasAnalyzed = true, entries = seeded) }
    }

    fun refreshAccess() { _uiState.update { it.copy(accessState = access.currentState()) } }
    fun setScope(value: OldFilesScope) = _uiState.update { it.copy(scope = value, selectedRefs = emptySet()) }
    fun setAge(value: OldFilesAge) = _uiState.update { it.copy(age = value, selectedRefs = emptySet()) }
    fun setSort(value: OldFilesSort) = _uiState.update { it.copy(sort = value) }
    fun setSearch(value: String) = _uiState.update { it.copy(search = value) }
    fun toggle(ref: String) = _uiState.update { state -> state.copy(selectedRefs = if (ref in state.selectedRefs) state.selectedRefs - ref else state.selectedRefs + ref) }
    fun selectAll() = _uiState.update { state ->
        val visible = state.visibleEntries.map { it.ref.toString() }.toSet()
        state.copy(selectedRefs = if (state.allVisibleSelected) state.selectedRefs - visible else state.selectedRefs + visible)
    }

    fun scan() {
        if (_uiState.value.isLoading) return
        job?.cancel()
        job = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, hasAnalyzed = true, entries = emptyList(), selectedRefs = emptySet(), errorMessage = null, visitedFiles = 0L) }
            val found = mutableListOf<StorageEntry>()
            try {
                FullStorageProvider(getApplication()).scan(0L).collect { event ->
                    when (event) {
                        StorageScanEvent.Started, StorageScanEvent.Completed -> Unit
                        is StorageScanEvent.EntryFound -> { found += event.entry; if (OldFilesRules.inScope(event.entry, _uiState.value.scope)) _uiState.update { it.copy(entries = found.toList()) } }
                        is StorageScanEvent.DirectoryVisited -> Unit
                        is StorageScanEvent.Progress -> _uiState.update { it.copy(visitedFiles = event.visitedFiles) }
                        is StorageScanEvent.Warning -> Unit
                    }
                }
                _uiState.update { it.copy(isLoading = false, entries = found) }
            } catch (_: CancellationException) { _uiState.update { it.copy(isLoading = false) } }
            catch (error: Exception) { _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Unable to scan old files.") } }
        }
    }
    fun cancelScan() { job?.cancel() }

    fun deleteSelected(coordinator: MediaStoreDeleteCoordinator) {
        val selected = _uiState.value.selectedEntries
        if (selected.isEmpty()) return
        viewModelScope.launch {
            try {
                val prepared = withContext(kotlinx.coroutines.Dispatchers.IO) { coordinator.prepare(selected.map { CleanupFile(it.ref, it.sizeBytes) }) }
                val result = withContext(kotlinx.coroutines.Dispatchers.IO) { coordinator.deleteImmediatelyAndVerify(prepared) }
                _uiState.update { state -> state.copy(entries = state.entries.filterNot { it.ref.toString() in state.selectedRefs }, selectedRefs = emptySet(), cleanupResult = result) }
            } catch (_: SecurityException) {
                _uiState.update { it.copy(cleanupResult = CleanupResult.permissionRevoked(az.simplesoft.tooliva.core.media.PreparedCleanupDeletion(selected.map { CleanupFile(it.ref, it.sizeBytes) }, emptyList(), selected.map { CleanupFile(it.ref, it.sizeBytes) }))) }
            } catch (error: Exception) { _uiState.update { it.copy(errorMessage = error.message ?: "Unable to delete selected old files.") } }
        }
    }
    fun dismissResult() = _uiState.update { it.copy(cleanupResult = null) }
}
