package az.simplesoft.tooliva.feature.clean

import android.net.Uri
import az.simplesoft.tooliva.core.media.ScreenshotClassifier
import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry
import java.io.File

enum class CleanerBucket(
    val title: String,
    val explanation: String,
    val priority: Int,
    val route: String,
) {
    LARGE_FILES("Large files", "Large files are reviewable space, not automatically junk.", 0, "large-files"),
    DOWNLOADS("Downloads", "Files in Downloads need your review before removal.", 1, "downloads"),
    APK_INSTALLERS("APK installers", "APK files are installers; removing one does not uninstall its app.", 2, "downloads"),
    ARCHIVES("Archives", "ZIP and similar archives are shown for review only.", 3, "downloads"),
    DOCUMENTS("Documents", "Documents are personal files and are never treated as junk.", 4, "downloads"),
    IMAGES("Images", "Images are personal files and are never treated as junk.", 5, "screenshots"),
    VIDEOS("Videos", "Videos are personal files and are never treated as junk.", 6, "large-files"),
    AUDIO("Audio", "Audio is shown as reviewable storage.", 7, "large-files"),
    SCREENSHOTS("Screenshots", "Screenshot candidates are identified from MediaStore names and locations.", 8, "screenshots"),
    OLD_FILES("Old files", "Conservative age and location rules; nothing is selected automatically.", 9, "old-files"),
    EMPTY_FOLDERS("Empty folders", "Only currently empty, safe, accessible folders are listed.", 10, "empty-folders"),
    RESIDUALS("Residual candidates", "Only old temporary download fragments are included.", 11, "recommendations"),
}

data class CleanerBucketSummary(
    val bucket: CleanerBucket,
    val count: Int,
    val bytes: Long,
)

data class CleanerAnalysisSnapshot(
    val summaries: List<CleanerBucketSummary> = emptyList(),
    val entries: Map<CleanerBucket, List<StorageEntry>> = emptyMap(),
    val filesChecked: Long = 0L,
    val foldersChecked: Long = 0L,
    val bytesChecked: Long = 0L,
    val warnings: Int = 0,
    val cancelled: Boolean = false,
) {
    fun entriesFor(bucket: CleanerBucket): List<StorageEntry> = entries[bucket].orEmpty()
}

/** Short-lived reuse only: avoids immediately repeating the same traversal after Action Plan navigation. */
object CleanerSessionStore {
    var latest: CleanerAnalysisSnapshot? = null
}

internal class CleanerAnalysisAccumulator(
    private val nowMillis: Long,
    private val oldDays: Int = 180,
) {
    private val entries = linkedMapOf<CleanerBucket, LinkedHashMap<String, StorageEntry>>()
    private val counts = linkedMapOf<CleanerBucket, Int>()
    private val bytesByBucket = linkedMapOf<CleanerBucket, Long>()
    var filesChecked: Long = 0L
        private set
    var foldersChecked: Long = 0L
        private set
    var bytesChecked: Long = 0L
        private set
    var warnings: Int = 0
        private set

    fun addFile(entry: StorageEntry) {
        if (entry.isDirectory) return
        filesChecked++
        bytesChecked += entry.sizeBytes.coerceAtLeast(0L)
        bucketsFor(entry).forEach { bucket -> add(bucket, entry) }
    }

    fun addDirectory(path: String, isEmpty: Boolean) {
        foldersChecked++
        if (!isEmpty || !CleanerAnalysisRules.isSafeEmptyFolderCandidate(path)) return
        val folder = StorageEntry(
            ref = Uri.fromFile(File(path)),
            name = File(path).name.ifBlank { path },
            path = path,
            category = StorageCategory.OTHER,
            sizeBytes = 0L,
            modifiedAtMillis = File(path).lastModified(),
            mimeType = null,
            extension = null,
            isDirectory = true,
        )
        add(CleanerBucket.EMPTY_FOLDERS, folder)
    }

    fun warning() { warnings++ }

    fun snapshot(cancelled: Boolean = false): CleanerAnalysisSnapshot {
        val copied = entries.mapValues { (_, values) -> values.values.toList() }
        return buildSnapshot(copied, cancelled)
    }

    /** Lightweight snapshot for progress updates; it never copies the candidate lists. */
    fun progressSnapshot(): CleanerAnalysisSnapshot = buildSnapshot(emptyMap(), false)

    private fun buildSnapshot(
        snapshotEntries: Map<CleanerBucket, List<StorageEntry>>,
        cancelled: Boolean,
    ): CleanerAnalysisSnapshot {
        return CleanerAnalysisSnapshot(
            summaries = CleanerBucket.entries.mapNotNull { bucket ->
                val count = counts[bucket] ?: 0
                if (count == 0) null else CleanerBucketSummary(bucket, count, bytesByBucket[bucket] ?: 0L)
            }.sortedBy { it.bucket.priority },
            entries = snapshotEntries,
            filesChecked = filesChecked,
            foldersChecked = foldersChecked,
            bytesChecked = bytesChecked,
            warnings = warnings,
            cancelled = cancelled,
        )
    }

    private fun add(bucket: CleanerBucket, entry: StorageEntry) {
        val bucketEntries = entries.getOrPut(bucket) { linkedMapOf() }
        if (bucketEntries.putIfAbsent(entry.ref.toString(), entry) == null) {
            counts[bucket] = (counts[bucket] ?: 0) + 1
            bytesByBucket[bucket] = (bytesByBucket[bucket] ?: 0L) + entry.sizeBytes.coerceAtLeast(0L)
        }
    }

    private fun bucketsFor(entry: StorageEntry): Set<CleanerBucket> = CleanerAnalysisRules.bucketsFor(
        path = entry.path,
        name = entry.name,
        category = entry.category,
        sizeBytes = entry.sizeBytes,
        extension = entry.extension,
        modifiedAtMillis = entry.modifiedAtMillis,
        isDirectory = entry.isDirectory,
        nowMillis = nowMillis,
        oldDays = oldDays,
    )

    companion object {
        const val LARGE_FILE_BYTES = 100L * 1024L * 1024L
    }
}

