package az.simplesoft.tooliva.core.storage

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed interface StorageMapEvent {
    data object Started : StorageMapEvent
    data class Progress(val filesChecked: Long, val foldersFound: Long, val bytesCounted: Long) : StorageMapEvent
    data class Warning(val path: String) : StorageMapEvent
    data class Completed(val result: StorageMapResult) : StorageMapEvent
}

class StorageMapAnalyzer(context: Context) {
    private val storage = FullStorageProvider(context.applicationContext)

    fun analyze(): Flow<StorageMapEvent> = flow {
        emit(StorageMapEvent.Started)
        val volumes = storage.volumeInfos().associate { it.root.absolutePath to it.name }
        val aggregator = StorageMapAggregator(volumes)
        storage.scan(0L, StorageScanScope.ALL_STORAGE).collect { event ->
            when (event) {
                StorageScanEvent.Started -> Unit
                is StorageScanEvent.EntryFound -> {
                    val root = event.entry.volumeId ?: return@collect
                    aggregator.addFile(root, event.entry.path, event.entry.sizeBytes)
                }
                is StorageScanEvent.Progress -> emit(StorageMapEvent.Progress(aggregator.filesChecked, 0L, aggregator.bytesCounted))
                is StorageScanEvent.Warning -> {
                    aggregator.addWarning()
                    emit(StorageMapEvent.Warning(event.path))
                }
                StorageScanEvent.Completed -> Unit
            }
        }
        val result = aggregator.build()
        emit(StorageMapEvent.Progress(result.filesChecked, result.foldersFound, result.bytesCounted))
        emit(StorageMapEvent.Completed(result))
    }
}
