package az.simplesoft.tooliva.feature.clean.duplicates

import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry

enum class DuplicateAnalysisStage {
    IDLE,
    METADATA,
    HASHING,
    VERIFYING,
    COMPLETED,
    CANCELED,
    ERROR,
}

enum class DuplicateSortOrder {
    MOST_RECOVERABLE,
    LARGEST,
    MOST_COPIES,
    NAME,
}

enum class DuplicateTypeFilter(val label: String) {
    ALL("All"),
    IMAGES("Images"),
    VIDEOS("Videos"),
    AUDIO("Audio"),
    DOCUMENTS("Documents"),
    APK("APK"),
    ARCHIVES("Archives"),
    OTHER("Other"),
}

data class DuplicateMetadata(
    val path: String,
    val sizeBytes: Long,
    val modifiedAtMillis: Long,
    val category: StorageCategory,
    val isDirectory: Boolean,
)

data class DuplicateGroup(
    val sessionId: String,
    val fileSizeBytes: Long,
    val hash: String,
    val entries: List<StorageEntry>,
) {
    val copyCount: Int get() = entries.size
    val potentialRecoverableBytes: Long get() = fileSizeBytes * (copyCount - 1L)
}

data class DuplicateAnalysisProgress(
    val stage: DuplicateAnalysisStage,
    val filesChecked: Long = 0L,
    val candidateFiles: Long = 0L,
    val filesHashed: Long = 0L,
    val bytesHashed: Long = 0L,
    val filesReusedFromCache: Long = 0L,
    val groupsConfirmed: Int = 0,
)

data class DuplicateAnalysisSummary(
    val groups: List<DuplicateGroup>,
    val filesChecked: Long,
    val candidateFiles: Long,
    val filesHashed: Long,
    val bytesHashed: Long,
    val durationMillis: Long,
)

sealed interface DuplicateAnalysisEvent {
    data object Started : DuplicateAnalysisEvent
    data class Progress(val value: DuplicateAnalysisProgress) : DuplicateAnalysisEvent
    data class GroupConfirmed(val group: DuplicateGroup) : DuplicateAnalysisEvent
    data class Completed(val summary: DuplicateAnalysisSummary) : DuplicateAnalysisEvent
}

object DuplicateRules {
    fun candidateGroups(files: Iterable<DuplicateMetadata>): List<List<DuplicateMetadata>> = files
        .asSequence()
        .filter { !it.isDirectory && it.sizeBytes > 0L }
        .groupBy(DuplicateMetadata::sizeBytes)
        .values
        .filter { it.size > 1 }

    fun recoverableBytes(sizeBytes: Long, copyCount: Int): Long =
        if (copyCount > 1) sizeBytes * (copyCount - 1L) else 0L

    fun keepThisCopy(
        groupPaths: List<String>,
        survivorPath: String,
        selectedPaths: Set<String>,
    ): Set<String> {
        require(survivorPath in groupPaths)
        return selectedPaths - survivorPath + groupPaths.filter { it != survivorPath }
    }

    fun canSelect(path: String, groupPaths: List<String>, selectedPaths: Set<String>): Boolean {
        if (path in selectedPaths) return true
        return groupPaths.count { it !in selectedPaths && it != path } >= 1
    }

    fun matchesFilter(entry: StorageEntry, filter: DuplicateTypeFilter): Boolean = when (filter) {
        DuplicateTypeFilter.ALL -> true
        DuplicateTypeFilter.IMAGES -> entry.category == StorageCategory.IMAGE
        DuplicateTypeFilter.VIDEOS -> entry.category == StorageCategory.VIDEO
        DuplicateTypeFilter.AUDIO -> entry.category == StorageCategory.AUDIO
        DuplicateTypeFilter.DOCUMENTS -> entry.category == StorageCategory.DOCUMENT
        DuplicateTypeFilter.APK -> entry.category == StorageCategory.APK
        DuplicateTypeFilter.ARCHIVES -> entry.category == StorageCategory.ARCHIVE
        DuplicateTypeFilter.OTHER -> entry.category == StorageCategory.OTHER || entry.category == StorageCategory.DOWNLOAD
    }
}
