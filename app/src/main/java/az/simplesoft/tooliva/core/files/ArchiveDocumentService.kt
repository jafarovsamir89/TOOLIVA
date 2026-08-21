package az.simplesoft.tooliva.core.files

import az.simplesoft.tooliva.core.storage.StorageEntry
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

sealed interface FilePreview {
    data class Text(val title: String, val content: String, val truncated: Boolean) : FilePreview
    data class Archive(val title: String, val entries: List<String>, val format: String) : FilePreview
    data class External(val title: String, val format: String) : FilePreview
    data class Unsupported(val title: String, val reason: String) : FilePreview
}

class ArchiveDocumentService {
    fun preview(file: File): FilePreview {
        val extension = file.extension.lowercase()
        return when {
            extension == "zip" -> FilePreview.Archive(file.name, previewZip(file), "ZIP")
            extension in setOf("7z", "rar", "tar", "gz", "bz2") -> FilePreview.External(file.name, extension.uppercase())
            extension in TEXT_EXTENSIONS -> {
                val bytes = file.inputStream().use { input ->
                    val output = ByteArrayOutputStream(MAX_TEXT_BYTES + 1)
                    val buffer = ByteArray(8 * 1024)
                    var remaining = MAX_TEXT_BYTES + 1
                    while (remaining > 0) {
                        val read = input.read(buffer, 0, minOf(buffer.size, remaining))
                        if (read <= 0) break
                        output.write(buffer, 0, read)
                        remaining -= read
                    }
                    output.toByteArray()
                }
                FilePreview.Text(file.name, bytes.toString(Charsets.UTF_8).take(MAX_TEXT_BYTES), bytes.size > MAX_TEXT_BYTES)
            }
            extension == "pdf" -> FilePreview.External(file.name, "PDF")
            else -> FilePreview.Unsupported(file.name, "This file type does not have a safe in-app preview.")
        }
    }

    fun extractZip(file: File, destination: File): Int {
        require(file.extension.equals("zip", true)) { "Only ZIP archives can be extracted by Tooliva." }
        var extracted = 0
        ZipFile(file).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val target = File(destination, entry.name).canonicalFile
                require(target.path.startsWith(destination.canonicalPath + File.separator)) { "Archive contains an unsafe path." }
                if (entry.isDirectory) target.mkdirs()
                else {
                    target.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { input -> target.outputStream().use(input::copyTo) }
                    extracted++
                }
            }
        }
        return extracted
    }

    fun createZip(entries: List<File>, destinationDirectory: File): File {
        require(destinationDirectory.isDirectory) { "Destination folder is not available." }
        val name = "Tooliva_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))}.zip"
        val destination = File(destinationDirectory, name)
        ZipOutputStream(FileOutputStream(destination)).use { output ->
            entries.filter { it.exists() && it.isFile }.forEach { file ->
                output.putNextEntry(ZipEntry(file.name))
                FileInputStream(file).use { it.copyTo(output) }
                output.closeEntry()
            }
        }
        return destination
    }

    private fun previewZip(file: File): List<String> = ZipFile(file).use { zip -> zip.entries().asSequence().take(MAX_ARCHIVE_ENTRIES).map { entry -> if (entry.isDirectory) "${entry.name}/" else entry.name }.toList() }

    private companion object {
        const val MAX_TEXT_BYTES = 64 * 1024
        const val MAX_ARCHIVE_ENTRIES = 200
        val TEXT_EXTENSIONS = setOf("txt", "md", "csv", "json", "xml", "log", "html", "htm")
    }
}
