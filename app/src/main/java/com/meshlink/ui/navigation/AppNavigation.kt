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
import androidx.compose.ui.unit.dp
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Modifier
import com.meshlink.ui.designsystem.theme.MeshSpacing
import androidx.navigation.NavType
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
            val topLevelRoutes = listOf(Screen.Home.route, Screen.Nearby.route, Screen.Sos.route, Screen.Settings.route)
            NavHost(
                modifier = Modifier.padding(
                    androidx.compose.foundation.layout.PaddingValues(
                        top = 0.dp,
                        bottom = paddingValues.calculateBottomPadding()
                    )
                ),
                navController = navController,
                startDestination = if (hasProfile == true) Screen.Landing.createRoute(isWelcome = false) else Screen.ProfileSetup.route,
                enterTransition = {
                    if (initialState.destination.route?.startsWith("landing") == true && targetState.destination.route == Screen.Home.route) {
                        androidx.compose.animation.EnterTransition.None
                    } else if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                        fadeIn(tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + 
                            androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = tween(250))
                    } else {
                        slideInHorizontally(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (it * 0.15f).toInt() } + 
                            fadeIn(tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.96f, animationSpec = tween(300))
                    }
                },
                exitTransition = {
                    if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                        fadeOut(tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + 
                            androidx.compose.animation.scaleOut(targetScale = 0.95f, animationSpec = tween(180))
                    } else {
                        fadeOut(tween(250)) + androidx.compose.animation.scaleOut(targetScale = 0.96f, animationSpec = tween(250))
                    }
                },
                popEnterTransition = {
                    if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                        fadeIn(tween(250, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + 
                            androidx.compose.animation.scaleIn(initialScale = 0.95f, animationSpec = tween(250))
                    } else {
                        slideInHorizontally(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { -(it * 0.15f).toInt() } + 
                            fadeIn(tween(300)) + androidx.compose.animation.scaleIn(initialScale = 0.96f, animationSpec = tween(300))
                    }
                },
                popExitTransition = {
                    if (initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes) {
                        fadeOut(tween(180, easing = androidx.compose.animation.core.FastOutSlowInEasing)) + 
                            androidx.compose.animation.scaleOut(targetScale = 0.95f, animationSpec = tween(180))
                    } else {
                        slideOutHorizontally(tween(300, easing = androidx.compose.animation.core.FastOutSlowInEasing)) { (it * 0.15f).toInt() } + 
                            fadeOut(tween(250))
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
                            fadeOut(tween(450, easing = androidx.compose.animation.core.LinearEasing))
                        } else {
                            fadeOut(tween(300))
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
}

@Composable
fun MeshNavigationBar(navController: NavHostController, currentRoute: String?) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val sosSelected = currentRoute == Screen.Sos.route

    // Standard nav item colors
    val navItemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.primary,
        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
    )

    // SOS always uses error color tokens — emergency accessibility signal
    val sosNavColors = NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onErrorContainer,
        selectedTextColor = MaterialTheme.colorScheme.error,
        indicatorColor = MaterialTheme.colorScheme.errorContainer,
        unselectedIconColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
        unselectedTextColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
    )

    NavigationBar(
        modifier = Modifier.defaultMinSize(minHeight = MeshSpacing.BottomNavHeight),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 0.dp
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = {
                Text(
                    "Home",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (currentRoute == Screen.Home.route) FontWeight.Bold else FontWeight.Medium
                )
            },
            selected = currentRoute == Screen.Home.route,
            colors = navItemColors,
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
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
            label = {
                Text(
                    "Nearby",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (currentRoute == Screen.Nearby.route) FontWeight.Bold else FontWeight.Medium
                )
            },
            selected = currentRoute == Screen.Nearby.route,
            colors = navItemColors,
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
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
            icon = { Icon(Icons.Default.Warning, contentDescription = "SOS") },
            label = {
                Text(
                    "SOS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            selected = sosSelected,
            colors = sosNavColors,
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                if (currentRoute != Screen.Sos.route) {
                    navController.navigate(Screen.Sos.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = {
                Text(
                    "Settings",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (currentRoute == Screen.Settings.route) FontWeight.Bold else FontWeight.Medium
                )
            },
            selected = currentRoute == Screen.Settings.route,
            colors = navItemColors,
            onClick = {
                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
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
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        NavigationRailItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home", style = MaterialTheme.typography.labelSmall) },
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
            label = { Text("Nearby", style = MaterialTheme.typography.labelSmall) },
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
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = "SOS",
                    tint = if (currentRoute == Screen.Sos.route)
                        MaterialTheme.colorScheme.onErrorContainer
                    else
                        MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
                )
            },
            label = {
                Text("SOS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            },
            selected = currentRoute == Screen.Sos.route,
            colors = androidx.compose.material3.NavigationRailItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onErrorContainer,
                selectedTextColor = MaterialTheme.colorScheme.error,
                indicatorColor = MaterialTheme.colorScheme.errorContainer,
                unselectedIconColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f),
                unselectedTextColor = MaterialTheme.colorScheme.error.copy(alpha = 0.72f)
            ),
            onClick = {
                if (currentRoute != Screen.Sos.route) {
                    navController.navigate(Screen.Sos.route) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        )
        NavigationRailItem(
            icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
            label = { Text("Settings", style = MaterialTheme.typography.labelSmall) },
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
