package az.simplesoft.tooliva.core.storage

import android.net.Uri
import java.io.File
import java.util.Locale

enum class StorageAccessMode {
    FULL,
    LIMITED,
}
enum class StorageCategory {
    ALL,
    VIDEO,
    IMAGE,
    AUDIO,
    APK,
    ARCHIVE,
    DOCUMENT,
    DOWNLOAD,
    OTHER,
}

enum class StorageSortOrder {
    SIZE,
    NEWEST,
    OLDEST,
    NAME,
}

data class StorageVolumeInfo(
    val id: String,
    val name: String,
    val root: File,
    val totalBytes: Long,
    val availableBytes: Long,
    val isPrimary: Boolean,
)

data class StorageEntry(
    val ref: Uri,
    val name: String,
    val path: String,
    val category: StorageCategory,
    val sizeBytes: Long,
    val modifiedAtMillis: Long,
    val mimeType: String?,
    val extension: String?,
    val isDirectory: Boolean = false,
    val volumeId: String? = null,
)

data class StorageAccessState(
    val fullStorageSupported: Boolean,
    val allFilesAccessGranted: Boolean,
) {
    val mode: StorageAccessMode
        get() = if (fullStorageSupported && allFilesAccessGranted) {
            StorageAccessMode.FULL
        } else {
            StorageAccessMode.LIMITED
        }
}

/** Stable ordering shared by Cleaner lists so ties never jump around between scans. */
fun storageEntryComparator(order: StorageSortOrder): Comparator<StorageEntry> = when (order) {
    StorageSortOrder.SIZE -> compareByDescending<StorageEntry> { it.sizeBytes }
    StorageSortOrder.NEWEST -> compareByDescending { it.modifiedAtMillis }
    StorageSortOrder.OLDEST -> compareBy { it.modifiedAtMillis }
    StorageSortOrder.NAME -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
}.thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
    .thenBy { it.path.lowercase(Locale.ROOT) }
