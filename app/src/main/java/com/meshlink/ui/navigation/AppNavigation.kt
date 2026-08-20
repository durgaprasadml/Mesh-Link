package com.meshlink.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.meshlink.common.logger.MeshLogger
import com.meshlink.messaging.presentation.ChatDetailScreen
import com.meshlink.messaging.presentation.ChatsListScreen
import com.meshlink.ui.broadcast.BroadcastScreen
import com.meshlink.ui.components.PermissionHandler
import com.meshlink.ui.components.areRadiosAndPermissionsReady
import com.meshlink.ui.components.findActivity
import com.meshlink.ui.components.rememberRadioAndPermissionState
import com.meshlink.ui.home.HomeScreen
import com.meshlink.ui.landing.LandingScreen
import com.meshlink.ui.nearby.NearbyDevicesScreen
import com.meshlink.ui.profile.ProfileSetupScreen
import com.meshlink.ui.settings.SettingsScreen
import com.meshlink.ui.sos.SosScreen
import com.meshlink.util.NotificationHelper

sealed class Screen(val route: String) {
    object Landing : Screen("landing/{isWelcome}") {
        fun createRoute(isWelcome: Boolean = false) = "landing/$isWelcome"
    }
    object Permission : Screen("permission")
    object Home : Screen("home")
    object ChatsList : Screen("chats")
    object Nearby : Screen("nearby")
    object ChatDetail : Screen("chat/{address}/{name}") {
        fun createRoute(address: String, name: String) = 
            "chat/${android.net.Uri.encode(address)}/${android.net.Uri.encode(name)}"
    }
    object Settings : Screen("settings")
    object ProfileSetup : Screen("profile_setup")
    object Sos : Screen("sos")
    object Broadcast : Screen("broadcast")
}

enum class StartupState {
    LANDING,
    PROFILE_SETUP,
    PERMISSIONS,
    MAIN_APP
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass,
    viewModel: AppNavigationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val hasProfile by viewModel.hasProfile.collectAsStateWithLifecycle(initialValue = null)
    val context = androidx.compose.ui.platform.LocalContext.current
    val isRadiosAndPermissionsReady = rememberRadioAndPermissionState(context)

    var startupState by rememberSaveable { mutableStateOf(StartupState.LANDING) }

    MeshLogger.d("AppNavigation", "STARTUP_STATE = $startupState, hasProfile = $hasProfile, radiosReady = $isRadiosAndPermissionsReady")

    when (startupState) {
        StartupState.LANDING -> {
            LandingScreen(
                onAnimationComplete = {
                    val userHasProfile = hasProfile ?: false
                    val nextState = when {
                        !userHasProfile -> StartupState.PROFILE_SETUP
                        !isRadiosAndPermissionsReady -> StartupState.PERMISSIONS
                        else -> StartupState.MAIN_APP
                    }
                    MeshLogger.d("AppNavigation", "Landing complete (hasProfile=$userHasProfile, radiosReady=$isRadiosAndPermissionsReady) -> transition to $nextState")
                    startupState = nextState
                }
            )
        }

        StartupState.PROFILE_SETUP -> {
            BackHandler {
                MeshLogger.d("AppNavigation", "Back pressed on PROFILE_SETUP -> return to LANDING")
                startupState = StartupState.LANDING
            }
            ProfileSetupScreen(
                onSetupSuccess = {
                    val nextState = if (areRadiosAndPermissionsReady(context)) {
                        StartupState.MAIN_APP
                    } else {
                        StartupState.PERMISSIONS
                    }
                    MeshLogger.d("AppNavigation", "Profile created -> transition to $nextState")
                    startupState = nextState
                }
            )
        }

        StartupState.PERMISSIONS -> {
            BackHandler {
                if (hasProfile == false) {
                    MeshLogger.d("AppNavigation", "Back pressed on PERMISSIONS -> return to PROFILE_SETUP")
                    startupState = StartupState.PROFILE_SETUP
                } else {
                    // Cannot bypass mandatory radio requirement
                    context.findActivity()?.moveTaskToBack(true)
                }
            }
            PermissionHandler(
                onPermissionsGranted = {
                    MeshLogger.d("AppNavigation", "Permissions granted -> transition to MAIN_APP")
                    startupState = StartupState.MAIN_APP
                }
            )
        }

        StartupState.MAIN_APP -> {
            if (!isRadiosAndPermissionsReady) {
                // Radio turned off or permissions revoked at runtime while in Main App
                BackHandler {
                    context.findActivity()?.moveTaskToBack(true)
                }
                PermissionHandler(
                    onPermissionsGranted = {
                        MeshLogger.d("AppNavigation", "Radios re-enabled at runtime -> gate dismissed, showing MAIN_APP")
                    }
                )
            } else {
                MainAppScaffold(
                    navController = navController,
                    windowSizeClass = windowSizeClass
                )
            }
        }
    }
}

