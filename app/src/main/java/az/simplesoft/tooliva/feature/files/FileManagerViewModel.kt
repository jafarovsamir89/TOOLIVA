package az.simplesoft.tooliva.feature.files

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessCoordinator
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import az.simplesoft.tooliva.core.storage.StorageVolumeInfo
import az.simplesoft.tooliva.core.settings.FavoriteFolder
import az.simplesoft.tooliva.core.settings.RecentFile
import az.simplesoft.tooliva.core.settings.ToolivaUserDataStore
import az.simplesoft.tooliva.core.files.ArchiveDocumentService
import az.simplesoft.tooliva.core.files.FilePreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class FileManagerUiState(
    val accessState: StorageAccessState = StorageAccessState(false, false),
    val volumes: List<StorageVolumeInfo> = emptyList(),
    val currentVolume: StorageVolumeInfo? = null,
    val currentDirectory: File? = null,
    val entries: List<StorageEntry> = emptyList(),
    val sortOrder: StorageSortOrder = StorageSortOrder.NAME,
    val viewMode: FileManagerViewMode = FileManagerViewMode.LIST,
    val searchQuery: String = "",
    val recursiveSearch: Boolean = false,
    val isLoading: Boolean = false,
    val isSearching: Boolean = false,
    val progressVisited: Long = 0L,
    val progressMatches: Long = 0L,
    val error: String? = null,
    val selectedPaths: Set<String> = emptySet(),
    val operationProgress: FileOperationProgress? = null,
    val operationResult: FileOperationResult? = null,
    val cleanupResult: CleanupResult? = null,
    val detailsEntry: StorageEntry? = null,
    val destinationDirectory: File? = null,
    val destinationEntries: List<StorageEntry> = emptyList(),
    val recentFiles: List<RecentFile> = emptyList(),
    val favoriteFolders: List<FavoriteFolder> = emptyList(),
    val preview: FilePreview? = null,
    val operationMessage: String? = null,
)

class FileManagerViewModel(application: Application) : AndroidViewModel(application) {
    private val accessCoordinator = StorageAccessCoordinator(application)
    private val storage = FullStorageProvider(application)
    private val operations = FileOperationCoordinator(application)
    private val userData = ToolivaUserDataStore(application)
    private val archiveService = ArchiveDocumentService()
    private val _uiState = MutableStateFlow(FileManagerUiState())
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()
    private var browseJob: Job? = null
    private var searchJob: Job? = null
    private var operationJob: Job? = null

    init {
        refreshAccess()
        viewModelScope.launch {
            launch { userData.recentFiles.collectLatest { recent -> _uiState.value = _uiState.value.copy(recentFiles = recent) } }
            launch { userData.favoriteFolders.collectLatest { favorites -> _uiState.value = _uiState.value.copy(favoriteFolders = favorites) } }
        }
    }

    fun refreshAccess() {
        val access = accessCoordinator.currentState()
        _uiState.value = _uiState.value.copy(accessState = access)
        if (access.allFilesAccessGranted) {
            viewModelScope.launch(Dispatchers.IO) {
                val volumes = storage.volumeInfos()
                withContext(Dispatchers.Main) {
                    _uiState.value = _uiState.value.copy(volumes = volumes, error = null)
                }
            }
        } else {
            _uiState.value = _uiState.value.copy(volumes = emptyList(), currentVolume = null, currentDirectory = null, entries = emptyList())
        }
    }

    fun openVolume(volume: StorageVolumeInfo) {
        _uiState.value = _uiState.value.copy(currentVolume = volume, currentDirectory = volume.root, selectedPaths = emptySet())
        loadDirectory(volume.root)
    }

    fun openDirectory(directory: File) {
        if (!storage.isAllowedPath(directory) || !directory.isDirectory) {
            _uiState.value = _uiState.value.copy(error = "This folder is unavailable or restricted by Android.")
            return
        }
        val volume = _uiState.value.volumes.firstOrNull { directory.path == it.root.path || directory.path.startsWith(it.root.path + File.separator) }
            ?: return
        _uiState.value = _uiState.value.copy(currentVolume = volume, currentDirectory = directory, selectedPaths = emptySet(), searchQuery = "", recursiveSearch = false)
        loadDirectory(directory)
    }

