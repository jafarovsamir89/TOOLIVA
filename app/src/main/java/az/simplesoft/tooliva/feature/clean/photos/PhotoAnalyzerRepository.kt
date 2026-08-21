package az.simplesoft.tooliva.feature.clean.photos

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import az.simplesoft.tooliva.core.media.MediaThumbnailLoader
import az.simplesoft.tooliva.core.media.ScreenshotClassifier
import az.simplesoft.tooliva.core.storage.FullStorageProvider
import az.simplesoft.tooliva.core.storage.StorageAccessMode
import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageScanEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.collect
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class PhotoAnalyzerEvent(
    val progress: PhotoAnalysisProgress,
    val items: List<PhotoAnalysisItem>,
    val completed: Boolean = false,
)

class PhotoAnalyzerRepository(private val context: Context) {
    private val appContext = context.applicationContext

    fun analyze(accessMode: StorageAccessMode): Flow<PhotoAnalyzerEvent> = flow {
        val images = mutableListOf<AnalyzedMedia>()
        val videos = mutableListOf<AnalyzedMedia>()
        var checked = 0
        var emitted = emptyList<PhotoAnalysisItem>()

        suspend fun addMedia(media: AnalyzedMedia) {
            checked++
            when (media.category) {
                StorageCategory.IMAGE -> {
                    val bitmap = MediaThumbnailLoader.load(appContext, media.uri)
                    val signature = bitmap?.let(::signature)
                    val blurry = bitmap?.let(::blurScore)?.let { it < BLUR_SCORE_THRESHOLD } == true
                    val screenshot = ScreenshotClassifier.isScreenshotCandidate(media.name, media.path, media.parentName)
                    val oldScreenshot = screenshot && media.modifiedAtMillis <= System.currentTimeMillis() - OLD_SCREENSHOT_DAYS * DAY_MILLIS
                    val current = mutableListOf<PhotoAnalysisItem>()
                    if (blurry) current += media.item(PhotoAnalysisKind.BLURRY, confidence = 62)
                    if (oldScreenshot) current += media.item(PhotoAnalysisKind.OLD_SCREENSHOT)
                    if (signature != null) {
                        val similar = images.asSequence().mapNotNull { previous ->
                            previous.signature?.let { other -> previous to hammingDistance(signature, other) }
                        }.minByOrNull { it.second }
                        if (similar != null && similar.second <= SIMILAR_DISTANCE_THRESHOLD) {
                            current += media.item(PhotoAnalysisKind.SIMILAR, confidence = (100 - similar.second * 4).coerceIn(50, 99))
                            if (emitted.none { it.uri == similar.first.uri && it.kind == PhotoAnalysisKind.SIMILAR }) {
                                emitted = emitted + similar.first.item(PhotoAnalysisKind.SIMILAR, confidence = (100 - similar.second * 4).coerceIn(50, 99))
                            }
                        }
                    }
                    images += media.copy(signature = signature)
                    emitted = emitted + current
                }
                StorageCategory.VIDEO -> {
                    videos += media
                    if (media.sizeBytes >= LARGE_VIDEO_BYTES) emitted = emitted + media.item(PhotoAnalysisKind.LARGE_VIDEO)
                }
                else -> Unit
            }
            if (checked % 24 == 0) emit(PhotoAnalyzerEvent(PhotoAnalysisProgress(checked, emitted.size), emitted))
        }

        if (accessMode == StorageAccessMode.FULL) {
            FullStorageProvider(appContext).scan(0L).collect { event ->
                if (event is StorageScanEvent.EntryFound && !event.entry.isDirectory &&
                    (event.entry.category == StorageCategory.IMAGE || event.entry.category == StorageCategory.VIDEO)
                ) addMedia(event.entry.toAnalyzedMedia())
            }
        } else {
            scanMediaStore { media -> addMedia(media) }
        }
        emit(PhotoAnalyzerEvent(PhotoAnalysisProgress(checked, emitted.size), emitted, completed = true))
    }.flowOn(Dispatchers.IO)

