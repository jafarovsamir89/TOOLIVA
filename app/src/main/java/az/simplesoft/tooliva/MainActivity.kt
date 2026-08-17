package az.simplesoft.tooliva

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import az.simplesoft.tooliva.ui.ToolivaApp
import az.simplesoft.tooliva.ui.theme.ToolivaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ToolivaTheme {
                ToolivaApp()
            }
        }
    }
}
