package az.simplesoft.tooliva.feature.clean.swipe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.feature.files.FileOperationCoordinator
import az.simplesoft.tooliva.feature.files.FileOperationEvent
import az.simplesoft.tooliva.feature.files.FileOperationKind
import az.simplesoft.tooliva.feature.files.FileOperationRequest
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

class CleanupSwipeViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val repository = CleanupSwipeRepository(application)
    private val operations = FileOperationCoordinator(application)
    private val _uiState = MutableStateFlow(CleanupSwipeSnapshot(accessState = accessCoordinator.currentState()))
    val uiState = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var deleteJob: Job? = null

    fun refreshAccess() {
        val access = accessCoordinator.currentState()
        _uiState.update { state ->
            state.copy(
                accessState = access,
                errorMessage = if (access.mode != StorageAccessMode.FULL && (state.session != null || state.isLoading)) {
                    "Full Storage Access was revoked. No cleanup action was started."
                } else state.errorMessage,
            )
        }
    }

    fun load(category: CleanupSwipeCategory) {
        if (_uiState.value.isLoading || _uiState.value.isDeleting) return
        if (accessCoordinator.currentState().mode != StorageAccessMode.FULL) {
            _uiState.update { it.copy(errorMessage = "Full Storage Access is required for Cleanup Swipe categories.") }
            return
        }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(phase = CleanupSwipePhase.LOADING, selectedCategory = category, isLoading = true, entries = emptyList(), session = null, filesChecked = 0L, errorMessage = null) }
            val loaded = mutableListOf<StorageEntry>()
            try {
                repository.scan(category).collect { event ->
                    when (event) {
                        CleanupSwipeLoadEvent.Started -> Unit
                        is CleanupSwipeLoadEvent.Entry -> loaded += event.entry
                        is CleanupSwipeLoadEvent.Progress -> _uiState.update { it.copy(filesChecked = event.filesChecked) }
                        is CleanupSwipeLoadEvent.Warning -> Unit
                        CleanupSwipeLoadEvent.Completed -> Unit
                    }
                }
                val sorted = sortEntries(loaded, _uiState.value.sort)
                _uiState.update {
                    it.copy(
                        phase = CleanupSwipePhase.REVIEW,
                        isLoading = false,
                        entries = sorted,
                        session = CleanupSwipeSession(category, sorted),
                    )
                }
            } catch (_: CancellationException) {
                _uiState.update { it.copy(isLoading = false, phase = CleanupSwipePhase.PICKER) }
            } catch (error: Exception) {
                _uiState.update { it.copy(isLoading = false, phase = CleanupSwipePhase.PICKER, errorMessage = error.message ?: "Unable to load this category.") }
            }
        }
    }

    fun cancelLoading() {
        loadJob?.cancel()
        _uiState.update { it.copy(isLoading = false, phase = CleanupSwipePhase.PICKER, selectedCategory = null) }
    }

    fun setSort(sort: CleanupSwipeSort) {
        _uiState.update { state ->
            val session = state.session ?: return@update state.copy(sort = sort)
            val reordered = sortEntries(session.entries, sort)
            state.copy(sort = sort, entries = reordered, session = session.copy(entries = reordered, currentIndex = 0, decisions = emptyMap(), decisionHistory = emptyList()))
        }
    }

    fun decide(decision: SwipeDecision) = _uiState.update { state -> state.copy(session = state.session?.applyDecision(decision)) }
    fun undo() = _uiState.update { state -> state.copy(session = state.session?.undoLast()) }
    fun openFinalReview() = _uiState.update { it.copy(phase = CleanupSwipePhase.FINAL_REVIEW) }
    fun backToReview() = _uiState.update { it.copy(phase = CleanupSwipePhase.REVIEW) }

    fun unselectForDelete(path: String) = _uiState.update { state ->
        val session = state.session ?: return@update state
        state.copy(session = session.copy(decisions = session.decisions + (path to SwipeDecision.KEEP)))
    }

    fun showDeleteConfirmation() {
        if (_uiState.value.session?.selectedDeleteEntries.isNullOrEmpty()) return
        _uiState.update { it.copy(showFinalConfirmation = true) }
    }

    fun dismissDeleteConfirmation() = _uiState.update { it.copy(showFinalConfirmation = false) }

    fun confirmDelete() {
        val selected = _uiState.value.session?.selectedDeleteEntries.orEmpty()
        if (selected.isEmpty() || _uiState.value.isDeleting) return
        _uiState.update { it.copy(showFinalConfirmation = false, isDeleting = true, errorMessage = null) }
        deleteJob?.cancel()
        deleteJob = viewModelScope.launch {
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    var verifiedResult: CleanupResult? = null
                    operations.execute(
                        FileOperationRequest(FileOperationKind.DELETE, selected.map { File(it.path) }),
                    ).collect { event ->
                        if (event is FileOperationEvent.Finished) verifiedResult = event.result.cleanupResult
                    }
                    verifiedResult
                }
            }.getOrElse { null }
            _uiState.update { it.copy(isDeleting = false, cleanupResult = result ?: CleanupResult(
                status = az.simplesoft.tooliva.core.media.CleanupResultStatus.NO_CHANGE,
                requestedCount = selected.size,
                requestedBytes = selected.sumOf(StorageEntry::sizeBytes),
                removedFromActiveCount = 0,
                removedFromActiveBytes = 0,
                trashedCount = 0,
                trashedBytes = 0,
                freedCount = 0,
                freedBytes = 0,
                missingBeforeCount = 0,
                missingBeforeBytes = 0,
                failedCount = selected.size,
                failedBytes = selected.sumOf(StorageEntry::sizeBytes),
                unchangedCount = selected.size,
                unchangedBytes = selected.sumOf(StorageEntry::sizeBytes),
                note = "The cleanup result could not be verified.",
            )) }
        }
    }

    fun dismissCleanupResult() = _uiState.update { it.copy(cleanupResult = null, phase = CleanupSwipePhase.PICKER, selectedCategory = null, entries = emptyList(), session = null) }
    fun showDetails(entry: StorageEntry) = _uiState.update { it.copy(detailsEntry = entry) }
    fun dismissDetails() = _uiState.update { it.copy(detailsEntry = null) }

    private fun sortEntries(entries: List<StorageEntry>, order: CleanupSwipeSort): List<StorageEntry> = when (order) {
        CleanupSwipeSort.NEWEST -> entries.sortedWith(compareByDescending<StorageEntry> { it.modifiedAtMillis }.thenBy { it.path })
        CleanupSwipeSort.OLDEST -> entries.sortedWith(compareBy<StorageEntry> { it.modifiedAtMillis }.thenBy { it.path })
        CleanupSwipeSort.LARGEST -> entries.sortedWith(compareByDescending<StorageEntry> { it.sizeBytes }.thenBy { it.path })
        CleanupSwipeSort.SMALLEST -> entries.sortedWith(compareBy<StorageEntry> { it.sizeBytes }.thenBy { it.path })
    }
}