    private suspend fun scanMediaStore(add: suspend (AnalyzedMedia) -> Unit) {
        queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, StorageCategory.IMAGE, add)
        queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, StorageCategory.VIDEO, add)
    }

    private suspend fun queryCollection(collection: Uri, category: StorageCategory, add: suspend (AnalyzedMedia) -> Unit) {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.MediaColumns.RELATIVE_PATH else MediaStore.MediaColumns.DATA,
        )
        appContext.contentResolver.query(collection, projection, null, null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC")?.use { cursor ->
            val id = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val name = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val size = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
            val mime = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val modified = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val path = cursor.getColumnIndexOrThrow(projection.last())
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(name) ?: "Unnamed file"
                val relativePath = cursor.getString(path)
                add(
                    AnalyzedMedia(
                        uri = ContentUris.withAppendedId(collection, cursor.getLong(id)),
                        name = displayName,
                        path = relativePath,
                        parentName = relativePath?.trimEnd('/')?.substringAfterLast('/'),
                        sizeBytes = cursor.getLong(size),
                        modifiedAtMillis = cursor.getLong(modified) * 1_000L,
                        mimeType = cursor.getString(mime),
                        category = category,
                    ),
                )
            }
        }
    }

    private fun AnalyzedMedia.item(kind: PhotoAnalysisKind, confidence: Int? = null) = PhotoAnalysisItem(uri, name, sizeBytes, modifiedAtMillis, mimeType, kind, confidence)

    private fun StorageEntry.toAnalyzedMedia() = AnalyzedMedia(ref, name, path, path.substringBeforeLast('/', "").substringAfterLast('/').takeIf { it.isNotBlank() }, sizeBytes, modifiedAtMillis, mimeType, category)

    private fun signature(bitmap: Bitmap): Long {
        val scaled = Bitmap.createScaledBitmap(bitmap, SIGNATURE_SIZE, SIGNATURE_SIZE, true)
        var total = 0L
        for (y in 0 until SIGNATURE_SIZE) for (x in 0 until SIGNATURE_SIZE) {
            val pixel = scaled.getPixel(x, y)
            total += (Color.red(pixel) * 299L + Color.green(pixel) * 587L + Color.blue(pixel) * 114L) / 1000L
        }
        val average = total / (SIGNATURE_SIZE * SIGNATURE_SIZE)
        var result = 0L
        for (y in 0 until SIGNATURE_SIZE) for (x in 0 until SIGNATURE_SIZE) {
            val pixel = scaled.getPixel(x, y)
            val gray = (Color.red(pixel) * 299L + Color.green(pixel) * 587L + Color.blue(pixel) * 114L) / 1000L
            if (gray >= average) result = result or (1L shl (y * SIGNATURE_SIZE + x))
        }
        if (scaled !== bitmap) scaled.recycle()
        return result
    }

    private fun blurScore(bitmap: Bitmap): Double {
        val width = bitmap.width.coerceAtLeast(2)
        val height = bitmap.height.coerceAtLeast(2)
        val stepX = (width / 48).coerceAtLeast(1)
        val stepY = (height / 48).coerceAtLeast(1)
        var sum = 0.0
        var samples = 0
        var y = 0
        while (y + stepY < height) {
            var x = 0
            while (x + stepX < width) {
                val a = gray(bitmap.getPixel(x, y))
                val b = gray(bitmap.getPixel(x + stepX, y))
                val c = gray(bitmap.getPixel(x, y + stepY))
                sum += abs(a - b) + abs(a - c)
                samples++
                x += stepX
            }
            y += stepY
        }
        return if (samples == 0) 0.0 else sum / samples
    }

    private fun gray(pixel: Int): Double = (Color.red(pixel) * 299.0 + Color.green(pixel) * 587.0 + Color.blue(pixel) * 114.0) / 1000.0
    private fun hammingDistance(first: Long, second: Long): Int = java.lang.Long.bitCount(first xor second)

    private data class AnalyzedMedia(
        val uri: Uri,
        val name: String,
        val path: String?,
        val parentName: String?,
        val sizeBytes: Long,
        val modifiedAtMillis: Long,
        val mimeType: String?,
        val category: StorageCategory,
        val signature: Long? = null,
    )

    private companion object {
        const val SIGNATURE_SIZE = 8
        const val SIMILAR_DISTANCE_THRESHOLD = 10
        const val BLUR_SCORE_THRESHOLD = 12.0
        const val LARGE_VIDEO_BYTES = 500L * 1024L * 1024L
        const val OLD_SCREENSHOT_DAYS = 90L
        const val DAY_MILLIS = 86_400_000L
    }
}
