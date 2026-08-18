package az.simplesoft.tooliva.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class ScreenshotMediaFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val modifiedEpochSeconds: Long,
    val dateTakenMillis: Long,
    val relativePath: String?,
    val bucketDisplayName: String?,
) {
    val ageTimestampMillis: Long
        get() = dateTakenMillis.takeIf { it > 0L } ?: modifiedEpochSeconds * 1_000L
}

object ScreenshotAgeFilter {
    fun isOlderThan(timestampMillis: Long, nowMillis: Long, ageDays: Int): Boolean =
        timestampMillis <= nowMillis - ageDays.coerceAtLeast(1).toLong() * MILLIS_PER_DAY

    private const val MILLIS_PER_DAY = 86_400_000L
}

object ScreenshotClassifier {
    fun isScreenshotCandidate(
        displayName: String?,
        relativePath: String?,
        bucketDisplayName: String?,
    ): Boolean {
        val haystack = listOf(displayName, relativePath, bucketDisplayName)
            .filterNotNull()
            .joinToString("/")
            .lowercase()
        return listOf(
            "screenshot",
            "screen_shot",
            "screen-shot",
            "screenshots",
            "скриншот",
        ).any(haystack::contains)
    }
}

class ScreenshotMediaRepository(context: Context) {
    private val appContext = context.applicationContext
    private val resolver = context.applicationContext.contentResolver

    fun scan(maxAgeDays: Int, fullStorageAccess: Boolean = false): Flow<ScreenshotMediaFile> = if (fullStorageAccess) {
        scanFullStorage(maxAgeDays)
    } else {
        scanMediaStore(maxAgeDays)
    }

    private fun scanFullStorage(maxAgeDays: Int): Flow<ScreenshotMediaFile> = flow {
        val now = System.currentTimeMillis()
        FullStorageProvider(appContext).scan(minBytes = 0L).collect { event ->
            if (event is StorageScanEvent.EntryFound && !event.entry.isDirectory && event.entry.category == StorageCategory.IMAGE) {
                val path = event.entry.path
                val bucket = path.substringBeforeLast('/', missingDelimiterValue = "").substringAfterLast('/').takeIf { it.isNotBlank() }
                val item = ScreenshotMediaFile(
                    uri = event.entry.ref,
                    displayName = event.entry.name,
                    sizeBytes = event.entry.sizeBytes,
                    mimeType = event.entry.mimeType,
                    modifiedEpochSeconds = event.entry.modifiedAtMillis / 1_000L,
                    dateTakenMillis = 0L,
                    relativePath = path,
                    bucketDisplayName = bucket,
                )
                if (ScreenshotAgeFilter.isOlderThan(item.ageTimestampMillis, now, maxAgeDays) &&
                    ScreenshotClassifier.isScreenshotCandidate(item.displayName, item.relativePath, item.bucketDisplayName)
                ) {
                    emit(item)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    private fun scanMediaStore(maxAgeDays: Int): Flow<ScreenshotMediaFile> = flow {
        val now = System.currentTimeMillis()
        val cutoffTimestamp = now - maxAgeDays.coerceAtLeast(1).toLong() * 86_400_000L
        val pathColumn = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.MediaColumns.RELATIVE_PATH
        } else {
            MediaStore.MediaColumns.DATA
        }
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            pathColumn,
        )
        val selection = "${MediaStore.MediaColumns.DATE_MODIFIED} <= ? OR ${MediaStore.Images.ImageColumns.DATE_TAKEN} <= ? OR ${MediaStore.Images.ImageColumns.DATE_TAKEN} IS NULL"
        val selectionArgs = arrayOf(
            (cutoffTimestamp / 1_000L).toString(),
            cutoffTimestamp.toString(),
        )

        resolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC",
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val takenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)
            val bucketIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME)
            val pathIndex = cursor.getColumnIndexOrThrow(pathColumn)

            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nameIndex)
                val relativePath = cursor.getString(pathIndex)
                val bucket = cursor.getString(bucketIndex)
                val modifiedSeconds = cursor.getLong(modifiedIndex)
                val takenMillis = cursor.getLong(takenIndex)
                val item = ScreenshotMediaFile(
                    uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idIndex)),
                    displayName = displayName ?: "Unnamed screenshot",
                    sizeBytes = cursor.getLong(sizeIndex),
                    mimeType = cursor.getString(mimeIndex),
                    modifiedEpochSeconds = modifiedSeconds,
                    dateTakenMillis = takenMillis,
                    relativePath = relativePath,
                    bucketDisplayName = bucket,
                )
                if (ScreenshotAgeFilter.isOlderThan(item.ageTimestampMillis, now, maxAgeDays) &&
                    ScreenshotClassifier.isScreenshotCandidate(displayName, relativePath, bucket)
                ) {
                    emit(item)
                }
            }
        }
    }.flowOn(Dispatchers.IO)
}
