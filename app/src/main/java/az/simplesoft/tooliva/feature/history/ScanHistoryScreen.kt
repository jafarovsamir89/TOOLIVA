package az.simplesoft.tooliva.feature.history

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import az.simplesoft.tooliva.core.settings.ScanHistoryRecord
import az.simplesoft.tooliva.core.settings.ToolivaUserDataStore
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import az.simplesoft.tooliva.ui.theme.ToolivaSpacing
import java.text.DateFormat
import java.util.Date

@Composable
fun ScanHistoryRoute(onBack: () -> Unit, viewModel: ScanHistoryViewModel = viewModel()) {
    val history = viewModel.history.collectAsStateWithLifecycle(initialValue = emptyList()).value
    ScanHistoryScreen(history, onBack)
}

@Composable
private fun ScanHistoryScreen(history: List<ScanHistoryRecord>, onBack: () -> Unit) {
    val context = LocalContext.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(ToolivaSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.md),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Scan history", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("Local snapshots from completed storage scans", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
        if (history.isEmpty()) {
            item {
                Card(shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Outlined.History, contentDescription = null)
                        Text("No completed scans yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Run Analyze storage from Clean to start building a local history.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(history) { record ->
                ScanHistoryCard(record, context)
            }
        }
    }
}

@Composable
private fun ScanHistoryCard(record: ScanHistoryRecord, context: android.content.Context) {
    Card(shape = ToolivaShapes.large, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(ToolivaSpacing.xl), verticalArrangement = Arrangement.spacedBy(ToolivaSpacing.sm)) {
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(record.finishedAtMillis)), fontWeight = FontWeight.Bold)
            Text("${Formatter.formatFileSize(context, record.usedBytes)} used of ${Formatter.formatFileSize(context, record.totalBytes)}", color = MaterialTheme.colorScheme.onSurfaceVariant)
            record.largestCategory?.let { category ->
                Text("Largest review group: $category · ${Formatter.formatFileSize(context, record.largestCategoryBytes)}", color = MaterialTheme.colorScheme.primary)
            }
            Text("${record.filesChecked} files checked", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

class ScanHistoryViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val store = ToolivaUserDataStore(application)
    val history = store.scanHistory
}
