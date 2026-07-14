package com.meshlink.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import com.meshlink.ui.auth.SplashScreen
import com.meshlink.ui.broadcast.BroadcastScreen
import com.meshlink.ui.home.HomeScreen
import com.meshlink.ui.mesh.MeshDebugScreen
import com.meshlink.ui.nearby.NearbyDevicesScreen
import com.meshlink.ui.settings.SettingsScreen
import com.meshlink.ui.sos.SosScreen
import com.meshlink.util.NotificationHelper
import java.net.URLEncoder

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Registration : Screen("registration")
    object Home : Screen("home")
    object ChatsList : Screen("chats")
    object Nearby : Screen("nearby")
    object DebugMesh : Screen("debug_mesh")
    object Sos : Screen("sos")
    object ChatDetail : Screen("chat/{address}/{name}") {
        fun createRoute(address: String, name: String) = 
            "chat/${URLEncoder.encode(address, "UTF-8")}/${URLEncoder.encode(name, "UTF-8")}"
    }
    object Settings : Screen("settings")
    object Analytics : Screen("analytics")
    object Broadcast : Screen("broadcast")
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isUserLoggedIn.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarHostState) {
        NotificationHelper.inAppNotifications.collect { notification ->
            snackbarHostState.showSnackbar("${notification.senderName}: ${notification.message}")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        NavHost(
            modifier = Modifier.padding(paddingValues),
            navController = navController,
            startDestination = Screen.Splash.route,
            enterTransition = { slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300)) },
            exitTransition = { fadeOut(tween(300)) },
            popEnterTransition = { fadeIn(tween(300)) },
            popExitTransition = { slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(300)) }
        ) {
            composable(Screen.Splash.route) {
                SplashScreen(
                    onTimeout = {
                        val dest = if (isLoggedIn) Screen.Home.route else Screen.Login.route
                        navController.navigate(dest) {
                            popUpTo(Screen.Splash.route) { inclusive = true }
                        }
                    }
                )
            }
            
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
                    onNavigateToChats = { navController.navigate(Screen.ChatsList.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToNearby = { navController.navigate(Screen.Nearby.route) },
                    onNavigateToChat = { address, name ->
                        navController.navigate(Screen.ChatDetail.createRoute(address, name))
                    },
                    onNavigateToMeshDebug = { navController.navigate(Screen.DebugMesh.route) },
                    onNavigateToSos = { navController.navigate(Screen.Sos.route) },
                    onNavigateToAnalytics = { navController.navigate(Screen.Analytics.route) },
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
