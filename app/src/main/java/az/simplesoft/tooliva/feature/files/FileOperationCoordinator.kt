package az.simplesoft.tooliva.feature.files

import android.content.Context
import android.net.Uri
import az.simplesoft.tooliva.core.media.CleanupFile
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.CleanupResultStatus
import az.simplesoft.tooliva.core.media.MediaStoreDeleteCoordinator
import az.simplesoft.tooliva.core.storage.StorageEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import kotlin.coroutines.coroutineContext

sealed interface FileOperationEvent {
    data class Progress(val value: FileOperationProgress) : FileOperationEvent
    data class Finished(val result: FileOperationResult) : FileOperationEvent
}

data class FileOperationRequest(
    val kind: FileOperationKind,
    val sources: List<File>,
    val destination: File? = null,
    val collisionPolicy: CollisionPolicy = CollisionPolicy.KEEP_BOTH,
)

/** Explicit file operations only. It never walks storage until the user starts an operation. */
class FileOperationCoordinator(context: Context) {
    private val appContext = context.applicationContext
    private val deleteCoordinator = MediaStoreDeleteCoordinator(appContext)

    fun execute(request: FileOperationRequest): Flow<FileOperationEvent> = flow {
        val items = request.sources.flatMap { flatten(it) }
        val totalBytes = items.sumOf { if (it.isFile) it.length() else 0L }
        var completedItems = 0
        var completedBytes = 0L
        var skipped = 0
        var failed = 0
        val errors = mutableListOf<String>()

        suspend fun progress(name: String) {
            emit(
                FileOperationEvent.Progress(
                    FileOperationProgress(
                        request.kind, name, completedItems, items.size, completedBytes, totalBytes,
                    ),
                ),
            )
        }

        try {
            if (request.kind == FileOperationKind.DELETE) {
                val fileSources = request.sources.filter { it.isFile }
                val directorySources = request.sources.filter { it.isDirectory }
                val prepared = deleteCoordinator.prepare(fileSources.map { CleanupFile(Uri.fromFile(it), it.length()) })
                val fileResult = deleteCoordinator.deleteImmediatelyAndVerify(prepared)
                var directoryItemCount = 0
                var directoryRequestedItemCount = 0
                var directoryRequestedBytes = 0L
                var directoryBytes = 0L
                var directoryFailedBytes = 0L
                var directoryFailures = 0
                directorySources.forEach { directory ->
                    val before = flatten(directory).filter(File::isFile)
                    val bytes = before.sumOf(File::length)
                    directoryRequestedItemCount += before.size + 1
                    directoryRequestedBytes += bytes
                    if (directory.deleteRecursively()) {
                        directoryItemCount += before.size + 1
                        directoryBytes += bytes
                    } else {
                        directoryFailures++
                        directoryFailedBytes += bytes
                    }
                }
                val directoryResult = if (directorySources.isEmpty()) {
                    fileResult
                } else {
                    val removedCount = fileResult.removedFromActiveCount + directoryItemCount
                    val removedBytes = fileResult.removedFromActiveBytes + directoryBytes
                    val failedCount = fileResult.failedCount + directoryFailures
                    CleanupResult(
                        status = when {
                            failedCount > 0 || fileResult.missingBeforeCount > 0 -> CleanupResultStatus.PARTIAL
                            removedCount == 0 -> CleanupResultStatus.NO_CHANGE
                            else -> CleanupResultStatus.COMPLETED
                        },
                        requestedCount = fileResult.requestedCount + directoryRequestedItemCount,
                        requestedBytes = fileResult.requestedBytes + directoryRequestedBytes,
                        removedFromActiveCount = removedCount,
                        removedFromActiveBytes = removedBytes,
                        trashedCount = fileResult.trashedCount,
                        trashedBytes = fileResult.trashedBytes,
                        freedCount = fileResult.freedCount + directoryItemCount,
                        freedBytes = fileResult.freedBytes + directoryBytes,
                        missingBeforeCount = fileResult.missingBeforeCount,
                        missingBeforeBytes = fileResult.missingBeforeBytes,
                        failedCount = failedCount,
                        failedBytes = fileResult.failedBytes + directoryFailedBytes,
                        unchangedCount = failedCount,
                        unchangedBytes = fileResult.failedBytes + directoryFailedBytes,
                        note = "Folder contents were removed permanently in Full Storage Mode.",
                        itemLabel = "items",
                    )
                }
                emit(
                    FileOperationEvent.Finished(
                        FileOperationResult(
                            kind = request.kind,
                            completedItems = fileResult.removedFromActiveCount + directoryItemCount,
                            completedBytes = fileResult.removedFromActiveBytes + directoryBytes,
                            failedItems = fileResult.failedCount + fileResult.missingBeforeCount + directoryFailures,
                            errors = buildList {
                                if (fileResult.missingBeforeCount > 0) add("Some items disappeared before deletion.")
                                if (fileResult.failedCount > 0 || directoryFailures > 0) add("Some items could not be deleted.")
                            },
                            cleanupResult = directoryResult,
                        ),
                    ),
                )
                return@flow
            }

            val destination = request.destination ?: error("A destination folder is required.")
            if (!destination.exists() && !destination.mkdirs()) error("Destination folder is not writable.")
            if (!destination.isDirectory) error("Destination is not a folder.")

            request.sources.forEach { source ->
                coroutineContext.ensureActive()
                if (!source.exists()) {
                    skipped++
                    errors += "${source.name} is no longer available."
                    return@forEach
                }
                val targetName = resolveCollision(source.name, destination, request.collisionPolicy)
                if (targetName == null) {
                    skipped++
                    progress(source.name)
                    return@forEach
                }
                val target = File(destination, targetName)
                try {
                    val sourceBytes = source.length()
                    if (request.kind == FileOperationKind.MOVE && source.renameTo(target)) {
                        completedItems += 1
                        completedBytes += sourceBytes
                        progress(source.name)
                    } else {
                        copyRecursively(source, target, request.collisionPolicy) { name, bytes ->
                            completedItems++
                            completedBytes += bytes
                            progress(name)
                        }
                        if (request.kind == FileOperationKind.MOVE) {
                            if (!source.deleteRecursively()) throw IOException("Source could not be removed after copy.")
                        }
                    }
                } catch (_: kotlinx.coroutines.CancellationException) {
                    throw kotlinx.coroutines.CancellationException("Operation canceled")
                } catch (error: Exception) {
                    target.deleteRecursively()
                    failed++
                    errors += "${source.name}: ${error.message ?: "operation failed"}"
                    progress(source.name)
                }
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            emit(
                FileOperationEvent.Finished(
                    FileOperationResult(request.kind, completedItems, completedBytes, skipped, failed, errors, canceled = true),
                ),
            )
            return@flow
        } catch (error: Exception) {
            failed++
            errors += error.message ?: "Operation failed."
        }

        emit(
            FileOperationEvent.Finished(
                FileOperationResult(request.kind, completedItems, completedBytes, skipped, failed, errors),
            ),
        )
    }.flowOn(Dispatchers.IO)

    private fun flatten(source: File): List<File> = if (source.isDirectory) {
        listOf(source) + source.listFiles().orEmpty().flatMap(::flatten)
    } else listOf(source)

    private fun resolveCollision(name: String, destination: File, policy: CollisionPolicy): String? {
        val target = File(destination, name)
        if (!target.exists()) return name
        return when (policy) {
            CollisionPolicy.SKIP -> null
            CollisionPolicy.REPLACE -> {
                if (!target.deleteRecursively()) throw IOException("Existing item could not be replaced.")
                name
            }
            CollisionPolicy.KEEP_BOTH -> FileManagerRules.keepBothName(name, destination.list()?.toSet().orEmpty())
        }
    }

    private suspend fun copyRecursively(
        source: File,
        target: File,
        collisionPolicy: CollisionPolicy,
        onFileCopied: suspend (String, Long) -> Unit,
    ) {
        coroutineContext.ensureActive()
        if (source.isDirectory) {
            if (!target.exists() && !target.mkdirs()) throw IOException("Folder could not be created.")
            source.listFiles().orEmpty().forEach { child ->
                coroutineContext.ensureActive()
                val childTarget = File(target, child.name)
                val resolved = if (childTarget.exists()) {
                    when (collisionPolicy) {
                        CollisionPolicy.SKIP -> null
                        CollisionPolicy.REPLACE -> {
                            if (!childTarget.deleteRecursively()) throw IOException("Existing item could not be replaced.")
                            childTarget
                        }
                        CollisionPolicy.KEEP_BOTH -> File(target, FileManagerRules.keepBothName(child.name, target.list()?.toSet().orEmpty()))
                    }
                } else childTarget
                if (resolved != null) copyRecursively(child, resolved, collisionPolicy, onFileCopied)
            }
        } else {
            copyFile(source, target)
            onFileCopied(source.name, target.length())
        }
    }

    private suspend fun copyFile(source: File, target: File) {
        val temp = File(target.parentFile, ".${target.name}.tooliva-partial")
        temp.delete()
        try {
            FileInputStream(source).use { input ->
                FileOutputStream(temp).use { output ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                    output.fd.sync()
                }
            }
            if (!temp.renameTo(target)) {
                throw IOException("Destination could not be finalized.")
            }
        } finally {
            if (temp.exists()) temp.delete()
        }
    }

    private companion object {
        const val COPY_BUFFER_BYTES = 64 * 1024
    }
}
