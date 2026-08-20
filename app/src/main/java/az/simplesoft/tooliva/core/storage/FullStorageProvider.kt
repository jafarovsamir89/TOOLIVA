package az.simplesoft.tooliva.core.storage

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributes
import kotlin.coroutines.coroutineContext

class FullStorageProvider(context: Context) : StorageProvider {
    private val appContext = context.applicationContext

    override val accessMode: StorageAccessMode = StorageAccessMode.FULL

    fun storageRoots(): List<File> = volumeRoots()

    fun volumeInfos(): List<StorageVolumeInfo> = volumeRoots().mapIndexed { index, root ->
        val stat = runCatching { android.os.StatFs(root.absolutePath) }.getOrNull()
        StorageVolumeInfo(
            id = root.absolutePath,
            name = if (index == 0) "Internal storage" else root.name.ifBlank { "External storage ${index + 1}" },
            root = root,
            totalBytes = stat?.totalBytes ?: 0L,
            availableBytes = stat?.availableBytes ?: 0L,
            isPrimary = index == 0,
        )
    }

    fun children(directory: File): List<StorageEntry> {
        require(isAllowedPath(directory)) { "This folder is restricted by Android." }
        if (!directory.exists() || !directory.isDirectory) return emptyList()
        return directory.listFiles().orEmpty().mapNotNull { child ->
            if (isProtectedPath(child.absolutePath) || Files.isSymbolicLink(child.toPath())) return@mapNotNull null
            entryFor(child, directory.absolutePath)
        }
    }

    fun search(directory: File, predicate: (StorageEntry) -> Boolean): Flow<StorageScanEvent> = flow {
        val stack = ArrayDeque<File>()
        val seen = HashSet<String>()
        stack.add(directory)
        var visited = 0L
        var matched = 0L
        var matchedBytes = 0L
        while (stack.isNotEmpty()) {
            kotlinx.coroutines.currentCoroutineContext().ensureActive()
            val current = stack.removeLast()
            val canonical = runCatching { current.canonicalPath }.getOrNull() ?: continue
            if (!seen.add(canonical) || !isAllowedPath(current)) continue
            current.listFiles().orEmpty().forEach { child ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                if (isProtectedPath(child.absolutePath) || Files.isSymbolicLink(child.toPath())) return@forEach
                val entry = entryFor(child, directory.absolutePath) ?: return@forEach
                if (child.isDirectory) stack.add(child)
                visited++
                if (predicate(entry)) {
                    matched++
                    matchedBytes += entry.sizeBytes
                    emit(StorageScanEvent.EntryFound(entry))
                }
                if (visited % PROGRESS_INTERVAL == 0L) emit(StorageScanEvent.Progress(visited, matched, matchedBytes))
            }
        }
        emit(StorageScanEvent.Progress(visited, matched, matchedBytes))
        emit(StorageScanEvent.Completed)
    }.flowOn(Dispatchers.IO)

    fun isAllowedPath(file: File): Boolean {
        val canonical = runCatching { file.canonicalFile }.getOrNull() ?: return false
        return storageRoots().any { root ->
            val rootCanonical = runCatching { root.canonicalFile }.getOrNull() ?: return@any false
            (canonical == rootCanonical || canonical.toPath().startsWith(rootCanonical.toPath())) && !isProtectedPath(canonical.path)
        }
    }

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

                    val entry = entryFor(child, root.absolutePath) ?: return@forEach
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

    /** Lightweight traversal for folder totals: no MIME lookup, classifier or file attributes. */
    internal fun scanStorageMap(): Flow<StorageMapScanEvent> = flow {
        emit(StorageMapScanEvent.Started)
        var filesChecked = 0L
        volumeRoots().forEach { root ->
            if (!root.exists() || !root.isDirectory) return@forEach
            val seenDirectories = HashSet<String>()
            val stack = ArrayDeque<File>()
            stack.add(root)
            while (stack.isNotEmpty()) {
                coroutineContext.ensureActive()
                val directory = stack.removeLast()
                val canonicalDirectory = runCatching { directory.canonicalPath }.getOrNull() ?: continue
                if (!seenDirectories.add(canonicalDirectory) || isProtectedPath(canonicalDirectory)) continue
                val children = runCatching { directory.listFiles() }.getOrNull()
                if (children == null) {
                    emit(StorageMapScanEvent.Warning(canonicalDirectory))
                    continue
                }
                children.forEach { child ->
                    coroutineContext.ensureActive()
                    if (isProtectedPath(child.absolutePath) || Files.isSymbolicLink(child.toPath())) return@forEach
                    if (child.isDirectory) {
                        stack.add(child)
                    } else if (child.isFile) {
                        filesChecked++
                        emit(StorageMapScanEvent.FileFound(root.absolutePath, child.absolutePath, child.length().coerceAtLeast(0L)))
                        if (filesChecked % PROGRESS_INTERVAL == 0L) emit(StorageMapScanEvent.Progress(filesChecked))
                    }
                }
            }
        }
        emit(StorageMapScanEvent.Progress(filesChecked))
        emit(StorageMapScanEvent.Completed)
    }.flowOn(Dispatchers.IO)

    private fun roots(scope: StorageScanScope): List<File> {
        val volumes = storageRoots()
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

    private fun entryFor(child: File, volumeId: String): StorageEntry? {
        val attributes = runCatching { Files.readAttributes(child.toPath(), BasicFileAttributes::class.java) }.getOrNull()
        val mime = if (child.isFile) mimeTypeFor(child) else null
        return StorageEntry(
            ref = Uri.fromFile(child),
            name = child.name,
            path = child.absolutePath,
            category = if (child.isDirectory) StorageCategory.OTHER else StorageFileClassifier.classify(child.name, mime, child.absolutePath),
            sizeBytes = if (child.isFile) runCatching { child.length() }.getOrDefault(0L) else 0L,
            modifiedAtMillis = attributes?.lastModifiedTime()?.toMillis() ?: child.lastModified(),
            mimeType = mime,
            extension = child.extension.lowercase().takeIf { it.isNotBlank() },
            isDirectory = child.isDirectory,
            volumeId = volumeId,
        )
    }

    private fun extensionFor(file: File): String? = file.extension.lowercase().takeIf { it.isNotBlank() }

    private fun mimeTypeFor(file: File): String? = android.webkit.MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(extensionFor(file))

    companion object {
        private const val PROGRESS_INTERVAL = 128L
    }
}
