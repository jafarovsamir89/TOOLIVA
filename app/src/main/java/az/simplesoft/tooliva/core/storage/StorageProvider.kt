package az.simplesoft.tooliva.core.storage

import kotlinx.coroutines.flow.Flow

sealed interface StorageScanEvent {
    data object Started : StorageScanEvent
    data class EntryFound(val entry: StorageEntry) : StorageScanEvent
    data class Progress(val visitedFiles: Long, val matchedFiles: Long, val matchedBytes: Long) : StorageScanEvent
    data class Warning(val path: String) : StorageScanEvent
    data object Completed : StorageScanEvent
}

interface StorageProvider {
    val accessMode: StorageAccessMode

    fun scan(minBytes: Long): Flow<StorageScanEvent>
}

