package az.simplesoft.tooliva.core.storage.index

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageProvider
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import java.util.UUID

enum class StorageIndexRunStatus {
    IDLE,
    SCANNING,
    COMPLETED,
    CANCELED,
    FAILED,
}

data class StorageIndexProgress(
    val status: StorageIndexRunStatus,
    val generationId: String,
    val filesDiscovered: Long,
    val foldersVisited: Long,
    val indexedBytes: Long,
    val warningCount: Int,
    val currentPath: String? = null,
    val elapsedMillis: Long = 0L,
    val message: String? = null,
)

data class StorageIndexRunResult(
    val status: StorageIndexRunStatus,
    val generationId: String,
    val filesDiscovered: Long,
    val foldersVisited: Long,
    val indexedBytes: Long,
    val warningCount: Int,
    val elapsedMillis: Long,
    val message: String? = null,
)

data class StorageIndexQuery(
    val accessMode: StorageAccessMode,
    val minimumSizeBytes: Long = 0L,
    val category: StorageCategory? = null,
    val searchQuery: String = "",
    val modifiedAfterMillis: Long? = null,
    val modifiedBeforeMillis: Long? = null,
    val parentPath: String? = null,
    val sortOrder: StorageSortOrder = StorageSortOrder.SIZE,
    val limit: Int = DEFAULT_QUERY_LIMIT,
    val offset: Int = 0,
) {
    companion object {
        const val DEFAULT_QUERY_LIMIT = 5_000
    }
}

data class IndexedStorageEntry(
    val stableKey: String,
    val accessMode: StorageAccessMode,
    val volumeId: String,
    val ref: Uri,
    val path: String?,
    val parentPath: String?,
    val displayName: String,
    val extension: String?,
    val mimeType: String?,
    val category: StorageCategory,
    val sizeBytes: Long,
    val modifiedTimeMillis: Long,
    val isDirectory: Boolean,
)

