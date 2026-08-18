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
        var matched = 0L
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
            )
            matched++
            bytes += file.sizeBytes
            emit(StorageScanEvent.EntryFound(entry))
            if (matched % 128L == 0L) emit(StorageScanEvent.Progress(matched, matched, bytes))
        }
        emit(StorageScanEvent.Progress(matched, matched, bytes))
        emit(StorageScanEvent.Completed)
    }.flowOn(Dispatchers.IO)
}
