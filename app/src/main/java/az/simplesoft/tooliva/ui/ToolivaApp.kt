package az.simplesoft.tooliva.ui

import android.net.Uri
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
import androidx.navigation.NavType
import androidx.navigation.navArgument
import az.simplesoft.tooliva.feature.clean.CleanRoute
import az.simplesoft.tooliva.feature.clean.largefiles.LargeFilesRoute
import az.simplesoft.tooliva.feature.clean.downloads.DownloadsAnalyzerRoute
import az.simplesoft.tooliva.feature.clean.recommendations.CleanupRecommendationsRoute
import az.simplesoft.tooliva.feature.clean.cache.CacheCleanupRoute
import az.simplesoft.tooliva.feature.clean.screenshots.ScreenshotCleanerRoute
import az.simplesoft.tooliva.feature.home.HomeRoute
import az.simplesoft.tooliva.feature.optimizer.PhoneOptimizerRoute
import az.simplesoft.tooliva.feature.files.FileManagerRoute
import az.simplesoft.tooliva.feature.clean.duplicates.ExactDuplicatesRoute
import az.simplesoft.tooliva.feature.doctor.CheckupRoute
import az.simplesoft.tooliva.feature.doctor.HardwareTestsRoute
import az.simplesoft.tooliva.feature.doctor.PhoneDoctorRoute
import az.simplesoft.tooliva.feature.appmanager.AppManagerRoute
import az.simplesoft.tooliva.feature.notifications.NotificationHistoryRoute
import az.simplesoft.tooliva.feature.storage.StorageMapRoute
import az.simplesoft.tooliva.feature.clean.swipe.CleanupSwipeRoute
import az.simplesoft.tooliva.feature.clean.oldfiles.OldFilesRoute
import az.simplesoft.tooliva.feature.clean.emptyfolders.EmptyFoldersRoute

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
                        selected = topLevelRoute(currentRoute) == destination.route,
                        onClick = {
                            navController.navigate(destination.route) {
                                // Nested Cleaner screens must not be restored when the user
                                // explicitly chooses a top-level destination such as Home.
                                popUpTo("home") { saveState = false }
                                launchSingleTop = true
                                restoreState = false
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
                            "notifications" -> "notification-history"
                            "storage-map" -> "storage-map"
                            "doctor" -> "doctor"
                            "hardware" -> "hardware"
                            "files" -> "files"
                            "qr" -> "tools"
                            "optimizer" -> "optimizer"
                            "app-manager" -> "app-manager"
                            else -> "tools"
                        }
                        navController.navigate(route)
                    },
                )
            }
            composable("clean") {
                CleanRoute(onOpenTool = { navController.navigate("clean/$it") })
            }
            composable("clean/large-files") { LargeFilesRoute(onOpenInFiles = { path -> navController.navigate("files?path=${Uri.encode(path)}") }) }
            composable("clean/downloads") { DownloadsAnalyzerRoute() }
            composable("clean/recommendations") { CleanupRecommendationsRoute() }
            composable("clean/cache") { CacheCleanupRoute() }
            composable("optimizer") { PhoneOptimizerRoute() }
            composable("app-manager") { AppManagerRoute(onBack = { navController.popBackStack() }) }
            composable("clean/screenshots") { ScreenshotCleanerRoute() }
            composable("clean/duplicates") { ExactDuplicatesRoute() }
            composable("clean/old-files") { OldFilesRoute() }
            composable("clean/empty-folders") { EmptyFoldersRoute() }
            composable("protect") { ModulePlaceholder("Protect", "App Lock, Vault and privacy tools.") }
            composable("tools") { ModulePlaceholder("Tools", "QR, network, compass and quick tools.") }
            composable("more") { ModulePlaceholder("More", "Settings, Pro and additional utilities.") }
            composable("notification-history") { NotificationHistoryRoute(onBack = { navController.popBackStack() }) }
            composable("storage-map") {
                StorageMapRoute(
                    onBack = { navController.popBackStack() },
                    onOpenInFiles = { directory -> navController.navigate("files?path=${Uri.encode(directory.absolutePath)}") },
                )
            }
            composable("notifications") { NotificationHistoryRoute(onBack = { navController.popBackStack() }) }
            composable("doctor") {
                PhoneDoctorRoute(
                    onBack = { navController.popBackStack() },
                    onHardwareTests = { navController.navigate("hardware") },
                    onCheckup = { navController.navigate("checkup") },
                    onOpenStorageTool = { id ->
                        if (id == "files") navController.navigate("files") else navController.navigate("clean/$id")
                    },
                )
            }
            composable("hardware") { HardwareTestsRoute(onBack = { navController.popBackStack() }) }
            composable("files") {
                FileManagerRoute(onOpenLargeFiles = { navController.navigate("clean/large-files") })
            }
            composable("files?path={path}", arguments = listOf(navArgument("path") { type = NavType.StringType; nullable = true; defaultValue = null })) { entry ->
                FileManagerRoute(onOpenLargeFiles = { navController.navigate("clean/large-files") }, initialDirectory = entry.arguments?.getString("path")?.let(Uri::decode))
            }
            composable("clean/cleanup-swipe") { CleanupSwipeRoute(onBack = { navController.popBackStack() }, onOpenInFiles = { file -> navController.navigate("files?path=${Uri.encode(file.parentFile?.absolutePath)}") }) }
            composable("checkup") {
                CheckupRoute(
                    onBack = { navController.popBackStack() },
                    onOpenAction = { id ->
                        if (id == "optimizer") navController.navigate("optimizer")
                        else navController.navigate("clean/$id")
                    },
                )
            }
        }
    }
}

internal fun topLevelRoute(route: String?): String? = route?.substringBefore('/')

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
