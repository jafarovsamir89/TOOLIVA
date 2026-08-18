package az.simplesoft.tooliva.core.storage

import kotlinx.coroutines.flow.Flow

sealed interface StorageScanEvent {
    data object Started : StorageScanEvent
    data class RootStarted(val volumeId: String) : StorageScanEvent
    data class EntryFound(val entry: StorageEntry) : StorageScanEvent
    data class Progress(
        val filesDiscovered: Long,
        val foldersVisited: Long,
        val indexedBytes: Long,
        val currentPath: String? = null,
    ) : StorageScanEvent
    data class Warning(val reason: StorageScanWarning = StorageScanWarning.UNREADABLE_ENTRY) : StorageScanEvent
    data class RootCompleted(val volumeId: String, val completedSuccessfully: Boolean) : StorageScanEvent
    data class Completed(val successfulVolumes: Set<String>) : StorageScanEvent
}

enum class StorageScanWarning {
    UNREADABLE_ENTRY,
    ENTRY_CHANGED,
    ACCESS_REVOKED,
}

interface StorageProvider {
    val accessMode: StorageAccessMode

    fun scan(minBytes: Long): Flow<StorageScanEvent>
}