object CleanerAnalysisRules {
    private const val DAY_MILLIS = 86_400_000L
    private val residualExtensions = setOf("tmp", "temp", "part", "partial", "download")

    fun bucketsFor(
        path: String,
        name: String,
        category: StorageCategory,
        sizeBytes: Long,
        extension: String?,
        modifiedAtMillis: Long,
        isDirectory: Boolean,
        nowMillis: Long,
        oldDays: Int,
    ): Set<CleanerBucket> = buildSet {
        val inDownloads = isDownloadPath(path)
        if (sizeBytes >= CleanerAnalysisAccumulator.LARGE_FILE_BYTES) add(CleanerBucket.LARGE_FILES)
        if (inDownloads) add(CleanerBucket.DOWNLOADS)
        when (category) {
            StorageCategory.APK -> add(CleanerBucket.APK_INSTALLERS)
            StorageCategory.ARCHIVE -> add(CleanerBucket.ARCHIVES)
            StorageCategory.DOCUMENT -> add(CleanerBucket.DOCUMENTS)
            StorageCategory.IMAGE -> add(CleanerBucket.IMAGES)
            StorageCategory.VIDEO -> add(CleanerBucket.VIDEOS)
            StorageCategory.AUDIO -> add(CleanerBucket.AUDIO)
            else -> Unit
        }
        if (category == StorageCategory.IMAGE && ScreenshotClassifier.isScreenshotCandidate(name, path, null)) {
            add(CleanerBucket.SCREENSHOTS)
        }
        if (isOldReviewCandidate(path, name, category, modifiedAtMillis, isDirectory, oldDays, nowMillis)) add(CleanerBucket.OLD_FILES)
        if (isResidualCandidate(path, extension, modifiedAtMillis, isDirectory, nowMillis)) add(CleanerBucket.RESIDUALS)
    }

    fun isDownloadPath(path: String): Boolean = path
        .replace('\\', '/')
        .split('/')
        .any { it.equals("Download", true) || it.equals("Downloads", true) }

    fun isOldReviewCandidate(entry: StorageEntry, ageDays: Int, nowMillis: Long): Boolean =
        isOldReviewCandidate(entry.path, entry.name, entry.category, entry.modifiedAtMillis, entry.isDirectory, ageDays, nowMillis)

    fun isOldReviewCandidate(path: String, name: String, category: StorageCategory, modifiedAtMillis: Long, isDirectory: Boolean, ageDays: Int, nowMillis: Long): Boolean =
        !isDirectory && modifiedAtMillis > 0L && nowMillis - modifiedAtMillis >= ageDays.coerceAtLeast(1) * DAY_MILLIS &&
            (isDownloadPath(path) || category == StorageCategory.APK || category == StorageCategory.ARCHIVE ||
                (category == StorageCategory.IMAGE && ScreenshotClassifier.isScreenshotCandidate(name, path, null)))

    fun isResidualCandidate(entry: StorageEntry, nowMillis: Long): Boolean =
        isResidualCandidate(entry.path, entry.extension, entry.modifiedAtMillis, entry.isDirectory, nowMillis)

    fun isResidualCandidate(path: String, extension: String?, modifiedAtMillis: Long, isDirectory: Boolean, nowMillis: Long): Boolean =
        !isDirectory && isDownloadPath(path) && modifiedAtMillis > 0L && nowMillis - modifiedAtMillis >= 7L * DAY_MILLIS && extension?.lowercase() in residualExtensions

    fun isSafeEmptyFolderCandidate(path: String): Boolean {
        val normalized = path.replace('\\', '/')
        val name = normalized.substringAfterLast('/').lowercase()
        if (normalized.isBlank() || name.isBlank()) return false
        if (normalized.trim('/').split('/').size <= 3) return false
        if (name == "android" || name == "data" || name == "obb" || name == "tooliva") return false
        if (normalized.endsWith("/Android/data") || normalized.contains("/Android/data/") ||
            normalized.endsWith("/Android/obb") || normalized.contains("/Android/obb/")) return false
        return normalized.count { it == '/' } >= 2
    }
}
