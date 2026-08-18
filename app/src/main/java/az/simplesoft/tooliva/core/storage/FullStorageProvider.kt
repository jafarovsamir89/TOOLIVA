package az.simplesoft.tooliva.core.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.coroutineContext

/**
 * Cancellable, single-traversal provider for accessible shared storage.
 *
 * Traversal is deliberately bounded to one producer. It keeps only the current directory
 * stack in memory and lets the index repository perform bounded database batches.
 */
enum class FullStorageScanPlan {
    PRIORITY,
    COMPLETE,
}

class FullStorageProvider(
    context: Context,
    private val scanPlan: FullStorageScanPlan = FullStorageScanPlan.COMPLETE,
) : StorageProvider {
    private val appContext = context.applicationContext

    override val accessMode: StorageAccessMode = StorageAccessMode.FULL

    override fun scan(minBytes: Long): Flow<StorageScanEvent> = flow {
        emit(StorageScanEvent.Started)
        var filesDiscovered = 0L
        var foldersVisited = 0L
        var indexedBytes = 0L
        val successfulVolumes = mutableSetOf<String>()

        roots().forEach { root ->
            coroutineContext.ensureActive()
            val volumeId = runCatching { root.canonicalPath }.getOrElse { root.absolutePath }
            emit(StorageScanEvent.RootStarted(volumeId))
            var rootCompletedSuccessfully = true
            val seenDirectories = HashSet<String>()
            val stack = ArrayDeque<ScanNode>()
            stack.add(ScanNode(root, recurseIntoChildren = scanPlan == FullStorageScanPlan.COMPLETE))

            while (stack.isNotEmpty()) {
                coroutineContext.ensureActive()
                val node = stack.removeLast()
                val directory = node.directory
                val canonicalDirectory = runCatching { directory.canonicalPath }.getOrNull()
                if (canonicalDirectory == null) {
                    emit(StorageScanEvent.Warning(StorageScanWarning.UNREADABLE_ENTRY))
                    continue
                }
                if (!seenDirectories.add(canonicalDirectory) || isProtectedPath(canonicalDirectory)) continue

                foldersVisited++
                val children = runCatching { directory.listFiles() }.getOrNull()
                if (children == null) {
                    rootCompletedSuccessfully = false
                    emit(StorageScanEvent.Warning(StorageScanWarning.UNREADABLE_ENTRY))
                    continue
                }

                val orderedChildren = if (
                    scanPlan == FullStorageScanPlan.PRIORITY && directory.canonicalPath == root.canonicalPath
                ) {
                    children.sortedWith(compareByDescending { priorityRank(it.name) })
                } else {
                    children.toList()
                }
                orderedChildren.forEach { child ->
                    coroutineContext.ensureActive()
                    val absolutePath = child.absolutePath
                    if (isProtectedPath(absolutePath)) return@forEach
                    val symbolicLink = runCatching { Files.isSymbolicLink(child.toPath()) }.getOrNull()
                    if (symbolicLink == null) {
                        emit(StorageScanEvent.Warning(StorageScanWarning.UNREADABLE_ENTRY))
                        return@forEach
                    }
                    if (symbolicLink) return@forEach

                    val isDirectory = runCatching { child.isDirectory }.getOrNull()
                    if (isDirectory == null) {
                        emit(StorageScanEvent.Warning(StorageScanWarning.UNREADABLE_ENTRY))
                        return@forEach
                    }
                    if (isDirectory) {
                        val shouldRecurse = node.recurseIntoChildren ||
                            (directory.canonicalPath == root.canonicalPath && child.name.lowercase() in PRIORITY_DIRECTORY_NAMES)
                        stack.add(ScanNode(child, shouldRecurse))
                        val directoryEntry = metadataFor(child, volumeId, isDirectory = true)
                        if (directoryEntry != null) emit(StorageScanEvent.EntryFound(directoryEntry))
                        else emit(StorageScanEvent.Warning(StorageScanWarning.ENTRY_CHANGED))
                        return@forEach
                    }

                    val isFile = runCatching { child.isFile }.getOrNull()
                    if (isFile == null) {
                        emit(StorageScanEvent.Warning(StorageScanWarning.UNREADABLE_ENTRY))
                        return@forEach
                    }
                    if (!isFile) return@forEach
                    filesDiscovered++
                    val entry = metadataFor(child, volumeId, isDirectory = false)
                    if (entry == null) {
                        emit(StorageScanEvent.Warning(StorageScanWarning.ENTRY_CHANGED))
                        return@forEach
                    }
                    if (entry.sizeBytes < minBytes) {
                        emitProgressIfNeeded(filesDiscovered, foldersVisited, indexedBytes, canonicalDirectory)
                        return@forEach
                    }

                    indexedBytes += entry.sizeBytes
                    emit(StorageScanEvent.EntryFound(entry))
                    emitProgressIfNeeded(filesDiscovered, foldersVisited, indexedBytes, canonicalDirectory)
                }
            }

            if (rootCompletedSuccessfully) successfulVolumes += volumeId
            emit(StorageScanEvent.RootCompleted(volumeId, rootCompletedSuccessfully))
        }

        emit(StorageScanEvent.Progress(filesDiscovered, foldersVisited, indexedBytes))
        emit(StorageScanEvent.Completed(successfulVolumes))
    }.flowOn(Dispatchers.IO)

    private suspend fun FlowCollector<StorageScanEvent>.emitProgressIfNeeded(
        filesDiscovered: Long,
        foldersVisited: Long,
        indexedBytes: Long,
        currentPath: String,
    ) {
        if (filesDiscovered % PROGRESS_INTERVAL == 0L) {
            emit(StorageScanEvent.Progress(filesDiscovered, foldersVisited, indexedBytes, currentPath))
        }
    }

    private fun metadataFor(child: File, volumeId: String, isDirectory: Boolean): StorageEntry? {
        val canonicalPath = runCatching { child.canonicalPath }.getOrNull() ?: return null
        if (!child.exists() || isProtectedPath(canonicalPath)) return null
        val attributes = runCatching {
            Files.readAttributes(child.toPath(), BasicFileAttributes::class.java)
        }.getOrNull()
        val modified = attributes?.lastModifiedTime()?.toMillis() ?: child.lastModified()
        val size = if (isDirectory) 0L else runCatching { child.length() }.getOrDefault(0L)
        return StorageEntry(
            ref = Uri.fromFile(child),
            name = child.name,
            path = canonicalPath,
            category = if (isDirectory) StorageCategory.OTHER else classify(child),
            sizeBytes = size,
            modifiedAtMillis = modified,
            mimeType = if (isDirectory) null else mimeTypeFor(child),
            extension = if (isDirectory) null else extensionFor(child),
            isDirectory = isDirectory,
            volumeId = volumeId,
        )
    }

    private fun roots(): List<File> {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return listOf(Environment.getExternalStorageDirectory())
        }
        val storageManager = appContext.getSystemService(StorageManager::class.java)
        return storageManager.storageVolumes
            .mapNotNull { it.directory }
            .ifEmpty { listOf(Environment.getExternalStorageDirectory()) }
            .distinctBy { it.absolutePath }
    }

    private data class ScanNode(
        val directory: File,
        val recurseIntoChildren: Boolean,
    )

    private fun isProtectedPath(path: String): Boolean {
        val normalized = path.replace(File.separatorChar, '/')
        return normalized.endsWith("/Android/data") ||
            normalized.contains("/Android/data/") ||
            normalized.endsWith("/Android/obb") ||
            normalized.contains("/Android/obb/")
    }

    private fun classify(file: File): StorageCategory {
        val extension = extensionFor(file) ?: return if (file.parentFile?.name.equals("Download", ignoreCase = true)) {
            StorageCategory.DOWNLOAD
        } else {
            StorageCategory.OTHER
        }
        return when {
            extension in VIDEO_EXTENSIONS -> StorageCategory.VIDEO
            extension in IMAGE_EXTENSIONS -> StorageCategory.IMAGE
            extension in AUDIO_EXTENSIONS -> StorageCategory.AUDIO
            extension == "apk" -> StorageCategory.APK
            extension in ARCHIVE_EXTENSIONS -> StorageCategory.ARCHIVE
            extension in DOCUMENT_EXTENSIONS -> StorageCategory.DOCUMENT
            file.path.replace(File.separatorChar, '/').contains("/Download/", ignoreCase = true) -> StorageCategory.DOWNLOAD
            else -> StorageCategory.OTHER
        }
    }

    private fun extensionFor(file: File): String? = file.extension.lowercase().takeIf { it.isNotBlank() }

    private fun mimeTypeFor(file: File): String? = android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extensionFor(file))

    private fun priorityRank(name: String): Int = when (name.lowercase()) {
        "download" -> 0
        "movies" -> 1
        "dcim" -> 2
        "documents" -> 3
        "pictures" -> 4
        "music" -> 5
        else -> 100
    }

    companion object {
        const val PROGRESS_INTERVAL = 128L
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp", "tiff")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr")
        private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso")
        private val DOCUMENT_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "epub",
        )
        private val PRIORITY_DIRECTORY_NAMES = setOf(
            "download", "dcim", "pictures", "movies", "documents", "music",
        )
    }
}
