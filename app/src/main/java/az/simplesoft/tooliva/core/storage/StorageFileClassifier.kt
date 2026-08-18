package az.simplesoft.tooliva.core.storage

import android.webkit.MimeTypeMap
import java.util.Locale

/** Lightweight extension/MIME rules shared by direct storage scans. */
object StorageFileClassifier {
    fun classify(name: String, mimeType: String? = null, path: String? = null): StorageCategory {
        val extension = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val mime = mimeType.orEmpty().lowercase(Locale.ROOT)
        return when {
            extension == "apk" || mime == "application/vnd.android.package-archive" -> StorageCategory.APK
            extension in ARCHIVE_EXTENSIONS || mime.startsWith("application/zip") -> StorageCategory.ARCHIVE
            extension in DOCUMENT_EXTENSIONS || mime.startsWith("text/") || mime in DOCUMENT_MIME_TYPES -> StorageCategory.DOCUMENT
            extension in IMAGE_EXTENSIONS || mime.startsWith("image/") -> StorageCategory.IMAGE
            extension in VIDEO_EXTENSIONS || mime.startsWith("video/") -> StorageCategory.VIDEO
            extension in AUDIO_EXTENSIONS || mime.startsWith("audio/") -> StorageCategory.AUDIO
            path.orEmpty().replace('\\', '/').contains("/Download/", ignoreCase = true) -> StorageCategory.DOWNLOAD
            else -> StorageCategory.OTHER
        }
    }

    fun mimeTypeForName(name: String): String? = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(name.substringAfterLast('.', "").lowercase(Locale.ROOT).takeIf { it.isNotBlank() })

    private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "mov", "avi", "webm", "3gp", "m4v")
    private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "heic", "gif", "bmp", "tiff")
    private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "aac", "wav", "flac", "ogg", "opus", "amr")
    private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "tgz", "bz2", "xz", "iso")
    private val DOCUMENT_EXTENSIONS = setOf(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "rtf", "csv", "epub",
    )
    private val DOCUMENT_MIME_TYPES = setOf(
        "application/pdf",
        "application/msword",
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
    )
}
