package az.simplesoft.tooliva.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.util.Size
import android.net.Uri

/** Loads only a sampled/thumbnail-sized bitmap for visible media rows. */
object MediaThumbnailLoader {
    fun load(context: Context, uri: Uri): Bitmap? = runCatching {
        val resolver = context.contentResolver
        if (uri.scheme == "file") {
            resolver.openInputStream(uri)?.use { input ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, bounds)
                val sample = calculateSample(bounds.outWidth, bounds.outHeight, TARGET_SIZE)
                resolver.openInputStream(uri)?.use { secondInput ->
                    BitmapFactory.decodeStream(secondInput, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            resolver.loadThumbnail(uri, Size(TARGET_SIZE, TARGET_SIZE), null)
        } else {
            resolver.openInputStream(uri)?.use { input ->
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(input, null, bounds)
                val sample = calculateSample(bounds.outWidth, bounds.outHeight, TARGET_SIZE)
                resolver.openInputStream(uri)?.use { secondInput ->
                    BitmapFactory.decodeStream(secondInput, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }
        }
    }.getOrNull()

    private fun calculateSample(width: Int, height: Int, target: Int): Int {
        var sample = 1
        while (width / sample > target || height / sample > target) sample *= 2
        return sample
    }

    private const val TARGET_SIZE = 480
}