    fun recordOpened(entry: StorageEntry) {
        if (entry.isDirectory) return
        viewModelScope.launch {
            userData.recordOpenedFile(
                RecentFile(
                    path = entry.path,
                    name = entry.name,
                    openedAtMillis = System.currentTimeMillis(),
                    sizeBytes = entry.sizeBytes,
                ),
            )
        }
    }

    fun toggleFavorite(directory: File) {
        if (!directory.isDirectory || !storage.isAllowedPath(directory)) return
        viewModelScope.launch {
            userData.toggleFavorite(
                FavoriteFolder(
                    path = directory.absolutePath,
                    name = directory.name.ifBlank { directory.absolutePath },
                    addedAtMillis = System.currentTimeMillis(),
                ),
            )
        }
    }

    fun goHome() {
        browseJob?.cancel()
        searchJob?.cancel()
        _uiState.value = _uiState.value.copy(currentVolume = null, currentDirectory = null, entries = emptyList(), selectedPaths = emptySet(), searchQuery = "", recursiveSearch = false, error = null)
    }

    fun goUp() {
        val current = _uiState.value.currentDirectory ?: return
        val root = _uiState.value.currentVolume?.root ?: return
        if (current.absolutePath == root.absolutePath) goHome() else current.parentFile?.let(::openDirectory)
    }

