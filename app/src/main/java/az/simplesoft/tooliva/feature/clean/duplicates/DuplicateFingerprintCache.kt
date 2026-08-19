package az.simplesoft.tooliva.feature.clean.duplicates

import az.simplesoft.tooliva.core.storage.StorageEntry
import java.io.File
import java.nio.charset.StandardCharsets
import java.util.Base64

data class CachedFingerprint(
    val path: String,
    val sizeBytes: Long,
    val modifiedAtMillis: Long,
    val hash: String,
    val lastUsedAtMillis: Long,
)

/** Small local cache for measured repeat-analysis cost; it stores no file contents. */
class DuplicateFingerprintCache(private val backingFile: File) {
    private val entries = LinkedHashMap<String, CachedFingerprint>()

    fun load() {
        entries.clear()
        if (!backingFile.isFile) return
        runCatching {
            backingFile.forEachLine(StandardCharsets.UTF_8) { line ->
                val parts = line.split('\t')
                if (parts.size != 5) return@forEachLine
                val path = String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8)
                val size = parts[1].toLongOrNull() ?: return@forEachLine
                val modified = parts[2].toLongOrNull() ?: return@forEachLine
                val used = parts[4].toLongOrNull() ?: return@forEachLine
                if (path.isNotBlank() && parts[3].length == 64) {
                    entries[path] = CachedFingerprint(path, size, modified, parts[3], used)
                }
            }
        }.onFailure { entries.clear() }
    }

    fun find(entry: StorageEntry): String? {
        return find(entry.path, entry.sizeBytes, entry.modifiedAtMillis)
    }

    fun find(path: String, sizeBytes: Long, modifiedAtMillis: Long): String? {
        val cached = entries[path] ?: return null
        if (cached.sizeBytes != sizeBytes || cached.modifiedAtMillis != modifiedAtMillis) return null
        entries[path] = cached.copy(lastUsedAtMillis = System.currentTimeMillis())
        return cached.hash
    }

    fun put(entry: StorageEntry, hash: String) {
        put(entry.path, entry.sizeBytes, entry.modifiedAtMillis, hash)
    }

    fun put(path: String, sizeBytes: Long, modifiedAtMillis: Long, hash: String) {
        entries[path] = CachedFingerprint(
            path = path,
            sizeBytes = sizeBytes,
            modifiedAtMillis = modifiedAtMillis,
            hash = hash,
            lastUsedAtMillis = System.currentTimeMillis(),
        )
        trim()
    }

    fun save() {
        backingFile.parentFile?.mkdirs()
        val temporary = File(backingFile.parentFile, "${backingFile.name}.partial")
        runCatching {
            temporary.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                entries.values.forEach { cached ->
                    val encodedPath = Base64.getUrlEncoder().withoutPadding()
                        .encodeToString(cached.path.toByteArray(StandardCharsets.UTF_8))
                    writer.append(encodedPath)
                        .append('\t').append(cached.sizeBytes.toString())
                        .append('\t').append(cached.modifiedAtMillis.toString())
                        .append('\t').append(cached.hash)
                        .append('\t').append(cached.lastUsedAtMillis.toString())
                        .append('\n')
                }
            }
            if (!temporary.renameTo(backingFile)) {
                temporary.delete()
            }
        }.onFailure { temporary.delete() }
    }

    fun size(): Int = entries.size

    private fun trim() {
        if (entries.size <= MAX_ENTRIES) return
        val keep = entries.values.sortedByDescending(CachedFingerprint::lastUsedAtMillis).take(MAX_ENTRIES)
        entries.clear()
        keep.forEach { entries[it.path] = it }
    }

    private companion object {
        const val MAX_ENTRIES = 20_000
    }
}
