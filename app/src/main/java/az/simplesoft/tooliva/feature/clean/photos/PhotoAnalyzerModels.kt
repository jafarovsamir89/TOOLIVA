package az.simplesoft.tooliva.feature.clean.photos

import android.net.Uri

enum class PhotoAnalysisKind(val title: String) {
    SIMILAR("Potentially similar"),
    BLURRY("Possibly blurry"),
    OLD_SCREENSHOT("Old screenshots"),
    LARGE_VIDEO("Large videos"),
}

data class PhotoAnalysisItem(
    val uri: Uri,
    val displayName: String,
    val sizeBytes: Long,
    val modifiedAtMillis: Long,
    val mimeType: String?,
    val kind: PhotoAnalysisKind,
    val confidence: Int? = null,
)

data class PhotoAnalysisProgress(
    val checked: Int = 0,
    val candidates: Int = 0,
)

data class PhotoAnalysisResult(
    val items: List<PhotoAnalysisItem> = emptyList(),
    val progress: PhotoAnalysisProgress = PhotoAnalysisProgress(),
)
