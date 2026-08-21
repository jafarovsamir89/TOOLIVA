package az.simplesoft.tooliva.feature.doctor

import android.os.PowerManager
import az.simplesoft.tooliva.feature.clean.CleanerBucket
import az.simplesoft.tooliva.feature.clean.CleanerAnalysisSnapshot
import az.simplesoft.tooliva.core.device.PhoneDoctorSnapshot
import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CheckupFindingSeverity {
    RECOMMENDED,
    ATTENTION,
    CRITICAL,
}

enum class CheckupFindingId {
    LOW_STORAGE,
    LOW_MEMORY,
    THERMAL,
    LOW_BATTERY,
    BATTERY_HEALTH,
    OLD_SECURITY_PATCH,
    HARDWARE_FAILED,
    HARDWARE_PENDING,
    LARGE_FILES,
    DOWNLOADS,
    SCREENSHOTS,
    STORAGE_SCAN,
}

data class CheckupFinding(
    val id: CheckupFindingId,
    val severity: CheckupFindingSeverity,
    val actionId: String,
    val count: Int = 0,
    val bytes: Long = 0L,
    val availableBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val ageDays: Long = 0L,
)

fun evaluateCheckup(
    snapshot: PhoneDoctorSnapshot,
    hardware: CheckupHardwareSummary,
    cleanerSnapshot: CleanerAnalysisSnapshot?,
    nowMillis: Long,
): List<CheckupFinding> = buildList {
    val primaryStorage = snapshot.storage.firstOrNull { !it.removable } ?: snapshot.storage.firstOrNull()
    if (primaryStorage != null && primaryStorage.totalBytes > 0L) {
        val freeFraction = primaryStorage.availableBytes.toDouble() / primaryStorage.totalBytes.toDouble()
        if (freeFraction <= 0.20) {
            add(
                CheckupFinding(
                    id = CheckupFindingId.LOW_STORAGE,
                    severity = if (freeFraction <= 0.10) CheckupFindingSeverity.CRITICAL else CheckupFindingSeverity.ATTENTION,
                    actionId = "large-files",
                    availableBytes = primaryStorage.availableBytes,
                    totalBytes = primaryStorage.totalBytes,
                ),
            )
        }
    }
    if (snapshot.memory?.lowMemory == true) add(CheckupFinding(CheckupFindingId.LOW_MEMORY, CheckupFindingSeverity.ATTENTION, "optimizer"))
    snapshot.thermal.status?.let { status ->
        if (status != PowerManager.THERMAL_STATUS_NONE) {
            add(CheckupFinding(CheckupFindingId.THERMAL, if (status >= PowerManager.THERMAL_STATUS_SEVERE) CheckupFindingSeverity.CRITICAL else CheckupFindingSeverity.ATTENTION, "doctor"))
        }
    }
    snapshot.battery.levelPercent?.let { level ->
        if (level <= 20) add(CheckupFinding(CheckupFindingId.LOW_BATTERY, if (level <= 10) CheckupFindingSeverity.CRITICAL else CheckupFindingSeverity.ATTENTION, "doctor", count = level))
    }
    if (snapshot.battery.health in setOf("Overheat", "Dead", "Over voltage", "Unspecified failure")) {
        add(CheckupFinding(CheckupFindingId.BATTERY_HEALTH, CheckupFindingSeverity.CRITICAL, "doctor"))
    }
    parsePatchAge(snapshot.system.securityPatch, nowMillis)?.takeIf { it >= 365L }?.let { age ->
        add(CheckupFinding(CheckupFindingId.OLD_SECURITY_PATCH, CheckupFindingSeverity.ATTENTION, "doctor", ageDays = age))
    }
    if (hardware.failed > 0) add(CheckupFinding(CheckupFindingId.HARDWARE_FAILED, CheckupFindingSeverity.CRITICAL, "hardware", count = hardware.failed))
    val pendingHardware = (hardware.supported - hardware.completed).coerceAtLeast(0)
    if (pendingHardware > 0) add(CheckupFinding(CheckupFindingId.HARDWARE_PENDING, CheckupFindingSeverity.RECOMMENDED, "hardware", count = pendingHardware))

    if (cleanerSnapshot == null) {
        add(CheckupFinding(CheckupFindingId.STORAGE_SCAN, CheckupFindingSeverity.RECOMMENDED, "cleaner"))
    } else {
        cleanerSnapshot.summaries.firstOrNull { it.bucket == CleanerBucket.LARGE_FILES }?.let { summary ->
            add(CheckupFinding(CheckupFindingId.LARGE_FILES, CheckupFindingSeverity.RECOMMENDED, "large-files", summary.count, summary.bytes))
        }
        cleanerSnapshot.summaries.firstOrNull { it.bucket == CleanerBucket.DOWNLOADS }?.let { summary ->
            add(CheckupFinding(CheckupFindingId.DOWNLOADS, CheckupFindingSeverity.RECOMMENDED, "downloads", summary.count, summary.bytes))
        }
        cleanerSnapshot.summaries.firstOrNull { it.bucket == CleanerBucket.SCREENSHOTS }?.let { summary ->
            add(CheckupFinding(CheckupFindingId.SCREENSHOTS, CheckupFindingSeverity.RECOMMENDED, "screenshots", summary.count, summary.bytes))
        }
    }
}

fun checkupOverallSeverity(findings: List<CheckupFinding>): CheckupFindingSeverity? = findings.maxByOrNull { it.severity.ordinal }?.severity

private fun parsePatchAge(value: String, nowMillis: Long): Long? = runCatching {
    val patchDate = LocalDate.parse(value.take(10))
    ChronoUnit.DAYS.between(patchDate, java.time.Instant.ofEpochMilli(nowMillis).atZone(java.time.ZoneId.systemDefault()).toLocalDate())
}.getOrNull()?.takeIf { it >= 0L }
