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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.meshlink.messaging.presentation.ChatDetailScreen
import com.meshlink.messaging.presentation.ChatsListScreen
import com.meshlink.ui.analytics.AnalyticsScreen
import com.meshlink.ui.profile.ProfileSetupScreen
import com.meshlink.ui.broadcast.BroadcastScreen
import com.meshlink.ui.home.HomeScreen
import com.meshlink.ui.nearby.NearbyDevicesScreen
import com.meshlink.ui.settings.SettingsScreen
import com.meshlink.ui.sos.SosScreen
import com.meshlink.util.NotificationHelper


sealed class Screen(val route: String) {
    object Home : Screen("home")
    object ChatsList : Screen("chats")
    object Nearby : Screen("nearby")
    object ChatDetail : Screen("chat/{address}/{name}") {
        fun createRoute(address: String, name: String) = 
            "chat/${android.net.Uri.encode(address)}/${android.net.Uri.encode(name)}"
    }
    object Settings : Screen("settings")
    object ProfileSetup : Screen("profile_setup")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass
) {
    val userRepository: com.meshlink.domain.repository.UserRepository = dagger.hilt.EntryPoints.get(
        androidx.compose.ui.platform.LocalContext.current.applicationContext,
        com.meshlink.di.UserRepositoryEntryPoint::class.java
    ).getUserRepository()
    
    val hasProfile by userRepository.hasProfile.collectAsState(initial = null)

    if (hasProfile == null) {
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize())
        return
    }

    val snackbarHostState = remember { SnackbarHostState() }

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
            val topLevelRoutes = listOf(Screen.Home.route, Screen.Nearby.route, Screen.Settings.route)
            NavHost(
                modifier = Modifier.padding(paddingValues),
                navController = navController,
                startDestination = if (hasProfile == true) Screen.Home.route else Screen.ProfileSetup.route,
                enterTransition = {
                    if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                        fadeIn(tween(210, delayMillis = 90))
                    } else {
                        slideInHorizontally(tween(300)) { (it * 0.2f).toInt() } + fadeIn(tween(300))
                    }
                },
                exitTransition = {
                    if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                        fadeOut(tween(90))
                    } else {
                        fadeOut(tween(300))
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                        fadeIn(tween(210, delayMillis = 90))
                    } else {
                        slideInHorizontally(tween(300)) { -(it * 0.2f).toInt() } + fadeIn(tween(300))
                    }
                },
                popExitTransition = {
                    if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                        fadeOut(tween(90))
                    } else {
                        slideOutHorizontally(tween(300)) { (it * 0.2f).toInt() } + fadeOut(tween(300))
                    }
                }
            ) {
                
                composable(Screen.ProfileSetup.route) {
                    ProfileSetupScreen(
                        onSetupComplete = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(0)
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
                        }
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
                if (currentRoute != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Wifi, contentDescription = "Nearby") },
            label = { Text("Nearby") },
            selected = currentRoute == Screen.Nearby.route,
            onClick = {
                if (currentRoute != Screen.Nearby.route) {
                    navController.navigate(Screen.Nearby.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == Screen.Settings.route,
            onClick = {
                if (currentRoute != Screen.Settings.route) {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
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
                if (currentRoute != Screen.Home.route) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Wifi, contentDescription = "Nearby") },
            label = { Text("Nearby") },
            selected = currentRoute == Screen.Nearby.route,
            onClick = {
                if (currentRoute != Screen.Nearby.route) {
                    navController.navigate(Screen.Nearby.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )

        NavigationRailItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == Screen.Settings.route,
            onClick = {
                if (currentRoute != Screen.Settings.route) {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
    }
}
