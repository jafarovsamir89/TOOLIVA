package az.simplesoft.tooliva.core.media

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import java.io.File

/** Centralizes preflight, user-mediated Trash/delete requests and post-action verification. */
class MediaStoreDeleteCoordinator(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun prepare(items: List<CleanupFile>): PreparedCleanupDeletion {
        val eligible = mutableListOf<CleanupFile>()
        val missing = mutableListOf<CleanupFile>()
        items.forEach { item ->
            val active = query(item.uri, includeTrashed = false)
            if (active.exists) {
                eligible += item.copy(sizeBytes = active.sizeBytes.takeIf { it > 0L } ?: item.sizeBytes)
            } else {
                missing += item
            }
        }
        return PreparedCleanupDeletion(
            requested = items,
            eligible = eligible,
            missingBeforeAction = missing,
        )
    }

    fun createTrashIntentSender(prepared: PreparedCleanupDeletion): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || prepared.eligible.isEmpty()) return null
        if (prepared.eligible.any { it.uri.scheme == "file" }) return null
        return MediaStore.createTrashRequest(
            resolver,
            prepared.eligible.map(CleanupFile::uri),
            true,
        ).intentSender
    }

    fun deleteImmediatelyAndVerify(prepared: PreparedCleanupDeletion): CleanupResult {
        prepared.eligible.forEach { item ->
            if (item.uri.scheme == "file") {
                File(item.uri.path.orEmpty()).delete()
            } else {
                resolver.delete(item.uri, null, null)
            }
        }
        return verify(prepared, operation = CleanupOperation.DELETE)
    }

    fun verifyTrash(prepared: PreparedCleanupDeletion): CleanupResult =
        verify(prepared, operation = CleanupOperation.TRASH)

    fun noChange(prepared: PreparedCleanupDeletion, note: String): CleanupResult = CleanupResult(
        status = if (prepared.missingBeforeAction.size == prepared.requested.size) {
            CleanupResultStatus.NO_CHANGE
        } else {
            CleanupResultStatus.PARTIAL
        },
        requestedCount = prepared.requested.size,
        requestedBytes = prepared.requestedBytes,
        removedFromActiveCount = 0,
        removedFromActiveBytes = 0,
        trashedCount = 0,
        trashedBytes = 0,
        freedCount = 0,
        freedBytes = 0,
        missingBeforeCount = prepared.missingBeforeAction.size,
        missingBeforeBytes = prepared.missingBeforeBytes,
        failedCount = 0,
        failedBytes = 0,
        unchangedCount = prepared.eligible.size,
        unchangedBytes = prepared.eligibleBytes,
        note = note,
    )

    private fun verify(
        prepared: PreparedCleanupDeletion,
        operation: CleanupOperation,
    ): CleanupResult {
        var trashedCount = 0
        var trashedBytes = 0L
        var freedCount = 0
        var freedBytes = 0L
        var failedCount = 0
        var failedBytes = 0L

        prepared.eligible.forEach { item ->
            val active = query(item.uri, includeTrashed = false)
            val trashed = if (operation == CleanupOperation.TRASH && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                query(item.uri, includeTrashed = true)
            } else {
                MediaEntryState.absent()
            }

            if (trashed.exists && trashed.isTrashed) {
                trashedCount++
                trashedBytes += trashed.sizeBytes.takeIf { it > 0L } ?: item.sizeBytes
            } else if (active.isTrashed && operation == CleanupOperation.TRASH) {
                trashedCount++
                trashedBytes += active.sizeBytes.takeIf { it > 0L } ?: item.sizeBytes
            } else if (active.exists) {
                failedCount++
                failedBytes += item.sizeBytes
            } else {
                freedCount++
                freedBytes += item.sizeBytes
            }
        }

        val removedCount = trashedCount + freedCount
        val removedBytes = trashedBytes + freedBytes
        val status = when {
            removedCount == 0 && prepared.missingBeforeAction.size == prepared.requested.size ->
                CleanupResultStatus.NO_CHANGE
            failedCount > 0 || prepared.missingBeforeAction.isNotEmpty() -> CleanupResultStatus.PARTIAL
            else -> CleanupResultStatus.COMPLETED
        }

        return CleanupResult(
            status = status,
            requestedCount = prepared.requested.size,
            requestedBytes = prepared.requestedBytes,
            removedFromActiveCount = removedCount,
            removedFromActiveBytes = removedBytes,
            trashedCount = trashedCount,
            trashedBytes = trashedBytes,
            freedCount = freedCount,
            freedBytes = freedBytes,
            missingBeforeCount = prepared.missingBeforeAction.size,
            missingBeforeBytes = prepared.missingBeforeBytes,
            failedCount = failedCount,
            failedBytes = failedBytes,
            unchangedCount = failedCount,
            unchangedBytes = failedBytes,
        )
    }

    private fun query(uri: Uri, includeTrashed: Boolean): MediaEntryState {
        if (uri.scheme == "file") {
            val file = File(uri.path.orEmpty())
            return if (file.exists() && file.isFile) {
                MediaEntryState(exists = true, sizeBytes = file.length(), isTrashed = false)
            } else {
                MediaEntryState.absent()
            }
        }
        val projection = buildList {
            add(MediaStore.MediaColumns.SIZE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                add(MediaStore.MediaColumns.IS_TRASHED)
            }
        }.toTypedArray()
        val queryArgs = if (includeTrashed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bundle().apply {
                putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
            }
        } else {
            null
        }

        resolver.query(uri, projection, queryArgs, null)?.use { cursor ->
            if (!cursor.moveToFirst()) return MediaEntryState.absent()
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val trashedIndex = cursor.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
            return MediaEntryState(
                exists = true,
                sizeBytes = sizeIndex.takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getLong) ?: 0L,
                isTrashed = trashedIndex >= 0 && !cursor.isNull(trashedIndex) && cursor.getInt(trashedIndex) != 0,
            )
        }
        return MediaEntryState.absent()
    }

    private enum class CleanupOperation { TRASH, DELETE }

    private data class MediaEntryState(
        val exists: Boolean,
        val sizeBytes: Long,
        val isTrashed: Boolean,
    ) {
        companion object {
            fun absent() = MediaEntryState(exists = false, sizeBytes = 0L, isTrashed = false)
        }
    }
}
