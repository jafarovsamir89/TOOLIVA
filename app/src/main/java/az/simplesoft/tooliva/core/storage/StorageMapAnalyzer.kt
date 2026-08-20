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
        storage.scanStorageMap().collect { event ->
            when (event) {
                StorageMapScanEvent.Started -> Unit
                is StorageMapScanEvent.FileFound -> {
                    aggregator.addFile(event.rootPath, event.path, event.sizeBytes)
                }
                is StorageMapScanEvent.Progress -> emit(StorageMapEvent.Progress(aggregator.filesChecked, 0L, aggregator.bytesCounted))
                is StorageMapScanEvent.Warning -> {
                    aggregator.addWarning()
                    emit(StorageMapEvent.Warning(event.path))
                }
                StorageMapScanEvent.Completed -> Unit
            }
        }
        val result = aggregator.build()
        emit(StorageMapEvent.Progress(result.filesChecked, result.foldersFound, result.bytesCounted))
        emit(StorageMapEvent.Completed(result))
    }
}
