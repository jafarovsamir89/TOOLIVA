package az.simplesoft.tooliva.feature.clean.duplicates

import az.simplesoft.tooliva.core.storage.StorageEntry
import kotlinx.coroutines.ensureActive
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import kotlin.coroutines.coroutineContext

sealed interface FileHashResult {
    data class Valid(val hash: String, val bytesRead: Long) : FileHashResult
    data class Invalid(val reason: String) : FileHashResult
}

object FileHasher {
    suspend fun hash(entry: StorageEntry): FileHashResult {
        return hash(File(entry.path), entry.sizeBytes, entry.modifiedAtMillis)
    }

    suspend fun hash(file: File, expectedSize: Long, expectedModifiedAt: Long): FileHashResult {
        val before = snapshot(file) ?: return FileHashResult.Invalid("File is no longer available.")
        if (before.size != expectedSize || (expectedModifiedAt > 0L && before.modifiedAt != expectedModifiedAt)) {
            return FileHashResult.Invalid("File changed before hashing.")
        }
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            var bytesRead = 0L
            FileInputStream(file).use { input ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    coroutineContext.ensureActive()
                    val read = input.read(buffer)
                    if (read < 0) break
                    digest.update(buffer, 0, read)
                    bytesRead += read
                }
            }
            val after = snapshot(file)
            if (after == null || after.size != before.size || after.modifiedAt != before.modifiedAt) {
                FileHashResult.Invalid("File changed during hashing.")
            } else {
                FileHashResult.Valid(digest.digest().toHex(), bytesRead)
            }
        } catch (error: java.io.IOException) {
            FileHashResult.Invalid(error.message ?: "File could not be read.")
        }
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().toHex()
    }

    private data class FileSnapshot(val size: Long, val modifiedAt: Long)

    private fun snapshot(file: File): FileSnapshot? = runCatching {
        if (!file.isFile) null else FileSnapshot(file.length(), file.lastModified())
    }.getOrNull()

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it.toInt() and 0xff) }

    private const val BUFFER_SIZE = 64 * 1024
}
