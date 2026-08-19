package az.simplesoft.tooliva.feature.appmanager

import android.content.Intent
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.appmanager.AppItem
import az.simplesoft.tooliva.core.appmanager.AppManagerRepository
import az.simplesoft.tooliva.core.appmanager.AppManagerSnapshot
import az.simplesoft.tooliva.core.appmanager.AppStorageInfo
import az.simplesoft.tooliva.core.appmanager.AppUsageInfo
import az.simplesoft.tooliva.core.appmanager.filteredAndSortedApps
import az.simplesoft.tooliva.core.appmanager.isRemovable
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class AppManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AppManagerRepository(application)
    private val _uiState = MutableStateFlow(AppManagerSnapshot())
    val uiState: StateFlow<AppManagerSnapshot> = _uiState.asStateFlow()
    private var enrichmentJob: Job? = null
    private val refreshGeneration = AtomicLong(0L)

    fun refresh() {
        val generation = refreshGeneration.incrementAndGet()
        enrichmentJob?.cancel()
        _uiState.update {
            it.copy(
                isLoading = true,
                isEnriching = false,
                usageAccessGranted = repository.isUsageAccessGranted(),
                selectedPackages = emptySet(),
                detailsPackage = null,
                pendingUninstallPackage = null,
                uninstallQueue = emptyList(),
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { repository.readVisibleApps() }
                .onSuccess { items ->
                    if (generation != refreshGeneration.get()) return@onSuccess
                    val usageGranted = repository.isUsageAccessGranted()
                    _uiState.update {
                        it.copy(
                            items = items.map { item ->
                                item.copy(
                                    usage = if (usageGranted) AppUsageInfo.NotLoaded else AppUsageInfo.Unavailable,
                                    storage = if (usageGranted) AppStorageInfo.NotLoaded else AppStorageInfo.Unavailable(az.simplesoft.tooliva.core.appmanager.StorageUnavailableReason.USAGE_ACCESS_REQUIRED),
                                )
                            },
                            isLoading = false,
                            isEnriching = usageGranted,
                            usageAccessGranted = usageGranted,
                        )
                    }
                    if (usageGranted) enrich(items, generation)
                }
                .onFailure { error ->
                    if (generation == refreshGeneration.get()) _uiState.update { it.copy(isLoading = false, isEnriching = false, error = error.message ?: "Installed apps are unavailable.") }
                }
        }
    }

    private fun enrich(items: List<AppItem>, generation: Long) {
        enrichmentJob = viewModelScope.launch {
            val usage = repository.readUsage()
            if (generation != refreshGeneration.get()) return@launch
            _uiState.update { state ->
                state.copy(items = state.items.map { item -> item.copy(usage = usage[item.packageName] ?: AppUsageInfo.Available(null)) })
            }
            repository.readStorageProgressively(items) { packageName, storage ->
                if (generation != refreshGeneration.get()) return@readStorageProgressively
                _uiState.update { state -> state.copy(items = state.items.map { if (it.packageName == packageName) it.copy(storage = storage) else it }) }
            }
            if (generation == refreshGeneration.get()) _uiState.update { it.copy(isEnriching = false) }
        }
    }

    fun refreshAccess() {
        val granted = repository.isUsageAccessGranted()
        if (granted != _uiState.value.usageAccessGranted || _uiState.value.items.isEmpty()) refresh()
        else _uiState.update { it.copy(usageAccessGranted = granted) }
    }

    fun setFilter(filter: az.simplesoft.tooliva.core.appmanager.AppFilter) = _uiState.update { it.copy(filter = filter) }
    fun setRarelyUsedDays(days: Int) = _uiState.update { it.copy(rarelyUsedDays = days) }
    fun setSort(sort: az.simplesoft.tooliva.core.appmanager.AppSort) = _uiState.update { it.copy(sort = sort) }
    fun setSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun clearMessage() = _uiState.update { it.copy(message = null, error = null) }
    fun openDetails(packageName: String) = _uiState.update { it.copy(detailsPackage = packageName) }
    fun closeDetails() = _uiState.update { it.copy(detailsPackage = null) }

    fun toggleSelection(item: AppItem) {
        if (!item.isRemovable()) return
        _uiState.update { state ->
            val next = state.selectedPackages.toMutableSet()
            if (!next.add(item.packageName)) next.remove(item.packageName)
            state.copy(selectedPackages = next)
        }
    }

    fun selectAll(items: List<AppItem>) = _uiState.update { state ->
        state.copy(selectedPackages = items.filter { it.isRemovable() }.mapTo(mutableSetOf()) { it.packageName })
    }

    fun clearSelection() = _uiState.update { it.copy(selectedPackages = emptySet()) }

    fun filteredItems(nowMillis: Long = System.currentTimeMillis()): List<AppItem> {
        val state = _uiState.value
        return filteredAndSortedApps(state.items, state.filter, state.searchQuery, state.sort, nowMillis, state.rarelyUsedDays)
    }

    fun beginUninstallSelected() {
        val queue = _uiState.value.items.filter { it.packageName in _uiState.value.selectedPackages && it.isRemovable() }.map { it.packageName }
        if (queue.isNotEmpty()) beginUninstallQueue(queue)
    }

    fun beginUninstall(packageName: String) {
        val item = _uiState.value.items.firstOrNull { it.packageName == packageName } ?: return
        if (item.isRemovable()) beginUninstallQueue(listOf(packageName))
    }

    private fun beginUninstallQueue(queue: List<String>) {
        _uiState.update { it.copy(pendingUninstallPackage = queue.firstOrNull(), uninstallQueue = queue.drop(1), selectedPackages = emptySet(), message = null) }
    }

    fun consumePendingUninstall(): String? = _uiState.value.pendingUninstallPackage

    fun onUninstallReturned() {
        val packageName = _uiState.value.pendingUninstallPackage ?: return
        val installed = repository.isInstalled(packageName)
        val remaining = _uiState.value.uninstallQueue
        _uiState.update {
            it.copy(
                pendingUninstallPackage = remaining.firstOrNull(),
                uninstallQueue = remaining.drop(1),
                message = if (installed) "Uninstall canceled or not completed." else "${packageName} was removed.",
            )
        }
        if (!installed && remaining.isEmpty()) refresh()
    }

    fun launchIntent(packageName: String): Intent? = repository.launchIntent(packageName)
    fun appInfoIntent(packageName: String): Intent = repository.appInfoIntent(packageName)
    fun usageAccessIntent(): Intent = repository.usageAccessIntent()

    fun currentDetails(): AppItem? = _uiState.value.items.firstOrNull { it.packageName == _uiState.value.detailsPackage }
}
