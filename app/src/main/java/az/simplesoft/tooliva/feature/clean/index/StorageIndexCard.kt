package az.simplesoft.tooliva.feature.clean.index

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
                    Text("Storage index", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(indexStatusLabel(state.status), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Text(
                "${state.filesDiscovered} files discovered · ${state.foldersVisited} folders visited",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "${Formatter.formatFileSize(context, state.indexedBytes)} indexed",
                color = MaterialTheme.colorScheme.primary,
            )
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
                    Text("Scan Storage")
                }
            }
        }
    }
}

private fun indexStatusLabel(status: az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus): String = when (status) {
    az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.IDLE -> "Not indexed yet"
    az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.SCANNING -> "Scanning accessible storage…"
    az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.COMPLETED -> "Index ready"
    az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.CANCELED -> "Scan canceled; previous index preserved"
    az.simplesoft.tooliva.core.storage.index.StorageIndexRunStatus.FAILED -> "Scan failed; previous index preserved"
}
