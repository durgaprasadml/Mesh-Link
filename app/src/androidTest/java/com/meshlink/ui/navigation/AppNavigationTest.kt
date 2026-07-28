package com.meshlink.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import androidx.activity.ComponentActivity
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.ComposeNavigator

@HiltAndroidTest
class AppNavigationTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: TestNavHostController

    @Before
    fun setup() {
        hiltRule.inject()
        composeTestRule.setContent {
            navController = TestNavHostController(ApplicationProvider.getApplicationContext()).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            
            // Provide a WindowSizeClass
            val sizeClass = WindowSizeClass.calculateFromSize(DpSize(400.dp, 800.dp))
            AppNavigation(
                navController = navController,
                windowSizeClass = sizeClass
            )
        }
    }

    @Test
    fun bottomNavigation_clickHomeTwice_doesNotCreateDuplicateDestinations() {
        // Assume we start at Splash and navigate to Home or Login based on Auth state.
        // For testing navigation bar directly, we can manually route to Home first if needed.
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.Home.route)
        }
        
        // Wait for UI to settle
        composeTestRule.waitForIdle()

        // Get initial back stack entry count
        val initialCount = navController.currentBackStack.value.size

        // Click Home tab again
        composeTestRule.onNodeWithContentDescription("Home").performClick()
        composeTestRule.waitForIdle()

        // The back stack size should remain the same (no duplicates)
        val finalCount = navController.currentBackStack.value.size
        assertEquals(initialCount, finalCount)
    }
    
    @Test
    fun bottomNavigation_switchTabs_restoresStateAndPopUpToStart() {
        composeTestRule.runOnUiThread {
            navController.navigate(Screen.Home.route)
        }
        composeTestRule.waitForIdle()
        
        val countAfterHome = navController.currentBackStack.value.size

        // Navigate to Nearby
        composeTestRule.onNodeWithContentDescription("Nearby").performClick()
        composeTestRule.waitForIdle()
        
        assertEquals(Screen.Nearby.route, navController.currentDestination?.route)
        
        // Back stack should only contain start destination and Nearby
        val countAfterNearby = navController.currentBackStack.value.size
        // Assuming Splash or Login was popped, Home is the start destination of our bottom nav conceptually, 
        // though graph start is Splash. Let's just ensure we haven't added on top of a deep stack.
        // Since we popUpTo(findStartDestination()), the stack should not keep growing infinitely.
        assertEquals(countAfterHome, countAfterNearby)
    }
}