@Composable
fun MainAppScaffold(
    navController: NavHostController,
    windowSizeClass: WindowSizeClass
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarHostState) {
        NotificationHelper.inAppNotifications.collect { notification ->
            snackbarHostState.showSnackbar("${notification.senderName}: ${notification.message}")
        }
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route

    val isTopLevelScreen = currentRoute in listOf(
        Screen.Home.route,
        Screen.Nearby.route,
        Screen.Sos.route,
        Screen.Settings.route
    )

    val showNavigationRail = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact && isTopLevelScreen
    val showNavigationBar = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Compact && isTopLevelScreen

    val topLevelRoutes = listOf(
        Screen.Home.route,
        Screen.Nearby.route,
        Screen.Sos.route,
        Screen.Settings.route
    )

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            if (showNavigationBar) {
                MeshNavigationBar(navController, currentRoute)
            }
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (showNavigationRail) {
                MeshNavigationRail(navController, currentRoute)
            }
            NavHost(
                modifier = Modifier.fillMaxSize(),
                navController = navController,
                startDestination = Screen.Home.route,
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
                composable(Screen.Home.route) {
                    HomeScreen(
                        onNavigateToSettings = { navController.navigateToTopLevel(Screen.Settings.route) },
                        onNavigateToNearby = { navController.navigateToTopLevel(Screen.Nearby.route) },
                        onNavigateToChat = { address, name ->
                            navController.navigate(Screen.ChatDetail.createRoute(address, name))
                        },
                        onNavigateToBroadcast = { navController.navigate(Screen.Broadcast.route) },
                        onNavigateToSos = { navController.navigateToTopLevel(Screen.Sos.route) }
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
            }
        }
    }
}

/**
 * Shared helper function for navigating to top-level destinations (Home, Nearby, SOS, Settings).
 * Ensures consistent back stack management, single-top behavior, and state preservation.
 */
fun NavHostController.navigateToTopLevel(route: String) {
    if (currentDestination?.route != route) {
        navigate(route) {
            popUpTo(Screen.Home.route) { saveState = true }
            launchSingleTop = true
            restoreState = true
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
            onClick = { navController.navigateToTopLevel(Screen.Home.route) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Wifi, contentDescription = "Nearby") },
            label = { Text("Nearby") },
            selected = currentRoute == Screen.Nearby.route,
            onClick = { navController.navigateToTopLevel(Screen.Nearby.route) }
        )
        
        NavigationBarItem(
            icon = { Icon(Icons.Default.Warning, contentDescription = "SOS") },
            label = { Text("SOS") },
            selected = currentRoute == Screen.Sos.route,
            onClick = { navController.navigateToTopLevel(Screen.Sos.route) }
        )

        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == Screen.Settings.route,
            onClick = { navController.navigateToTopLevel(Screen.Settings.route) }
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
            onClick = { navController.navigateToTopLevel(Screen.Home.route) }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Wifi, contentDescription = "Nearby") },
            label = { Text("Nearby") },
            selected = currentRoute == Screen.Nearby.route,
            onClick = { navController.navigateToTopLevel(Screen.Nearby.route) }
        )
        
        NavigationRailItem(
            icon = { Icon(Icons.Default.Warning, contentDescription = "SOS") },
            label = { Text("SOS") },
            selected = currentRoute == Screen.Sos.route,
            onClick = { navController.navigateToTopLevel(Screen.Sos.route) }
        )

        NavigationRailItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings") },
            selected = currentRoute == Screen.Settings.route,
            onClick = { navController.navigateToTopLevel(Screen.Settings.route) }
        )
    }
}