    fun loadDirectory(directory: File? = _uiState.value.currentDirectory) {
        val target = directory ?: return
        browseJob?.cancel()
        browseJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            val result = withContext(Dispatchers.IO) { runCatching { storage.children(target) } }
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                entries = result.getOrElse { emptyList() },
                error = result.exceptionOrNull()?.message,
                selectedPaths = emptySet(),
            )
        }
    }

    fun setSortOrder(order: StorageSortOrder) { _uiState.value = _uiState.value.copy(sortOrder = order) }
    fun setViewMode(mode: FileManagerViewMode) { _uiState.value = _uiState.value.copy(viewMode = mode) }
    fun setSearchQuery(query: String) { _uiState.value = _uiState.value.copy(searchQuery = query, recursiveSearch = false) }

    fun runRecursiveSearch() {
        val directory = _uiState.value.currentDirectory ?: return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val found = mutableListOf<StorageEntry>()
            var visitedFiles = 0L
            var matchedFiles = 0L
            var lastUiPublishAt = 0L
            fun publishProgress(force: Boolean = false) {
                val now = SystemClock.uptimeMillis()
                if (force || now - lastUiPublishAt >= SEARCH_UI_UPDATE_INTERVAL_MS) {
                    lastUiPublishAt = now
                    _uiState.value = _uiState.value.copy(
                        entries = found.toList(),
                        progressVisited = visitedFiles,
                        progressMatches = matchedFiles,
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isSearching = true, recursiveSearch = true, progressVisited = 0, progressMatches = 0, error = null, selectedPaths = emptySet(), entries = emptyList())
            val query = _uiState.value.searchQuery.trim()
            storage.search(directory) { entry -> query.isBlank() || entry.name.contains(query, ignoreCase = true) }
                .collectLatest { event ->
                    when (event) {
                        is StorageScanEvent.EntryFound -> {
                            found += event.entry
                            publishProgress()
                        }
                        is StorageScanEvent.DirectoryVisited -> Unit
                        is StorageScanEvent.Progress -> {
                            visitedFiles = event.visitedFiles
                            matchedFiles = event.matchedFiles
                            publishProgress()
                        }
                        is StorageScanEvent.Warning -> Unit
                        StorageScanEvent.Started -> Unit
                        StorageScanEvent.Completed -> publishProgress(force = true)
                    }
                }
            publishProgress(force = true)
            _uiState.value = _uiState.value.copy(isSearching = false)
        }
    }

    fun runShortcut(shortcut: FileManagerShortcut) {
        if (shortcut == FileManagerShortcut.LARGE) return
        val roots = _uiState.value.volumes.map(StorageVolumeInfo::root)
        val firstRoot = _uiState.value.volumes.firstOrNull()
        if (firstRoot != null) {
            _uiState.value = _uiState.value.copy(currentVolume = firstRoot, currentDirectory = firstRoot.root, entries = emptyList(), selectedPaths = emptySet())
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            val found = mutableListOf<StorageEntry>()
            var visitedFiles = 0L
            var matchedFiles = 0L
            var lastUiPublishAt = 0L
            fun publishProgress(force: Boolean = false) {
                val now = SystemClock.uptimeMillis()
                if (force || now - lastUiPublishAt >= SEARCH_UI_UPDATE_INTERVAL_MS) {
                    lastUiPublishAt = now
                    _uiState.value = _uiState.value.copy(
                        entries = found.toList(),
                        progressVisited = visitedFiles,
                        progressMatches = matchedFiles,
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isSearching = true, recursiveSearch = true, entries = emptyList(), selectedPaths = emptySet(), error = null)
            roots.forEach { root ->
                storage.search(root) { entry -> matchesShortcut(entry, shortcut) }.collectLatest { event ->
                    when (event) {
                        is StorageScanEvent.EntryFound -> {
                            found += event.entry
                            publishProgress()
                        }
                        is StorageScanEvent.DirectoryVisited -> Unit
                        is StorageScanEvent.Progress -> {
                            visitedFiles = event.visitedFiles
                            matchedFiles = event.matchedFiles
                            publishProgress()
                        }
                        else -> Unit
                    }
                }
            }
            publishProgress(force = true)
            _uiState.value = _uiState.value.copy(isSearching = false)
        }
    }

    fun cancelSearch() { searchJob?.cancel(); _uiState.value = _uiState.value.copy(isSearching = false) }

    fun toggleSelection(entry: StorageEntry) {
        val selected = _uiState.value.selectedPaths.toMutableSet()
        if (!selected.add(entry.path)) selected.remove(entry.path)
        _uiState.value = _uiState.value.copy(selectedPaths = selected)
    }

    fun selectAllVisible() {
        val visible = visibleEntries()
        val allSelected = visible.isNotEmpty() && visible.all { it.path in _uiState.value.selectedPaths }
        _uiState.value = _uiState.value.copy(selectedPaths = if (allSelected) emptySet() else visible.mapTo(mutableSetOf(), StorageEntry::path))
    }

    fun clearSelection() { _uiState.value = _uiState.value.copy(selectedPaths = emptySet()) }

    fun showDetails(entry: StorageEntry) { _uiState.value = _uiState.value.copy(detailsEntry = entry) }
    fun dismissDetails() { _uiState.value = _uiState.value.copy(detailsEntry = null) }

    fun preview(entry: StorageEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val preview = runCatching { archiveService.preview(File(entry.path)) }.getOrElse { FilePreview.Unsupported(entry.name, it.message ?: "Preview is unavailable.") }
            withContext(Dispatchers.Main) { _uiState.value = _uiState.value.copy(preview = preview) }
        }
    }

    fun dismissPreview() { _uiState.value = _uiState.value.copy(preview = null) }

    fun extractArchive(entry: StorageEntry) {
        val destination = _uiState.value.currentDirectory ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { archiveService.extractZip(File(entry.path), destination) }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(operationMessage = result.fold({ "Extracted $it file(s) to ${destination.name}." }, { it.message ?: "Archive extraction failed." }))
                loadDirectory(destination)
            }
        }
    }

    fun createZipFromSelection() {
        val destination = _uiState.value.currentDirectory ?: return
        val files = selectedEntries().map { File(it.path) }
        if (files.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val result = runCatching { archiveService.createZip(files, destination) }
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(operationMessage = result.fold({ "Created ${it.name}." }, { it.message ?: "Archive creation failed." }), selectedPaths = emptySet())
                loadDirectory(destination)
            }
        }
    }

    fun dismissOperationMessage() { _uiState.value = _uiState.value.copy(operationMessage = null) }

    fun rename(entry: StorageEntry, newName: String): String? {
        FileManagerRules.validateName(newName)?.let { return it }
        val source = File(entry.path)
        val target = File(source.parentFile, newName.trim())
        if (!storage.isAllowedPath(source) || !storage.isAllowedPath(target)) return "This path is restricted by Android."
        if (target.exists()) return "An item with this name already exists."
        if (!source.renameTo(target)) return "The item could not be renamed."
        loadDirectory()
        return null
    }

    fun createFolder(name: String): String? {
        FileManagerRules.validateName(name)?.let { return it }
        val directory = _uiState.value.currentDirectory ?: return "Open a folder first."
        val target = File(directory, name.trim())
        if (!storage.isAllowedPath(target)) return "This path is restricted by Android."
        if (target.exists()) return "An item with this name already exists."
        if (!target.mkdir()) return "The folder could not be created."
        loadDirectory()
        return null
    }

    fun startOperation(kind: FileOperationKind, destination: File? = null, collision: CollisionPolicy = CollisionPolicy.KEEP_BOTH) {
        val selected = selectedEntries()
        if (selected.isEmpty()) return
        operationJob?.cancel()
        operationJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(operationProgress = null, operationResult = null, error = null)
            operations.execute(FileOperationRequest(kind, selected.map { File(it.path) }, destination, collision)).collectLatest { event ->
                when (event) {
                    is FileOperationEvent.Progress -> _uiState.value = _uiState.value.copy(operationProgress = event.value)
                    is FileOperationEvent.Finished -> {
                        _uiState.value = _uiState.value.copy(operationProgress = null, operationResult = event.result, cleanupResult = event.result.cleanupResult, selectedPaths = emptySet())
                        loadDirectory()
                    }
                }
            }
        }
    }

    fun cancelOperation() { operationJob?.cancel(); _uiState.value = _uiState.value.copy(operationProgress = null) }
    fun dismissOperationResult() { _uiState.value = _uiState.value.copy(operationResult = null, cleanupResult = null) }
    fun dismissCleanupResult() { _uiState.value = _uiState.value.copy(cleanupResult = null, operationResult = null) }

    fun selectedEntries(): List<StorageEntry> = _uiState.value.entries.filter { it.path in _uiState.value.selectedPaths }

    fun visibleEntries(): List<StorageEntry> {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        val filtered = if (state.recursiveSearch || query.isBlank()) state.entries else state.entries.filter { it.name.contains(query, ignoreCase = true) }
        return FileManagerRules.sorted(filtered, state.sortOrder)
    }

    fun selectedBytes(): Long = FileManagerRules.selectedBytes(_uiState.value.entries, _uiState.value.selectedPaths)

    fun openDestination(directory: File) {
        viewModelScope.launch(Dispatchers.IO) {
            val children = runCatching { storage.children(directory) }.getOrDefault(emptyList())
            withContext(Dispatchers.Main) {
                _uiState.value = _uiState.value.copy(destinationDirectory = directory, destinationEntries = children)
            }
        }
    }

    fun clearDestination() { _uiState.value = _uiState.value.copy(destinationDirectory = null, destinationEntries = emptyList()) }

    private fun matchesShortcut(entry: StorageEntry, shortcut: FileManagerShortcut): Boolean = when (shortcut) {
        FileManagerShortcut.DOWNLOADS -> entry.category == StorageCategory.DOWNLOAD || entry.path.contains("/Download/", true)
        FileManagerShortcut.DOCUMENTS -> entry.category == StorageCategory.DOCUMENT
        FileManagerShortcut.APKS -> entry.category == StorageCategory.APK
        FileManagerShortcut.ARCHIVES -> entry.category == StorageCategory.ARCHIVE
        FileManagerShortcut.IMAGES -> entry.category == StorageCategory.IMAGE
        FileManagerShortcut.VIDEOS -> entry.category == StorageCategory.VIDEO
        FileManagerShortcut.AUDIO -> entry.category == StorageCategory.AUDIO
        FileManagerShortcut.RECENT -> entry.modifiedAtMillis >= System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
        FileManagerShortcut.LARGE -> false
    }

    private companion object {
        const val SEARCH_UI_UPDATE_INTERVAL_MS = 250L
    }
}
