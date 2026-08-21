package az.simplesoft.tooliva.feature.clean

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Lock
import az.simplesoft.tooliva.ui.LocalizedIcon as Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import az.simplesoft.tooliva.ui.LocalizedText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import az.simplesoft.tooliva.ui.theme.ToolivaIconTile
import az.simplesoft.tooliva.ui.theme.ToolivaShapes
import az.simplesoft.tooliva.ui.theme.ToolivaSpacing

@Composable
fun StorageAccessCard(
    fullMode: Boolean,
    supported: Boolean,
    errorMessage: String?,
    onEnableFull: () -> Unit,
) {
    Card(
        shape = ToolivaShapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(modifier = Modifier.padding(ToolivaSpacing.xl)) {
            androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(ToolivaSpacing.md), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                ToolivaIconTile(Icons.Outlined.Lock)
                Text(if (fullMode) "Full Storage Mode" else "Limited Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(
                if (fullMode) {
                    "Tooliva can scan accessible shared-storage files, including APKs, archives and documents. Android protected app data remains unavailable."
                } else {
                    "Tooliva is using MediaStore fallback. Results may be limited to media until Full Storage Access is enabled."
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = ToolivaSpacing.sm),
            )
            if (!fullMode && supported) {
                OutlinedButton(onClick = onEnableFull, modifier = Modifier.fillMaxWidth().padding(top = ToolivaSpacing.md)) {
                    Text("Enable Full Storage Access")
                }
            }
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = ToolivaSpacing.sm))
            }
        }
    }
}
