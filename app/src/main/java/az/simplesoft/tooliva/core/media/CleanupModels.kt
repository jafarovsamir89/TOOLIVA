package az.simplesoft.tooliva.core.media

import android.content.IntentSender
import android.net.Uri

enum class CleanupResultStatus {
    COMPLETED,
    PARTIAL,
    CANCELED,
    PERMISSION_REVOKED,
    NO_CHANGE,
}

data class CleanupFile(
    val uri: Uri,
    val sizeBytes: Long,
)

data class PreparedCleanupDeletion(
    val requested: List<CleanupFile>,
    val eligible: List<CleanupFile>,
    val missingBeforeAction: List<CleanupFile>,
) {
    val requestedBytes: Long = requested.sumOf { it.sizeBytes }
    val eligibleBytes: Long = eligible.sumOf { it.sizeBytes }
    val missingBeforeBytes: Long = missingBeforeAction.sumOf { it.sizeBytes }
}

data class PendingMediaDelete(
    val requestId: Long,
    val prepared: PreparedCleanupDeletion,
    val intentSender: IntentSender,
)

data class CleanupResult(
    val status: CleanupResultStatus,
    val requestedCount: Int,
    val requestedBytes: Long,
    val removedFromActiveCount: Int,
    val removedFromActiveBytes: Long,
    val trashedCount: Int,
    val trashedBytes: Long,
    val freedCount: Int,
    val freedBytes: Long,
    val missingBeforeCount: Int,
    val missingBeforeBytes: Long,
    val failedCount: Int,
    val failedBytes: Long,
    val unchangedCount: Int,
    val unchangedBytes: Long,
    val note: String? = null,
) {
    companion object {
        fun canceled(prepared: PreparedCleanupDeletion): CleanupResult = CleanupResult(
            status = CleanupResultStatus.CANCELED,
            requestedCount = prepared.requested.size,
            requestedBytes = prepared.requestedBytes,
            removedFromActiveCount = 0,
            removedFromActiveBytes = 0,
            trashedCount = 0,
            trashedBytes = 0,
            freedCount = 0,
            freedBytes = 0,
            missingBeforeCount = prepared.missingBeforeAction.size,
            missingBeforeBytes = prepared.missingBeforeBytes,
            failedCount = 0,
            failedBytes = 0,
            unchangedCount = prepared.eligible.size,
            unchangedBytes = prepared.eligibleBytes,
            note = "No file was changed because the system confirmation was canceled.",
        )

        fun permissionRevoked(prepared: PreparedCleanupDeletion): CleanupResult = CleanupResult(
            status = CleanupResultStatus.PERMISSION_REVOKED,
            requestedCount = prepared.requested.size,
            requestedBytes = prepared.requestedBytes,
            removedFromActiveCount = 0,
            removedFromActiveBytes = 0,
            trashedCount = 0,
            trashedBytes = 0,
            freedCount = 0,
            freedBytes = 0,
            missingBeforeCount = prepared.missingBeforeAction.size,
            missingBeforeBytes = prepared.missingBeforeBytes,
            failedCount = prepared.eligible.size,
            failedBytes = prepared.eligibleBytes,
            unchangedCount = 0,
            unchangedBytes = 0,
            note = "Media access changed before the cleanup result could be verified.",
        )
    }
}
