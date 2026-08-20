package az.simplesoft.tooliva.feature.clean.swipe

import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageAccessState
import az.simplesoft.tooliva.core.storage.StorageEntry

enum class CleanupSwipeCategory(val title: String, val subtitle: String) {
    SCREENSHOTS("Screenshots", "Review screenshots with real thumbnails"),
    IMAGES("Images", "Review shared-storage images"),
    VIDEOS("Videos", "Review shared-storage videos"),
    DOWNLOADS("Downloads", "Review files in Downloads"),
    LARGE_FILES("Large files", "Review files at least 100 MB"),
}

enum class CleanupSwipeSort(val title: String) {
    NEWEST("Newest first"),
    OLDEST("Oldest first"),
    LARGEST("Largest first"),
    SMALLEST("Smallest first"),
}

enum class SwipeDecision { KEEP, DELETE, SKIP }

data class CleanupSwipeSession(
    val category: CleanupSwipeCategory,
    val entries: List<StorageEntry>,
    val currentIndex: Int = 0,
    val decisions: Map<String, SwipeDecision> = emptyMap(),
    val decisionHistory: List<String> = emptyList(),
) {
    val current: StorageEntry? get() = entries.getOrNull(currentIndex)
    val isComplete: Boolean get() = currentIndex >= entries.size
    val selectedDeleteEntries: List<StorageEntry> get() = entries.filter { decisions[it.path] == SwipeDecision.DELETE }
    val selectedDeleteBytes: Long get() = selectedDeleteEntries.sumOf(StorageEntry::sizeBytes)
    val keptCount: Int get() = decisions.values.count { it == SwipeDecision.KEEP }
    val skippedCount: Int get() = decisions.values.count { it == SwipeDecision.SKIP }
    val decidedCount: Int get() = decisions.size
}

data class CleanupSwipeSnapshot(
    val phase: CleanupSwipePhase = CleanupSwipePhase.PICKER,
    val selectedCategory: CleanupSwipeCategory? = null,
    val isLoading: Boolean = false,
    val filesChecked: Long = 0L,
    val entries: List<StorageEntry> = emptyList(),
    val session: CleanupSwipeSession? = null,
    val sort: CleanupSwipeSort = CleanupSwipeSort.LARGEST,
    val errorMessage: String? = null,
    val cleanupResult: az.simplesoft.tooliva.core.media.CleanupResult? = null,
    val showFinalConfirmation: Boolean = false,
    val isDeleting: Boolean = false,
    val detailsEntry: StorageEntry? = null,
    val accessState: StorageAccessState = StorageAccessState(false, false),
)

enum class CleanupSwipePhase { PICKER, LOADING, REVIEW, FINAL_REVIEW }

fun CleanupSwipeSession.applyDecision(decision: SwipeDecision): CleanupSwipeSession {
    val item = current ?: return this
    return copy(
        currentIndex = (currentIndex + 1).coerceAtMost(entries.size),
        decisions = decisions + (item.path to decision),
        decisionHistory = decisionHistory + item.path,
    )
}

fun CleanupSwipeSession.undoLast(): CleanupSwipeSession {
    val path = decisionHistory.lastOrNull() ?: return this
    return copy(
        currentIndex = (currentIndex - 1).coerceAtLeast(0),
        decisions = decisions - path,
        decisionHistory = decisionHistory.dropLast(1),
    )
}

fun CleanupSwipeCategory.matches(entry: StorageEntry): Boolean = when (this) {
    CleanupSwipeCategory.SCREENSHOTS -> entry.category == StorageCategory.IMAGE &&
        (entry.name.contains("screenshot", true) || entry.path.contains("screenshot", true))
    CleanupSwipeCategory.IMAGES -> entry.category == StorageCategory.IMAGE
    CleanupSwipeCategory.VIDEOS -> entry.category == StorageCategory.VIDEO
    CleanupSwipeCategory.DOWNLOADS -> true
    CleanupSwipeCategory.LARGE_FILES -> entry.sizeBytes >= 100L * 1024L * 1024L
}