class StorageIndexRepository(
    private val database: StorageIndexDatabase,
    private val clock: () -> Long = { System.currentTimeMillis() },
) {
    constructor(context: Context) : this(
        database = StorageIndexDatabase.getInstance(context),
        clock = { System.currentTimeMillis() },
    )

    private val dao = database.storageIndexDao()

    suspend fun index(
        provider: StorageProvider,
        onProgress: suspend (StorageIndexProgress) -> Unit = {},
    ): StorageIndexRunResult {
        val generationId = UUID.randomUUID().toString()
        val accessMode = provider.accessMode
        val startedAt = clock()
        dao.insertGeneration(
            StorageIndexGenerationEntity(
                generationId = generationId,
                accessMode = accessMode.name,
                startedAtMillis = startedAt,
                completedAtMillis = null,
                status = StorageIndexRunStatus.SCANNING.name,
                filesDiscovered = 0L,
                foldersVisited = 0L,
                indexedBytes = 0L,
                warningCount = 0,
            ),
        )

        var filesDiscovered = 0L
        var foldersVisited = 0L
        var indexedBytes = 0L
        var warningCount = 0
        var currentPath: String? = null
        val successfulVolumes = mutableSetOf<String>()
        var completedEventReceived = false
        val batch = ArrayList<StorageEntry>(BATCH_SIZE)

        suspend fun flushBatch() {
            if (batch.isEmpty()) return
            val entities = batch.map { it.toEntity(accessMode, generationId) }
            batch.clear()
            database.withTransaction {
                val existing = dao.findEntries(entities.map(StorageIndexEntity::stableKey)).associateBy(StorageIndexEntity::stableKey)
                val changed = ArrayList<StorageIndexEntity>()
                entities.forEach { entity ->
                    val previous = existing[entity.stableKey]
                    if (previous != null && previous.hasSameMetadata(entity)) {
                        dao.markSeen(entity.stableKey, generationId)
                    } else {
                        changed += entity
                    }
                }
                if (changed.isNotEmpty()) dao.upsertEntries(changed)
            }
        }

        suspend fun publish(status: StorageIndexRunStatus, message: String? = null) {
            onProgress(
                StorageIndexProgress(
                    status = status,
                    generationId = generationId,
                    filesDiscovered = filesDiscovered,
                    foldersVisited = foldersVisited,
                    indexedBytes = indexedBytes,
                    warningCount = warningCount,
                    currentPath = currentPath,
                    elapsedMillis = (clock() - startedAt).coerceAtLeast(0L),
                    message = message,
                ),
            )
        }

        publish(StorageIndexRunStatus.SCANNING)
        try {
            provider.scan(minBytes = 0L).collect { event ->
                when (event) {
                    StorageScanEvent.Started -> Unit
                    is StorageScanEvent.RootStarted -> currentPath = null
                    is StorageScanEvent.EntryFound -> {
                        batch += event.entry
                        if (!event.entry.isDirectory) {
                            filesDiscovered++
                            indexedBytes += event.entry.sizeBytes
                        }
                    }
                    is StorageScanEvent.Progress -> {
                        filesDiscovered = event.filesDiscovered
                        foldersVisited = event.foldersVisited
                        indexedBytes = event.indexedBytes
                        currentPath = event.currentPath
                        publish(StorageIndexRunStatus.SCANNING)
                    }
                    is StorageScanEvent.Warning -> {
                        warningCount++
                        publish(StorageIndexRunStatus.SCANNING)
                    }
                    is StorageScanEvent.RootCompleted -> {
                        if (event.completedSuccessfully) successfulVolumes += event.volumeId
                    }
                    is StorageScanEvent.Completed -> {
                        successfulVolumes += event.successfulVolumes
                        completedEventReceived = true
                    }
                }
                if (batch.size >= BATCH_SIZE) flushBatch()
            }
            flushBatch()
            check(completedEventReceived) { "Storage provider did not complete its scan." }

            val completedAt = clock()
            database.withTransaction {
                successfulVolumes.forEach { volumeId ->
                    dao.upsertScope(
                        StorageIndexScopeEntity(
                            accessMode = accessMode.name,
                            volumeId = volumeId,
                            activeGeneration = generationId,
                            lastSuccessfulAtMillis = completedAt,
                        ),
                    )
                    dao.deleteStaleEntries(accessMode.name, volumeId, generationId)
                }
                dao.finishGeneration(
                    generationId = generationId,
                    completedAtMillis = completedAt,
                    status = StorageIndexRunStatus.COMPLETED.name,
                    filesDiscovered = filesDiscovered,
                    foldersVisited = foldersVisited,
                    indexedBytes = indexedBytes,
                    warningCount = warningCount,
                )
            }
            publish(StorageIndexRunStatus.COMPLETED)
            return StorageIndexRunResult(
                status = StorageIndexRunStatus.COMPLETED,
                generationId = generationId,
                filesDiscovered = filesDiscovered,
                foldersVisited = foldersVisited,
                indexedBytes = indexedBytes,
                warningCount = warningCount,
                elapsedMillis = (completedAt - startedAt).coerceAtLeast(0L),
            )
        } catch (cancellation: CancellationException) {
            val canceledAt = clock()
            withContext(NonCancellable) {
                dao.finishGeneration(
                    generationId = generationId,
                    completedAtMillis = canceledAt,
                    status = StorageIndexRunStatus.CANCELED.name,
                    filesDiscovered = filesDiscovered,
                    foldersVisited = foldersVisited,
                    indexedBytes = indexedBytes,
                    warningCount = warningCount,
                )
                publish(StorageIndexRunStatus.CANCELED, "Scan canceled. The previous completed index remains active.")
            }
            throw cancellation
        } catch (error: Exception) {
            val failedAt = clock()
            dao.finishGeneration(
                generationId = generationId,
                completedAtMillis = failedAt,
                status = StorageIndexRunStatus.FAILED.name,
                filesDiscovered = filesDiscovered,
                foldersVisited = foldersVisited,
                indexedBytes = indexedBytes,
                warningCount = warningCount,
            )
            val message = error.message ?: "Storage index scan failed. The previous completed index remains active."
            publish(StorageIndexRunStatus.FAILED, message)
            return StorageIndexRunResult(
                status = StorageIndexRunStatus.FAILED,
                generationId = generationId,
                filesDiscovered = filesDiscovered,
                foldersVisited = foldersVisited,
                indexedBytes = indexedBytes,
                warningCount = warningCount,
                elapsedMillis = (failedAt - startedAt).coerceAtLeast(0L),
                message = message,
            )
        }
    }

    suspend fun query(query: StorageIndexQuery): List<IndexedStorageEntry> = dao.queryFiles(
        accessMode = query.accessMode.name,
        minimumSizeBytes = query.minimumSizeBytes,
        category = query.category?.name,
        searchQuery = query.searchQuery,
        modifiedAfterMillis = query.modifiedAfterMillis,
        modifiedBeforeMillis = query.modifiedBeforeMillis,
        parentPath = query.parentPath,
        sortOrder = query.sortOrder.name,
        limit = query.limit.coerceIn(1, StorageIndexQuery.DEFAULT_QUERY_LIMIT),
        offset = query.offset.coerceAtLeast(0),
    ).map(::toDomain)

    suspend fun count(query: StorageIndexQuery): Int = dao.countFiles(
        accessMode = query.accessMode.name,
        minimumSizeBytes = query.minimumSizeBytes,
        category = query.category?.name,
        searchQuery = query.searchQuery,
    )

    suspend fun lastSuccessfulScan(accessMode: StorageAccessMode): StorageIndexGenerationEntity? =
        dao.lastSuccessfulGeneration(accessMode.name)

    suspend fun removeEntriesByRefs(accessMode: StorageAccessMode, refs: Set<String>) {
        if (refs.isNotEmpty()) dao.deleteEntriesByRefs(accessMode.name, refs.toList())
    }

    private fun StorageEntry.toEntity(accessMode: StorageAccessMode, generationId: String): StorageIndexEntity {
        val volume = volumeId ?: "unknown"
        val fullPath = if (accessMode == StorageAccessMode.FULL) path else null
        val uri = if (accessMode == StorageAccessMode.LIMITED) ref.toString() else ref.toString()
        val location = fullPath ?: uri
        return StorageIndexEntity(
            stableKey = StorageIndexKeys.forEntry(accessMode, volume, location),
            accessMode = accessMode.name,
            volumeId = volume,
            canonicalPath = fullPath,
            uriRef = uri,
            parentPath = fullPath?.substringBeforeLast('/', missingDelimiterValue = "").takeIf { !it.isNullOrBlank() },
            displayName = name,
            extension = extension,
            mimeType = mimeType,
            category = category.name,
            sizeBytes = sizeBytes,
            modifiedTimeMillis = modifiedAtMillis,
            isDirectory = isDirectory,
            scanGeneration = generationId,
        )
    }

    private fun StorageIndexEntity.hasSameMetadata(other: StorageIndexEntity): Boolean =
        accessMode == other.accessMode &&
            volumeId == other.volumeId &&
            canonicalPath == other.canonicalPath &&
            uriRef == other.uriRef &&
            parentPath == other.parentPath &&
            displayName == other.displayName &&
            extension == other.extension &&
            mimeType == other.mimeType &&
            category == other.category &&
            sizeBytes == other.sizeBytes &&
            modifiedTimeMillis == other.modifiedTimeMillis &&
            isDirectory == other.isDirectory

    private fun toDomain(entity: StorageIndexEntity): IndexedStorageEntry {
        val mode = runCatching { StorageAccessMode.valueOf(entity.accessMode) }.getOrDefault(StorageAccessMode.LIMITED)
        return IndexedStorageEntry(
            stableKey = entity.stableKey,
            accessMode = mode,
            volumeId = entity.volumeId,
            ref = Uri.parse(entity.uriRef ?: entity.canonicalPath.orEmpty()),
            path = entity.canonicalPath ?: entity.uriRef,
            parentPath = entity.parentPath,
            displayName = entity.displayName,
            extension = entity.extension,
            mimeType = entity.mimeType,
            category = runCatching { StorageCategory.valueOf(entity.category) }.getOrDefault(StorageCategory.OTHER),
            sizeBytes = entity.sizeBytes,
            modifiedTimeMillis = entity.modifiedTimeMillis,
            isDirectory = entity.isDirectory,
        )
    }

    companion object {
        const val BATCH_SIZE = 256
    }
}

object StorageIndexKeys {
    fun forEntry(accessMode: StorageAccessMode, volumeId: String, location: String): String =
        "${accessMode.name}|$volumeId|$location"
}
