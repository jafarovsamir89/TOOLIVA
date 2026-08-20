package az.simplesoft.tooliva.feature.notifications

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.notifications.NotificationHistoryAppCount
import az.simplesoft.tooliva.core.notifications.NotificationHistoryEntity
import az.simplesoft.tooliva.core.notifications.NotificationHistoryPreferences
import az.simplesoft.tooliva.core.notifications.NotificationHistoryRange
import az.simplesoft.tooliva.core.notifications.NotificationHistoryRepository
import az.simplesoft.tooliva.core.notifications.NotificationRetention
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotificationHistoryUiState(
    val accessGranted: Boolean = false,
    val entries: List<NotificationHistoryEntity> = emptyList(),
    val appCounts: List<NotificationHistoryAppCount> = emptyList(),
    val query: String = "",
    val range: NotificationHistoryRange = NotificationHistoryRange.ALL,
    val packageFilter: String? = null,
    val paused: Boolean = false,
    val includeOngoing: Boolean = false,
    val retention: NotificationRetention = NotificationRetention.THIRTY_DAYS,
    val excludedPackages: Set<String> = emptySet(),
    val detailsId: Long? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

class NotificationHistoryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = NotificationHistoryRepository(application)
    private val preferences: NotificationHistoryPreferences = repository.preferences()
    private val _uiState = MutableStateFlow(NotificationHistoryUiState())
    val uiState = _uiState.asStateFlow()
    private var historyJob: Job? = null
    private var appsJob: Job? = null

    init { refreshAccess() }

    fun refreshAccess() {
        _uiState.update {
            it.copy(
                accessGranted = repository.isAccessGranted(),
                paused = preferences.paused,
                includeOngoing = preferences.includeOngoing,
                retention = preferences.retention,
                excludedPackages = preferences.excludedPackages(),
            )
        }
        observe()
    }

    private fun observe() {
        historyJob?.cancel()
        appsJob?.cancel()
        historyJob = viewModelScope.launch {
            repository.observeHistory(_uiState.value.query, _uiState.value.range, _uiState.value.packageFilter).collect { entries ->
                _uiState.update { it.copy(entries = entries, isLoading = false) }
            }
        }
        appsJob = viewModelScope.launch {
            repository.observeAppCounts(_uiState.value.range).collect { counts ->
                _uiState.update { it.copy(appCounts = counts) }
            }
        }
    }

    fun setQuery(query: String) { _uiState.update { it.copy(query = query) }; observe() }
    fun setRange(range: NotificationHistoryRange) { _uiState.update { it.copy(range = range, packageFilter = if (range == NotificationHistoryRange.PINNED) null else it.packageFilter) }; observe() }
    fun setPackageFilter(packageName: String?) { _uiState.update { it.copy(packageFilter = packageName) }; observe() }
    fun openDetails(id: Long) = _uiState.update { it.copy(detailsId = id) }
    fun closeDetails() = _uiState.update { it.copy(detailsId = null) }

    fun setPaused(value: Boolean) {
        preferences.paused = value
        _uiState.update { it.copy(paused = value) }
    }

    fun setIncludeOngoing(value: Boolean) {
        preferences.includeOngoing = value
        _uiState.update { it.copy(includeOngoing = value) }
    }

    fun setRetention(value: NotificationRetention) {
        preferences.retention = value
        _uiState.update { it.copy(retention = value) }
        viewModelScope.launch { repository.pruneExpired() }
    }

    fun excludePackage(packageName: String, deleteExisting: Boolean) {
        preferences.setExcluded(packageName, true)
        _uiState.update { it.copy(excludedPackages = preferences.excludedPackages()) }
        if (deleteExisting) viewModelScope.launch { repository.deleteForPackage(packageName) }
    }

    fun includePackage(packageName: String) {
        preferences.setExcluded(packageName, false)
        _uiState.update { it.copy(excludedPackages = preferences.excludedPackages()) }
    }

    fun togglePinned(entry: NotificationHistoryEntity) = viewModelScope.launch { repository.setPinned(entry.id, !entry.isPinned) }
    fun deleteEntry(entry: NotificationHistoryEntity) = viewModelScope.launch { repository.delete(entry) }
    fun clearForPackage(packageName: String) = viewModelScope.launch { repository.deleteForPackage(packageName) }
    fun clearAll() = viewModelScope.launch { repository.deleteAll() }
    fun accessIntent(): Intent = repository.accessIntent()
    fun details(): NotificationHistoryEntity? = _uiState.value.entries.firstOrNull { it.id == _uiState.value.detailsId }
}
