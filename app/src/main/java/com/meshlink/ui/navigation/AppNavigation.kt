package com.meshlink.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.meshlink.messaging.presentation.ChatDetailScreen
import com.meshlink.messaging.presentation.ChatsListScreen
import com.meshlink.ui.analytics.AnalyticsScreen
import com.meshlink.ui.auth.AuthViewModel
import com.meshlink.ui.auth.LoginScreen
import com.meshlink.ui.auth.RegistrationScreen
import com.meshlink.ui.auth.RegistrationScreen
import com.meshlink.ui.broadcast.BroadcastScreen
import com.meshlink.ui.home.HomeScreen
import com.meshlink.ui.mesh.MeshDebugScreen
import com.meshlink.ui.nearby.NearbyDevicesScreen
import com.meshlink.ui.settings.SettingsScreen
import com.meshlink.ui.sos.SosScreen
import com.meshlink.util.NotificationHelper


sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Registration : Screen("registration")
    object Home : Screen("home")
    object ChatsList : Screen("chats")
    object Nearby : Screen("nearby")
    object DebugMesh : Screen("debug_mesh")
    object Sos : Screen("sos")
    object ChatDetail : Screen("chat/{address}/{name}") {
        fun createRoute(address: String, name: String) = 
            "chat/${android.net.Uri.encode(address)}/${android.net.Uri.encode(name)}"
    }
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
    object Broadcast : Screen("broadcast")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isUserLoggedIn.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    if (isLoggedIn == null) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
        return
    }

    LaunchedEffect(snackbarHostState) {
        NotificationHelper.inAppNotifications.collect { notification ->
            snackbarHostState.showSnackbar("${notification.senderName}: ${notification.message}")
        }
    }

    val currentBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry.value?.destination?.route

    val isTopLevelScreen = currentRoute in listOf(
        Screen.Home.route,
        Screen.Nearby.route,
        Screen.Sos.route,
        Screen.Settings.route
    )

    val showNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact && isTopLevelScreen
    val showNavigationBar = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact && isTopLevelScreen

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showNavigationBar) {
                MeshNavigationBar(navController, currentRoute)
            }
        }
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize()) {
            if (showNavigationRail) {
                MeshNavigationRail(navController, currentRoute)
            }
        NavHost(
            modifier = Modifier.padding(paddingValues),
            navController = navController,
            startDestination = if (isLoggedIn == true) Screen.Home.route else Screen.Login.route,
            enterTransition = { slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300)) }
        ) {
            
            composable(Screen.Login.route) {
                LoginScreen(
                    onNavigateToRegistration = {
                        navController.navigate(Screen.Registration.route)
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Screen.Registration.route) {
                RegistrationScreen(
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onRegistrationSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                            popUpTo(Screen.Registration.route) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToNearby = { navController.navigate(Screen.Nearby.route) },
                    onNavigateToChat = { address, name ->
                        navController.navigate(Screen.ChatDetail.createRoute(address, name))
                    },
                    onNavigateToSos = { navController.navigate(Screen.Sos.route) },
                    onNavigateToBroadcast = { navController.navigate(Screen.Broadcast.route) }
                )
            }

            composable(Screen.Nearby.route) {
                NearbyDevicesScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { address, name ->
                        navController.navigate(Screen.ChatDetail.createRoute(address, name))
                    }
                )
            }

            composable(Screen.DebugMesh.route) {
                MeshDebugScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Sos.route) {
                SosScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Broadcast.route) {
                BroadcastScreen(
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.ChatsList.route) {
                ChatsListScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToChat = { address, name ->
                        navController.navigate(Screen.ChatDetail.createRoute(address, name))
                    }
                )
            }

            composable(
                route = Screen.ChatDetail.route,
                arguments = listOf(
                    navArgument("address") { 
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("name") { 
                        type = NavType.StringType
                        defaultValue = "Unknown"
                    }
                )
            ) {
                ChatDetailScreen(onBack = { navController.popBackStack() })
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLoggedOut = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Screen.Analytics.route) {
                AnalyticsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            }
        }
    }
}

@Composable
fun MeshNavigationBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == Screen.Home.route,
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Wifi, contentDescription = "Nearby") },
            label = { Text("Nearby") },
            selected = currentRoute == Screen.Nearby.route,
            onClick = {
                navController.navigate(Screen.Nearby.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Warning, contentDescription = "SOS") },
            label = { Text("SOS") },
            selected = currentRoute == Screen.Sos.route,
            onClick = {
                navController.navigate(Screen.Sos.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == Screen.Settings.route,
            onClick = {
                navController.navigate(Screen.Settings.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}

@Composable
fun MeshNavigationRail(navController: NavHostController, currentRoute: String?) {
    NavigationRail {
        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == Screen.Home.route,
            onClick = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Wifi, contentDescription = "Nearby") },
            label = { Text("Nearby") },
            selected = currentRoute == Screen.Nearby.route,
            onClick = {
                navController.navigate(Screen.Nearby.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Warning, contentDescription = "SOS") },
            label = { Text("SOS") },
            selected = currentRoute == Screen.Sos.route,
            onClick = {
                navController.navigate(Screen.Sos.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == Screen.Settings.route,
            onClick = {
                navController.navigate(Screen.Settings.route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        )
    }
}
