package az.simplesoft.tooliva.feature.clean.cache

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.cache.AppCacheStatsReader
import az.simplesoft.tooliva.core.cache.BrowserDiscovery
import az.simplesoft.tooliva.core.cache.CacheAppEntry
import az.simplesoft.tooliva.core.cache.CacheMeasurementState
import az.simplesoft.tooliva.core.cache.CacheSelectionRules
import az.simplesoft.tooliva.core.cache.UsageAccessChecker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CacheCleanerUiState(
    val usageAccessGranted: Boolean = false,
    val isLoading: Boolean = false,
    val entries: List<CacheAppEntry> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val errorMessage: String? = null,
) {
    val measuredTotalBytes: Long get() = CacheSelectionRules.totalMeasuredBytes(entries)
    val selectedBytes: Long get() = CacheSelectionRules.selectedBytes(entries, selectedPackages)
}

class CacheCleanerViewModel(application: Application) : AndroidViewModel(application) {
    private val usageAccess = UsageAccessChecker(application)
    private val discovery = BrowserDiscovery(application.packageManager)
    private val statsReader = AppCacheStatsReader(application)
    private val _uiState = MutableStateFlow(CacheCleanerUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refreshAccessState()
    }

    fun refreshAccessState() {
        _uiState.update {
            it.copy(
                usageAccessGranted = usageAccess.isGranted(),
            )
        }
    }

    fun usageAccessIntent(): Intent = usageAccess.settingsIntent()

    fun manualSettingsIntent(packageName: String): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))

    fun analyze() {
        if (!_uiState.value.usageAccessGranted) return
        if (_uiState.value.isLoading) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val apps = withContext(Dispatchers.IO) { discovery.discover() }
                val measurements = statsReader.readAll(apps)
                apps.map { app ->
                    val measurement = measurements[app.packageName]
                    CacheAppEntry(
                        packageName = app.packageName,
                        appLabel = app.appLabel,
                        category = app.category,
                        cacheBytes = measurement?.bytes,
                        measurementState = measurement?.state ?: CacheMeasurementState.UNAVAILABLE,
                    )
                }.sortedWith(compareByDescending<CacheAppEntry> { it.cacheBytes ?: -1L }.thenBy { it.appLabel.lowercase() })
            }.onSuccess { entries ->
                _uiState.update { it.copy(isLoading = false, entries = entries, selectedPackages = emptySet()) }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message ?: "Cache analysis could not be completed.") }
            }
        }
    }

    fun toggleSelection(packageName: String) = _uiState.update {
        val selected = CacheSelectionRules.toggle(it.selectedPackages, packageName)
        it.copy(selectedPackages = selected, entries = it.entries.map { entry -> entry.copy(selected = entry.packageName in selected) })
    }

    fun selectAll() = _uiState.update {
        val selected = CacheSelectionRules.selectAll(it.entries)
        it.copy(selectedPackages = selected, entries = it.entries.map { entry -> entry.copy(selected = entry.packageName in selected) })
    }

    fun clearSelection() = _uiState.update {
        it.copy(selectedPackages = emptySet(), entries = it.entries.map { entry -> entry.copy(selected = false) })
    }

}
