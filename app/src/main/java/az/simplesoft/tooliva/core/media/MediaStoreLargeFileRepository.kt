package az.simplesoft.tooliva.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LargeMediaFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val modifiedEpochSeconds: Long,
)

class MediaStoreLargeFileRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    suspend fun scan(minBytes: Long = DEFAULT_MIN_BYTES): List<LargeMediaFile> = withContext(Dispatchers.IO) {
        buildList {
            addAll(queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, minBytes))
            addAll(queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, minBytes))
        }.sortedByDescending { it.sizeBytes }
    }

    private fun queryCollection(collection: Uri, minBytes: Long): List<LargeMediaFile> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val selection = "${MediaStore.MediaColumns.SIZE} >= ?"
        val selectionArgs = arrayOf(minBytes.toString())
        val sortOrder = "${MediaStore.MediaColumns.SIZE} DESC"

        return buildList {
            resolver.query(collection, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val modifiedIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idIndex)
                    val size = cursor.getLong(sizeIndex)
                    add(
                        LargeMediaFile(
                            uri = ContentUris.withAppendedId(collection, id),
                            displayName = cursor.getString(nameIndex) ?: "Unnamed file",
                            sizeBytes = size,
                            mimeType = cursor.getString(mimeIndex),
                            modifiedEpochSeconds = cursor.getLong(modifiedIndex),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val DEFAULT_MIN_BYTES: Long = 100L * 1024L * 1024L
    }
}
