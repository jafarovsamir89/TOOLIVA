package az.simplesoft.tooliva.core.storage

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

object StorageFileActions {
    fun shareUri(context: Context, entry: StorageEntry): Uri = shareableUri(context, entry.ref)

    fun open(context: Context, entry: StorageEntry) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(shareableUri(context, entry.ref), entry.mimeType ?: "*/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }

    fun share(context: Context, entry: StorageEntry) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = entry.mimeType ?: "*/*"
            putExtra(Intent.EXTRA_STREAM, shareableUri(context, entry.ref))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share file"))
    }

    fun shareableUri(context: Context, uri: Uri): Uri = if (uri.scheme == "file") {
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            File(uri.path.orEmpty()),
        )
    } else {
        uri
    }
}

fun Context.tryOpen(entry: StorageEntry): String? = try {
    StorageFileActions.open(this, entry)
    null
} catch (_: ActivityNotFoundException) {
    "No app can open this file."
} catch (_: IllegalArgumentException) {
    "This file cannot be opened from its current location."
} catch (_: SecurityException) {
    "Android blocked access to this file location."
}

fun Context.tryShare(entry: StorageEntry): String? = try {
    StorageFileActions.share(this, entry)
    null
} catch (_: ActivityNotFoundException) {
    "No app can share this file."
} catch (_: IllegalArgumentException) {
    "This file cannot be shared from its current location."
} catch (_: SecurityException) {
    "Android blocked sharing from this file location."
}
