package az.simplesoft.tooliva.core.media

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import az.simplesoft.tooliva.core.storage.StorageCategory

data class LargeMediaFile(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val modifiedEpochSeconds: Long,
    val category: StorageCategory = StorageCategory.OTHER,
    val path: String? = null,
)

class MediaStoreLargeFileRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Files.getContentUri("external")
    }

    fun scan(minBytes: Long = DEFAULT_MIN_BYTES): Flow<LargeMediaFile> = flow {
        queryCollection(minBytes).forEach { file ->
            emit(file)
        }
    }.flowOn(Dispatchers.IO)

    private fun queryCollection(minBytes: Long): List<LargeMediaFile> {
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
                            category = classify(
                                cursor.getString(nameIndex).orEmpty(),
                                cursor.getString(mimeIndex),
                            ),
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val DEFAULT_MIN_BYTES: Long = 100L * 1024L * 1024L

        private fun classify(name: String, mimeType: String?): StorageCategory {
            val extension = name.substringAfterLast('.', "").lowercase()
            return when {
                mimeType?.startsWith("video/") == true -> StorageCategory.VIDEO
                mimeType?.startsWith("image/") == true -> StorageCategory.IMAGE
                mimeType?.startsWith("audio/") == true -> StorageCategory.AUDIO
                extension == "apk" -> StorageCategory.APK
                extension in setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso") -> StorageCategory.ARCHIVE
                extension in setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "epub") -> StorageCategory.DOCUMENT
                else -> StorageCategory.OTHER
            }
        }
    }
}
