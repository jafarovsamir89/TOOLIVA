package az.simplesoft.tooliva.core.storage

import android.net.Uri

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

