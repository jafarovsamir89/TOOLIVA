package az.simplesoft.tooliva.core.storage

import android.content.Context
import az.simplesoft.tooliva.core.media.MediaStoreLargeFileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers

class MediaStoreStorageProvider(context: Context) : StorageProvider {
    private val repository = MediaStoreLargeFileRepository(context)

    override val accessMode: StorageAccessMode = StorageAccessMode.LIMITED

    override fun scan(minBytes: Long): Flow<StorageScanEvent> = flow {
        emit(StorageScanEvent.Started)
        val volumeId = "mediastore:external"
        emit(StorageScanEvent.RootStarted(volumeId))
        var matched = 0L
        var folders = 0L
        var bytes = 0L
        repository.scan(minBytes).collect { file ->
            val entry = StorageEntry(
                ref = file.uri,
                name = file.displayName,
                path = file.uri.toString(),
                category = file.category,
                sizeBytes = file.sizeBytes,
                modifiedAtMillis = file.modifiedEpochSeconds * 1000L,
                mimeType = file.mimeType,
                extension = file.displayName.substringAfterLast('.', "").lowercase().takeIf { it.isNotBlank() },
                volumeId = volumeId,
            )
            matched++
            bytes += file.sizeBytes
            emit(StorageScanEvent.EntryFound(entry))
            if (matched % 128L == 0L) emit(StorageScanEvent.Progress(matched, folders, bytes))
        }
        emit(StorageScanEvent.Progress(matched, folders, bytes))
        emit(StorageScanEvent.RootCompleted(volumeId, true))
        emit(StorageScanEvent.Completed(setOf(volumeId)))
    }.flowOn(Dispatchers.IO)
}
