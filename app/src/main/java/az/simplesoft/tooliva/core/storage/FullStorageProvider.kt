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

    override fun scan(minBytes: Long, scope: StorageScanScope): Flow<StorageScanEvent> = flow {
        emit(StorageScanEvent.Started)
        var visited = 0L
        var matched = 0L
        var matchedBytes = 0L

        roots(scope).forEach { root ->
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
                    val category = StorageFileClassifier.classify(
                        name = child.name,
                        mimeType = mimeTypeFor(child),
                        path = path,
                    )
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

    private fun roots(scope: StorageScanScope): List<File> {
        val volumes = volumeRoots()
        if (scope == StorageScanScope.ALL_STORAGE) return volumes

        return volumes
            .flatMap { volume ->
                runCatching { volume.listFiles().orEmpty().toList() }
                    .getOrDefault(emptyList())
                    .filter { it.isDirectory && it.name.equals("Download", ignoreCase = true) }
            }
            .distinctBy { it.absolutePath }
    }

    private fun volumeRoots(): List<File> {
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

    private fun extensionFor(file: File): String? = file.extension.lowercase().takeIf { it.isNotBlank() }

    private fun mimeTypeFor(file: File): String? = android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extensionFor(file))

    companion object {
        private const val PROGRESS_INTERVAL = 128L
    }
}
