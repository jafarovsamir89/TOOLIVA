package az.simplesoft.tooliva.core.cleanup

import az.simplesoft.tooliva.core.storage.StorageCategory
import az.simplesoft.tooliva.core.storage.StorageEntry

enum class CleanupReasonId {
    OLD_APK_INSTALLER,
    OLD_DOWNLOAD,
}

data class CleanupReason(
    val id: CleanupReasonId,
    val title: String,
    val explanation: String,
)

data class CleanupCandidate(
    val entry: StorageEntry,
    val reason: CleanupReason,
    val defaultSelected: Boolean = false,
)

object CleanupRecommendationRules {
    const val DAY_MILLIS = 24L * 60L * 60L * 1000L

    fun candidateFor(entry: StorageEntry, thresholdDays: Int, nowMillis: Long): CleanupCandidate? {
        if (!isKnownOld(entry.modifiedAtMillis, thresholdDays, nowMillis)) return null

        return if (entry.category == StorageCategory.APK && isDownloadPath(entry.path)) {
            CleanupCandidate(
                entry = entry,
                reason = CleanupReason(
                    id = CleanupReasonId.OLD_APK_INSTALLER,
                    title = "Old APK installer",
                    explanation = "${ageInDays(entry.modifiedAtMillis, nowMillis)} days old. Removing it does not uninstall the app, but you may need the installer later.",
                ),
            )
        } else if (entry.category != StorageCategory.APK && isDownloadPath(entry.path) && !entry.isDirectory) {
            CleanupCandidate(
                entry = entry,
                reason = CleanupReason(
                    id = CleanupReasonId.OLD_DOWNLOAD,
                    title = "Old Download",
                    explanation = "This file has been in Downloads for ${ageInDays(entry.modifiedAtMillis, nowMillis)} days. Review whether you still need it.",
                ),
            )
        } else {
            null
        }
    }

    fun isKnownOld(modifiedAtMillis: Long, thresholdDays: Int, nowMillis: Long): Boolean =
        modifiedAtMillis > 0L && nowMillis - modifiedAtMillis >= thresholdDays * DAY_MILLIS

    fun ageInDays(modifiedAtMillis: Long, nowMillis: Long): Long =
        ((nowMillis - modifiedAtMillis).coerceAtLeast(0L) / DAY_MILLIS)

    fun isDownloadPath(path: String): Boolean = path
        .replace('\\', '/')
        .split('/')
        .any { it.equals("Download", ignoreCase = true) || it.equals("Downloads", ignoreCase = true) }
}
