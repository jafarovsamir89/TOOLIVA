package az.simplesoft.tooliva.feature.files

import az.simplesoft.tooliva.core.storage.StorageEntry
import az.simplesoft.tooliva.core.storage.StorageSortOrder
import az.simplesoft.tooliva.core.media.CleanupResult
import java.util.Locale

enum class FileManagerViewMode { LIST, GRID }

enum class FileManagerShortcut(val title: String) {
    DOWNLOADS("Downloads"),
    DOCUMENTS("Documents"),
    APKS("APKs"),
    ARCHIVES("Archives"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    RECENT("Modified 30 days"),
    LARGE("Large files"),
}

enum class FileOperationKind { COPY, MOVE, DELETE }

enum class CollisionPolicy { SKIP, KEEP_BOTH, REPLACE }

data class FileManagerItem(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val modifiedAtMillis: Long,
    val isDirectory: Boolean,
)

data class FileOperationProgress(
    val kind: FileOperationKind,
    val currentName: String,
    val completedItems: Int,
    val totalItems: Int,
    val completedBytes: Long,
    val totalBytes: Long,
)

data class FileOperationResult(
    val kind: FileOperationKind,
    val completedItems: Int,
    val completedBytes: Long,
    val skippedItems: Int = 0,
    val failedItems: Int = 0,
    val errors: List<String> = emptyList(),
    val canceled: Boolean = false,
    val cleanupResult: CleanupResult? = null,
)

object FileManagerRules {
    fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Name cannot be empty."
            trimmed == "." || trimmed == ".." -> "This name is reserved."
            trimmed.contains('/') || trimmed.contains('\\') -> "Name cannot contain a path separator."
            trimmed.any { it.code < 0x20 } -> "Name contains an unsupported character."
            else -> null
        }
    }

    fun sorted(entries: List<StorageEntry>, order: StorageSortOrder): List<StorageEntry> = entries.sortedWith(
        Comparator { left, right -> compare(order, left.toSortValues(), right.toSortValues()) },
    )

    fun sortedItems(items: List<FileManagerItem>, order: StorageSortOrder): List<FileManagerItem> = items.sortedWith(
        Comparator { left, right -> compare(order, left.toSortValues(), right.toSortValues()) },
    )

    fun selectedBytes(entries: List<StorageEntry>, selected: Set<String>): Long =
        entries.filter { it.path in selected }.sumOf { it.sizeBytes }

    fun selectedBytesItems(items: List<FileManagerItem>, selected: Set<String>): Long =
        items.filter { it.path in selected }.sumOf { it.sizeBytes }

    private fun compare(order: StorageSortOrder, a: SortValues, b: SortValues): Int {
        val folderOrder = (!a.isDirectory).compareTo(!b.isDirectory)
        if (folderOrder != 0) return folderOrder
        return when (order) {
            StorageSortOrder.NAME -> a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT))
            StorageSortOrder.NEWEST -> b.modifiedAtMillis.compareTo(a.modifiedAtMillis)
            StorageSortOrder.OLDEST -> a.modifiedAtMillis.compareTo(b.modifiedAtMillis)
            StorageSortOrder.SIZE -> b.sizeBytes.compareTo(a.sizeBytes)
        }.takeIf { it != 0 } ?: a.name.lowercase(Locale.ROOT).compareTo(b.name.lowercase(Locale.ROOT))
    }

    private fun StorageEntry.toSortValues() = SortValues(name, path, sizeBytes, modifiedAtMillis, isDirectory)
    private fun FileManagerItem.toSortValues() = SortValues(name, path, sizeBytes, modifiedAtMillis, isDirectory)

    private data class SortValues(val name: String, val path: String, val sizeBytes: Long, val modifiedAtMillis: Long, val isDirectory: Boolean)

    fun keepBothName(name: String, existingNames: Set<String>): String {
        if (name !in existingNames) return name
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var index = 1
        var candidate: String
        do {
            candidate = "$base ($index)$extension"
            index++
        } while (candidate in existingNames)
        return candidate
    }
}
