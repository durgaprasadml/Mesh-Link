package com.meshlink.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.meshlink.ui.designsystem.shell.MeshScaffold
import com.meshlink.ui.designsystem.motion.MeshTransitionSystem
import androidx.navigation.NavType
import androidx.navigation.*
import androidx.navigation.compose.*
import com.meshlink.messaging.presentation.ChatDetailScreen
import com.meshlink.messaging.presentation.ChatsListScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.meshlink.ui.home.HomeScreen
import com.meshlink.ui.profile.ProfileSetupScreen
import com.meshlink.ui.nearby.NearbyDevicesScreen
import com.meshlink.ui.settings.SettingsScreen
import com.meshlink.ui.broadcast.BroadcastScreen
import com.meshlink.ui.sos.SosScreen
import com.meshlink.util.NotificationHelper
import com.meshlink.ui.landing.LandingScreen
import com.meshlink.ui.diagnostics.MediaDiagnosticsScreen

sealed class Screen(val route: String) {
    object Landing : Screen("landing/{isWelcome}") {
        fun createRoute(isWelcome: Boolean = false) = "landing/$isWelcome"
    }
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
    object Diagnostics : Screen("diagnostics")
    object MediaDiagnostics : Screen("media_diagnostics")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    windowSizeClass: WindowSizeClass,
    viewModel: AppNavigationViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val hasProfile by viewModel.hasProfile.collectAsStateWithLifecycle(initialValue = null)

    if (hasProfile == null) {
        Box(modifier = Modifier.fillMaxSize())
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

    val topLevelRoutes = listOf(
        Screen.Home.route,
        Screen.Nearby.route,
        Screen.Sos.route,
        Screen.Settings.route
    )

    MeshScaffold(
        currentRoute = currentRoute,
        onNavigate = { route ->
            if (currentRoute != route) {
                navController.navigate(route) {
                    popUpTo(Screen.Home.route) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        },
        windowSizeClass = windowSizeClass,
        snackbarHostState = snackbarHostState
    ) {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = if (hasProfile == true) Screen.Landing.createRoute(isWelcome = false) else Screen.ProfileSetup.route,
            enterTransition = {
                if (initialState.destination.route?.startsWith("landing") == true && targetState.destination.route == Screen.Home.route) {
                    androidx.compose.animation.EnterTransition.None
                } else if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                    MeshTransitionSystem.TopLevelEnter
                } else {
                    MeshTransitionSystem.ForwardEnter
                }
            },
            exitTransition = {
                if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                    MeshTransitionSystem.TopLevelExit
                } else {
                    MeshTransitionSystem.ForwardExit
                }
            },
            popEnterTransition = {
                if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                    MeshTransitionSystem.TopLevelEnter
                } else {
                    MeshTransitionSystem.BackEnter
                }
            },
            popExitTransition = {
                if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                    MeshTransitionSystem.TopLevelExit
                } else {
                    MeshTransitionSystem.BackExit
                }
            }
        ) {
            
            composable(
                route = Screen.Landing.route,
                arguments = listOf(
                    navArgument("isWelcome") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                ),
                exitTransition = {
                    if (targetState.destination.route == Screen.Home.route) {
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(450, easing = androidx.compose.animation.core.LinearEasing))
                    } else {
                        androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(300))
                    }
                }
            ) {
                LandingScreen(
                    onAnimationComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Landing.route) { inclusive = true }
                        }
                    }
                )
            }
            
            composable(Screen.ProfileSetup.route) {
                ProfileSetupScreen(
                    onSetupSuccess = {
                        navController.navigate(Screen.Landing.createRoute(isWelcome = true)) {
                            popUpTo(Screen.ProfileSetup.route) { inclusive = true }
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
                    onNavigateToBroadcast = { navController.navigate(Screen.Broadcast.route) },
                    onNavigateToSos = { navController.navigate(Screen.Sos.route) }
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

            composable(Screen.Diagnostics.route) {
                com.meshlink.ui.diagnostics.RoutingDiagnosticsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Screen.MediaDiagnostics.route) {
                MediaDiagnosticsScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
