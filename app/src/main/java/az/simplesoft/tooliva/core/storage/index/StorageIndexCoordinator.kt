package az.simplesoft.tooliva.core.storage.index

import android.content.Context
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.FullStorageScanPlan
import az.simplesoft.tooliva.core.storage.MediaStoreStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

enum class StorageIndexScanPhase {
    IDLE,
    FAST,
    DEEP,
}

data class StorageIndexCoordinatorState(
    val accessMode: StorageAccessMode? = null,
    val phase: StorageIndexScanPhase = StorageIndexScanPhase.IDLE,
    val status: StorageIndexRunStatus = StorageIndexRunStatus.IDLE,
    val filesDiscovered: Long = 0L,
    val foldersVisited: Long = 0L,
    val indexedBytes: Long = 0L,
    val warningCount: Int = 0,
    val elapsedMillis: Long = 0L,
    val fastScanElapsedMillis: Long? = null,
    val firstResultCount: Int = 0,
    val message: String? = null,
)

/**
 * Process-scoped scan owner. Clean and Large Files share this coordinator, so navigation never
 * starts a second filesystem walk and cached Room rows remain usable while deep indexing runs.
 */
class StorageIndexCoordinator private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val repository = StorageIndexRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val runMutex = Mutex()
    private var runningJob: Job? = null
    private val _state = MutableStateFlow(StorageIndexCoordinatorState())
    val state: StateFlow<StorageIndexCoordinatorState> = _state.asStateFlow()

    fun start(accessMode: StorageAccessMode) {
        scope.launch {
            runMutex.withLock {
                if (runningJob?.isActive == true) return@withLock
                runningJob = kotlinx.coroutines.currentCoroutineContext()[Job]
                try {
                    runScan(accessMode)
                } finally {
                    runningJob = null
                }
            }
        }
    }

    fun cancel() {
        runningJob?.cancel()
    }

    private suspend fun runScan(accessMode: StorageAccessMode) {
        try {
            runScanInternal(accessMode)
        } catch (_: CancellationException) {
            withContext(NonCancellable) {
                updateState {
                    it.copy(
                        phase = StorageIndexScanPhase.IDLE,
                        status = StorageIndexRunStatus.CANCELED,
                        message = "Scan canceled. The last completed results remain available.",
                    )
                }
            }
        } catch (error: Exception) {
            updateState {
                it.copy(
                    phase = StorageIndexScanPhase.IDLE,
                    status = StorageIndexRunStatus.FAILED,
                    message = error.message ?: "Storage scan failed. The last completed results remain available.",
                )
            }
        }
    }

    private suspend fun runScanInternal(accessMode: StorageAccessMode) {
        val cached = repository.lastSuccessfulScan(accessMode)
        if (cached == null) {
            updateState(
                StorageIndexCoordinatorState(
                    accessMode = accessMode,
                    phase = StorageIndexScanPhase.FAST,
                    status = StorageIndexRunStatus.SCANNING,
                    message = "Finding cleanup opportunities…",
                ),
            )
            val fastResult = repository.index(
                provider = provider(accessMode, fast = true),
                onProgress = { progress -> publish(StorageIndexScanPhase.FAST, progress) },
            )
            if (fastResult.status != StorageIndexRunStatus.COMPLETED) return

            val firstResultCount = repository.count(
                StorageIndexQuery(accessMode = accessMode, minimumSizeBytes = LARGE_FILE_THRESHOLD_BYTES),
            )
            updateState {
                it.copy(
                    phase = if (accessMode == StorageAccessMode.FULL) StorageIndexScanPhase.DEEP else StorageIndexScanPhase.IDLE,
                    status = if (accessMode == StorageAccessMode.FULL) StorageIndexRunStatus.SCANNING else StorageIndexRunStatus.COMPLETED,
                    filesDiscovered = fastResult.filesDiscovered,
                    foldersVisited = fastResult.foldersVisited,
                    indexedBytes = fastResult.indexedBytes,
                    warningCount = fastResult.warningCount,
                    elapsedMillis = fastResult.elapsedMillis,
                    fastScanElapsedMillis = fastResult.elapsedMillis,
                    firstResultCount = firstResultCount,
                    message = if (accessMode == StorageAccessMode.FULL) {
                        "Fast results are ready. Deep scan continues in the background."
                    } else {
                        "Limited storage results are ready."
                    },
                )
            }
            if (accessMode != StorageAccessMode.FULL) return
        } else {
            updateState {
                it.copy(
                    accessMode = accessMode,
                    phase = StorageIndexScanPhase.DEEP,
                    status = StorageIndexRunStatus.SCANNING,
                    filesDiscovered = cached.filesDiscovered,
                    foldersVisited = cached.foldersVisited,
                    indexedBytes = cached.indexedBytes,
                    warningCount = cached.warningCount,
                    elapsedMillis = (cached.completedAtMillis ?: cached.startedAtMillis) - cached.startedAtMillis,
                    message = "Refreshing storage in the background…",
                )
            }
        }

        val deepResult = repository.index(
            provider = provider(accessMode, fast = false),
            onProgress = { progress -> publish(StorageIndexScanPhase.DEEP, progress) },
        )
        updateState {
            it.copy(
                phase = StorageIndexScanPhase.IDLE,
                status = deepResult.status,
                filesDiscovered = deepResult.filesDiscovered,
                foldersVisited = deepResult.foldersVisited,
                indexedBytes = deepResult.indexedBytes,
                warningCount = deepResult.warningCount,
                elapsedMillis = deepResult.elapsedMillis,
                message = deepResult.message ?: "Storage results are ready.",
            )
        }
    }

    private suspend fun publish(
        phase: StorageIndexScanPhase,
        progress: StorageIndexProgress,
    ) {
        updateState {
            it.copy(
                accessMode = it.accessMode,
                phase = phase,
                status = progress.status,
                filesDiscovered = progress.filesDiscovered,
                foldersVisited = progress.foldersVisited,
                indexedBytes = progress.indexedBytes,
                warningCount = progress.warningCount,
                elapsedMillis = progress.elapsedMillis,
                message = progress.message,
            )
        }
    }

    private fun provider(accessMode: StorageAccessMode, fast: Boolean) = when (accessMode) {
        StorageAccessMode.FULL -> FullStorageProvider(
            appContext,
            if (fast) FullStorageScanPlan.PRIORITY else FullStorageScanPlan.COMPLETE,
        )
        StorageAccessMode.LIMITED -> MediaStoreStorageProvider(appContext)
    }

    private suspend fun updateState(value: StorageIndexCoordinatorState) {
        _state.emit(value)
    }

    private suspend fun updateState(transform: (StorageIndexCoordinatorState) -> StorageIndexCoordinatorState) {
        _state.emit(transform(_state.value))
    }

    companion object {
        private const val LARGE_FILE_THRESHOLD_BYTES = 100L * 1024L * 1024L
        @Volatile private var instance: StorageIndexCoordinator? = null

        fun getInstance(context: Context): StorageIndexCoordinator = instance ?: synchronized(this) {
            instance ?: StorageIndexCoordinator(context).also { instance = it }
        }
    }
}
