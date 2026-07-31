package com.meshlink.ui

import com.meshlink.ui.designsystem.theme.LayoutConstants
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Layout Regression Unit Tests for Mesh Link.
 * Guarantees layout tokens, bottom bar height, FAB margins, and spacing values stay regression-free.
 */
class LayoutRegressionTest {

    @Test
    fun testLayoutConstantsValues() {
        assertEquals(80.dp, LayoutConstants.BottomBarHeight)
        assertEquals(64.dp, LayoutConstants.TopAppBarHeight)
        assertEquals(56.dp, LayoutConstants.FabSize)
        assertEquals(16.dp, LayoutConstants.FabBottomMargin)
        assertEquals(16.dp, LayoutConstants.ScreenHorizontalPadding)
        assertEquals(12.dp, LayoutConstants.CardSpacing)
    }

    @Test
    fun testViewportCalculations() {
        // Assert that TopAppBar + Content spacing leaves expected content height for compact screens
        val sampleScreenHeight = 800.dp
        val usableContentHeight = sampleScreenHeight - LayoutConstants.TopAppBarHeight - LayoutConstants.BottomBarHeight
        assertEquals(656.dp, usableContentHeight)
        assertTrue("Usable content height must exceed 500dp for standard viewports", usableContentHeight > 500.dp)
    }
}
