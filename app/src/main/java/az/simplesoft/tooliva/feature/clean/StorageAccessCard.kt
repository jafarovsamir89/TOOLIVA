package az.simplesoft.tooliva.feature.clean

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun StorageAccessCard(
    fullMode: Boolean,
    supported: Boolean,
    errorMessage: String?,
    onEnableFull: () -> Unit,
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                if (fullMode) "Full Storage Mode" else "Limited Mode",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (fullMode) {
                    "Tooliva can scan accessible shared-storage files, including APKs, archives and documents. Android protected app data remains unavailable."
                } else {
                    "Tooliva is using MediaStore fallback. Results may be limited to media until Full Storage Access is enabled."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            if (!fullMode && supported) {
                OutlinedButton(onClick = onEnableFull, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    Text("Enable Full Storage Access")
                }
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}

