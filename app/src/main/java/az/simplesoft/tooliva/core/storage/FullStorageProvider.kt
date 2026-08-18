package az.simplesoft.tooliva.core.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.coroutineContext

class FullStorageProvider(context: Context) : StorageProvider {
    private val appContext = context.applicationContext

    override val accessMode: StorageAccessMode = StorageAccessMode.FULL

    override fun scan(minBytes: Long): Flow<StorageScanEvent> = flow {
        emit(StorageScanEvent.Started)
        var visited = 0L
        var matched = 0L
        var matchedBytes = 0L

        roots().forEach { root ->
            if (!root.exists() || !root.isDirectory) return@forEach
            val seenDirectories = HashSet<String>()
            val stack = ArrayDeque<File>()
            stack.add(root)

            while (stack.isNotEmpty()) {
                coroutineContext.ensureActive()
                val directory = stack.removeLast()
                val canonicalDirectory = runCatching { directory.canonicalPath }.getOrNull()
                    ?: continue
                if (!seenDirectories.add(canonicalDirectory) || isProtectedPath(canonicalDirectory)) continue

                val children = runCatching { directory.listFiles() }.getOrNull()
                if (children == null) {
                    emit(StorageScanEvent.Warning(canonicalDirectory))
                    continue
                }

                children.forEach { child ->
                    coroutineContext.ensureActive()
                    val path = child.absolutePath
                    if (isProtectedPath(path)) return@forEach
                    if (child.isDirectory) {
                        stack.add(child)
                        return@forEach
                    }
                    if (!child.isFile || Files.isSymbolicLink(child.toPath())) return@forEach

                    visited++
                    val size = runCatching { child.length() }.getOrDefault(0L)
                    if (size < minBytes) {
                        if (visited % PROGRESS_INTERVAL == 0L) {
                            emit(StorageScanEvent.Progress(visited, matched, matchedBytes))
                        }
                        return@forEach
                    }

                    val attributes = runCatching {
                        Files.readAttributes(child.toPath(), BasicFileAttributes::class.java)
                    }.getOrNull()
                    val category = classify(child)
                    val entry = StorageEntry(
                        ref = Uri.fromFile(child),
                        name = child.name,
                        path = path,
                        category = category,
                        sizeBytes = size,
                        modifiedAtMillis = attributes?.lastModifiedTime()?.toMillis() ?: child.lastModified(),
                        mimeType = mimeTypeFor(child),
                        extension = extensionFor(child),
                        volumeId = root.absolutePath,
                    )
                    matched++
                    matchedBytes += size
                    emit(StorageScanEvent.EntryFound(entry))
                    if (matched % PROGRESS_INTERVAL == 0L) {
                        emit(StorageScanEvent.Progress(visited, matched, matchedBytes))
                    }
                }
            }
        }
        emit(StorageScanEvent.Progress(visited, matched, matchedBytes))
        emit(StorageScanEvent.Completed)
    }.flowOn(Dispatchers.IO)

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

    companion object {
        private const val PROGRESS_INTERVAL = 128L
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp", "tiff")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr")
        private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz", "iso")
        private val DOCUMENT_EXTENSIONS = setOf(
            "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "epub",
        )
    }
}
