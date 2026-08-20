package az.simplesoft.tooliva.feature.clean.result

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.simplesoft.tooliva.core.media.CleanupResult
import az.simplesoft.tooliva.core.media.CleanupResultStatus

@Composable
fun CleanupResultScreen(
    result: CleanupResult,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val status = result.statusPresentation()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                Text("Cleanup result", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                Text(
                    status.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = status.color,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(status.icon, contentDescription = null, tint = status.color)
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                "${result.removedFromActiveCount} ${result.itemLabel}",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                            )
                            Text("no longer in the active media list")
                        }
                    }
                    Text(
                        "${Formatter.formatFileSize(context, result.removedFromActiveBytes)} reviewed from the selected items",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ResultRow(
                        icon = Icons.Outlined.DeleteSweep,
                        label = "Moved to Trash",
                        value = "${result.trashedCount} · ${Formatter.formatFileSize(context, result.trashedBytes)}",
                    )
                    ResultRow(
                        icon = Icons.Outlined.CheckCircle,
                        label = if (result.trashedCount > 0) "Physically freed" else "Space freed",
                        value = "${result.freedCount} · ${Formatter.formatFileSize(context, result.freedBytes)}",
                    )
                    if (result.trashedCount > 0) {
                        Text(
                            "Items moved to Trash still use storage until Trash is emptied.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 44.dp, top = 2.dp),
                        )
                    }
                    if (result.missingBeforeCount > 0) {
                        ResultRow(
                            icon = Icons.Outlined.Info,
                            label = "Already gone before action",
                            value = "${result.missingBeforeCount} · ${Formatter.formatFileSize(context, result.missingBeforeBytes)}",
                        )
                    }
                    if (result.failedCount > 0) {
                        ResultRow(
                            icon = Icons.Outlined.ErrorOutline,
                            label = "Still present / not confirmed",
                            value = "${result.failedCount} · ${Formatter.formatFileSize(context, result.failedBytes)}",
                        )
                    }
                    if (result.unchangedCount > 0 && result.status == CleanupResultStatus.CANCELED) {
                        ResultRow(
                            icon = Icons.Outlined.Lock,
                            label = "Unchanged",
                            value = "${result.unchangedCount} · ${Formatter.formatFileSize(context, result.unchangedBytes)}",
                        )
                    }
                }
            }
        }

        result.note?.let { note ->
            item {
                Text(
                    note,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        item {
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ResultRow(
    icon: ImageVector,
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Bold)
    }
}

private data class StatusPresentation(
    val title: String,
    val color: androidx.compose.ui.graphics.Color,
    val icon: ImageVector,
)

@Composable
private fun CleanupResult.statusPresentation(): StatusPresentation = when (status) {
    CleanupResultStatus.COMPLETED -> StatusPresentation(
        title = "Cleanup complete",
        color = MaterialTheme.colorScheme.primary,
        icon = Icons.Outlined.CheckCircle,
    )
    CleanupResultStatus.PARTIAL -> StatusPresentation(
        title = "Cleanup partially completed",
        color = MaterialTheme.colorScheme.tertiary,
        icon = Icons.Outlined.ErrorOutline,
    )
    CleanupResultStatus.CANCELED -> StatusPresentation(
        title = "Cleanup canceled",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = Icons.Outlined.Lock,
    )
    CleanupResultStatus.PERMISSION_REVOKED -> StatusPresentation(
        title = "Media access is unavailable",
        color = MaterialTheme.colorScheme.error,
        icon = Icons.Outlined.ErrorOutline,
    )
    CleanupResultStatus.NO_CHANGE -> StatusPresentation(
        title = "Nothing changed",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        icon = Icons.Outlined.Info,
    )
}
