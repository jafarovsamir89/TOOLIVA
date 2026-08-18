package az.simplesoft.tooliva.feature.clean.index

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.DateFormat
import java.util.Date

@Composable
fun StorageIndexCard(
    state: StorageIndexUiState,
    onScan: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (state.status == az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.SCANNING) {
                    CircularProgressIndicator(strokeWidth = 3.dp)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Cleaner scan", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(indexStatusLabel(state.status, state.phase), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                "${state.filesDiscovered} files checked · ${state.foldersVisited} folders checked",
                style = MaterialTheme.typography.bodyMedium,
            )
            val foundBytes = state.categorySummaries.sumOf { it.totalBytes }
            if (foundBytes > 0L) {
                Text(
                    "Found ${Formatter.formatFileSize(context, foundBytes)} for review",
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (state.categorySummaries.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.categorySummaries.take(4).chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            row.forEach { summary ->
                                Card(
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(categoryLabel(summary.category), style = MaterialTheme.typography.labelLarge)
                                        Text(
                                            Formatter.formatFileSize(context, summary.totalBytes),
                                            fontWeight = FontWeight.Bold,
                                        )
                                        Text("${summary.fileCount} files", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                            if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
            if (state.phase == az.simplesoft.tooliva.core.storage.index.StorageIndexScanPhase.FAST) {
                Text(
                    "Finding cleanup opportunities…",
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (state.phase == az.simplesoft.tooliva.core.storage.index.StorageIndexScanPhase.DEEP) {
                Text(
                    "Deep scan still running in the background. You can keep using Tooliva.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            state.fastScanElapsedMillis?.takeIf { it > 0L }?.let { elapsed ->
                Text(
                    "Fast results ready in ${elapsed / 1000.0}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state.warningCount > 0) {
                Text(
                    "${state.warningCount} entries changed or could not be read; the scan continued.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            state.lastScanAtMillis?.let {
                Text(
                    "Last scan: ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(it))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.status == az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.SCANNING) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancel scan")
                }
            } else {
                Button(onClick = onScan, modifier = Modifier.fillMaxWidth()) {
                    Text(if (state.lastScanAtMillis == null) "Scan Storage" else "Refresh in background")
                }
            }
        }
    }
}

private fun indexStatusLabel(
    status: az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus,
    phase: az.simplesoft.tooliva.core.storage.index.StorageIndexScanPhase,
): String = when {
    phase == az.simplesoft.tooliva.core.storage.index.StorageIndexScanPhase.FAST -> "Finding cleanup opportunities…"
    phase == az.simplesoft.tooliva.core.storage.index.StorageIndexScanPhase.DEEP -> "Deep scan running in background"
    status == az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.IDLE -> "Not indexed yet"
    status == az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.SCANNING -> "Scanning accessible storage…"
    status == az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.COMPLETED -> "Index ready"
    status == az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.CANCELED -> "Scan canceled; previous index preserved"
    status == az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.FAILED -> "Scan failed; previous index preserved"
    else -> "Storage results unavailable"
}

private fun categoryLabel(category: az.simplesoft.tooliva.core.storage.StorageCategory): String = when (category) {
    az.simplesoft.tooliva.core.storage.StorageCategory.APK -> "APK installers"
    az.simplesoft.tooliva.core.storage.StorageCategory.ARCHIVE -> "Archives"
    az.simplesoft.tooliva.core.storage.StorageCategory.DOWNLOAD -> "Downloads"
    az.simplesoft.tooliva.core.storage.StorageCategory.VIDEO -> "Videos"
    az.simplesoft.tooliva.core.storage.StorageCategory.IMAGE -> "Images"
    az.simplesoft.tooliva.core.storage.StorageCategory.AUDIO -> "Audio"
    az.simplesoft.tooliva.core.storage.StorageCategory.DOCUMENT -> "Documents"
    az.simplesoft.tooliva.core.storage.StorageCategory.OTHER -> "Other"
    az.simplesoft.tooliva.core.storage.StorageCategory.ALL -> "All files"
}
