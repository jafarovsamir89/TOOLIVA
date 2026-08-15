package az.simplesoft.tooliva.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import az.simplesoft.tooliva.feature.home.HomeRoute

private data class TopDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination("home", "Home", Icons.Outlined.Home),
    TopDestination("clean", "Clean", Icons.Outlined.CleaningServices),
    TopDestination("protect", "Protect", Icons.Outlined.Security),
    TopDestination("tools", "Tools", Icons.Outlined.Workspaces),
    TopDestination("more", "More", Icons.Outlined.MoreHoriz),
)

@Composable
fun ToolivaApp(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar {
                topDestinations.forEach { destination ->
                    NavigationBarItem(
                        selected = currentRoute == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo("home") { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding),
        ) {
            composable("home") {
                HomeRoute(
                    onCheckup = { navController.navigate("checkup") },
                    onOpenTool = { id ->
                        val route = when (id) {
                            "clean" -> "clean"
                            "protect" -> "protect"
                            "notifications" -> "notifications"
                            "doctor" -> "doctor"
                            "files" -> "files"
                            "qr" -> "tools"
                            else -> "tools"
                        }
                        navController.navigate(route)
                    },
                )
            }
            composable("clean") { ModulePlaceholder("Clean", "Storage analysis starts here.") }
            composable("protect") { ModulePlaceholder("Protect", "App Lock, Vault and privacy tools.") }
            composable("tools") { ModulePlaceholder("Tools", "QR, network, compass and quick tools.") }
            composable("more") { ModulePlaceholder("More", "Settings, Pro and additional utilities.") }
            composable("notifications") { ModulePlaceholder("Notification History", "Local notification history module.") }
            composable("doctor") { ModulePlaceholder("Phone Doctor", "Battery, thermal, memory and sensor diagnostics.") }
            composable("files") { ModulePlaceholder("File Tools", "Image, PDF and metadata utilities.") }
            composable("checkup") { ModulePlaceholder("Phone Checkup", "Guided device checkup pipeline.") }
        }
    }
}

@Composable
private fun ModulePlaceholder(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
