package az.simplesoft.tooliva.core.media

import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

data class ImmediateDeleteResult(
    val requestedCount: Int,
    val deletedCount: Int,
)

/** Keeps user-mediated MediaStore trash/delete behavior out of feature Composables. */
class MediaStoreDeleteCoordinator(context: Context) {
    private val resolver = context.applicationContext.contentResolver

    fun createTrashIntentSender(uris: List<Uri>): IntentSender? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || uris.isEmpty()) return null
        return MediaStore.createTrashRequest(resolver, uris, true).intentSender
    }

    fun deleteImmediately(uris: List<Uri>): ImmediateDeleteResult {
        var deletedCount = 0
        uris.forEach { uri ->
            deletedCount += resolver.delete(uri, null, null)
        }
        return ImmediateDeleteResult(
            requestedCount = uris.size,
            deletedCount = deletedCount,
        )
    }
}
