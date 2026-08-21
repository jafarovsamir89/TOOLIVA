package az.simplesoft.tooliva.core.files

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TrashedItem(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val mimeType: String?,
    val expiresAtMillis: Long?,
)

class RecycleBinRepository(context: Context) {
    private val resolver = context.applicationContext.contentResolver
    private val collection = MediaStore.Files.getContentUri("external")

    suspend fun read(): List<TrashedItem> = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@withContext emptyList()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_EXPIRES,
        )
        val args = android.os.Bundle().apply { putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY) }
        resolver.query(collection, projection, args, null)?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val name = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mime = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val expires = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_EXPIRES)
            buildList {
                while (cursor.moveToNext()) {
                    add(TrashedItem(ContentUris.withAppendedId(collection, cursor.getLong(id)), cursor.getString(name) ?: "Unnamed", cursor.getLong(size), cursor.getString(mime), cursor.getLong(expires).takeIf { it > 0L }?.times(1_000L)))
                }
            }
        }.orEmpty()
    }

    suspend fun restore(item: TrashedItem): Boolean = withContext(Dispatchers.IO) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) false else resolver.update(item.uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_TRASHED, 0); put(MediaStore.MediaColumns.DATE_EXPIRES, 0) }, null, null) > 0
    }

    suspend fun permanentlyDelete(item: TrashedItem): Boolean = withContext(Dispatchers.IO) { resolver.delete(item.uri, null, null) > 0 }
}
