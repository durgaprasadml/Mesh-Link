package com.meshlink.ui.designsystem.layout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.responsive.LocalMeshDeviceProfile
import com.meshlink.ui.designsystem.responsive.MeshDeviceProfile
import com.meshlink.ui.designsystem.shell.LocalMeshWindowInsets
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Production-Grade Layout Framework for Mesh-Link 2026.
 * Provides all standard adaptive layout templates: Hero, Dashboard, List, Detail, Split, FullScreen, Tablet, Foldable.
 */

@Composable
fun MeshPageLayout(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MeshTheme.colors.background,
    content: @Composable (PaddingValues) -> Unit
) {
    val insets = LocalMeshWindowInsets.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (topBar != null) {
                    topBar()
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    content(insets.safeContentPadding)
                }
                if (bottomBar != null) {
                    bottomBar()
                }
            }

            if (floatingActionButton != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = insets.navigationBarHeight + 80.dp, end = 20.dp)
                ) {
                    floatingActionButton()
                }
            }
        }
    }
}

/**
 * 1. Hero Layout Template
 */
@Composable
fun MeshHeroLayout(
    heroContent: @Composable () -> Unit,
    bodyContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    heroHeight: Dp = 220.dp
) {
    MeshPageLayout(
        modifier = modifier,
        topBar = topBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(heroHeight),
                contentAlignment = Alignment.Center
            ) {
                heroContent()
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                content = bodyContent
            )
        }
    }
}

/**
 * 2. Dashboard Layout Template
 */
@Composable
fun MeshDashboardLayout(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    profile: MeshDeviceProfile = LocalMeshDeviceProfile.current,
    metricsSection: (@Composable ColumnScope.() -> Unit)? = null,
    gridContent: LazyGridScope.() -> Unit
) {
    MeshPageLayout(
        modifier = modifier,
        topBar = topBar
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (metricsSection != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = profile.defaultPadding, vertical = 8.dp),
                    content = metricsSection
                )
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(profile.recommendedColumns),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(profile.defaultPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                content = gridContent
            )
        }
    }
}

/**
 * 3. List Layout Template
 */
@Composable
fun MeshListLayout(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    listContent: LazyListScope.() -> Unit
) {
    MeshPageLayout(
        modifier = modifier,
        topBar = topBar,
        floatingActionButton = floatingActionButton
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding,
            content = listContent
        )
    }
}

/**
 * 4. Detail Layout Template
 */
@Composable
fun MeshDetailLayout(
    modifier: Modifier = Modifier,
    topBar: (@Composable () -> Unit)? = null,
    bottomBar: (@Composable () -> Unit)? = null,
    floatingActionButton: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    MeshPageLayout(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        floatingActionButton = floatingActionButton
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            content = content
        )
    }
}

/**
 * 5. Split Layout Template (Master-Detail / Side-by-side)
 */
@Composable
fun MeshSplitLayout(
    primaryContent: @Composable ColumnScope.() -> Unit,
    secondaryContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    splitRatio: Float = 0.4f,
    dividerColor: Color = Color.White.copy(alpha = 0.1f)
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(splitRatio)
                .fillMaxHeight()
        ) {
            primaryContent()
        }
        Spacer(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(dividerColor)
        )
        Column(
            modifier = Modifier
                .weight(1f - splitRatio)
                .fillMaxHeight()
        ) {
            secondaryContent()
        }
    }
}

/**
 * 6. Full Screen Layout Template (Edge-to-Edge)
 */
@Composable
fun MeshFullScreenLayout(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MeshTheme.colors.background,
    content: @Composable BoxScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            content = content
        )
    }
}

/**
 * 7. Tablet Layout Template
 */
@Composable
fun MeshTabletLayout(
    navigationRail: (@Composable () -> Unit)? = null,
    primaryPane: @Composable ColumnScope.() -> Unit,
    secondaryPane: (@Composable ColumnScope.() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxSize()) {
        if (navigationRail != null) {
            Box(modifier = Modifier.fillMaxHeight()) {
                navigationRail()
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .weight(if (secondaryPane != null) 0.5f else 1.0f)
                    .fillMaxHeight()
                    .padding(24.dp)
            ) {
                primaryPane()
            }
            if (secondaryPane != null) {
                Spacer(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(Color.White.copy(alpha = 0.08f))
                )
                Column(
                    modifier = Modifier
                        .weight(0.5f)
                        .fillMaxHeight()
                        .padding(24.dp)
                ) {
                    secondaryPane()
                }
            }
        }
    }
}

/**
 * 8. Foldable Layout Template (Dual Pane / Posture Aware)
 */
@Composable
fun MeshFoldableLayout(
    leftOrTopPane: @Composable ColumnScope.() -> Unit,
    rightOrBottomPane: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    isVerticalSplit: Boolean = true
) {
    if (isVerticalSplit) {
        Row(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                leftOrTopPane()
            }
            Spacer(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(16.dp)
            ) {
                rightOrBottomPane()
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                leftOrTopPane()
            }
            Spacer(
                modifier = Modifier
                    .height(1.dp)
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f))
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                rightOrBottomPane()
            }
        }
    }
}
