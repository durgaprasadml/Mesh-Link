package com.meshlink.ui.auth

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.MeshSpacing

/**
 * Animated Setup Stepper Progress Indicator.
 * Displays step progression: Welcome • Walkthrough • Permissions • Profile • Identity • Ready.
 */
@Composable
fun SetupProgress(
    currentStepIndex: Int,
    totalSteps: Int = 6,
    stepLabels: List<String> = listOf("Welcome", "Tour", "Permissions", "Profile", "Identity", "Ready"),
    modifier: Modifier = Modifier
) {
    val reducedMotion = LocalReducedMotion.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MeshSpacing.ScreenPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        stepLabels.forEachIndexed { index, label ->
            val isCompleted = index < currentStepIndex
            val isCurrent = index == currentStepIndex

            val circleColor by animateColorAsState(
                targetValue = when {
                    isCurrent -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                },
                animationSpec = if (reducedMotion) tween(0) else tween(300),
                label = "circleColor_$index"
            )

            val indicatorWidth by animateFloatAsState(
                targetValue = if (isCurrent) 28f else 10f,
                animationSpec = if (reducedMotion) tween(0) else OnboardingAnimations.gentleSpring(),
                label = "indicatorWidth_$index"
            )

            Box(
                modifier = Modifier
                    .height(10.dp)
                    .width(indicatorWidth.dp)
                    .clip(CircleShape)
                    .background(circleColor)
            )

            if (index < stepLabels.lastIndex) {
                val lineColor by animateColorAsState(
                    targetValue = if (index < currentStepIndex) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    animationSpec = if (reducedMotion) tween(0) else tween(300),
                    label = "lineColor_$index"
                )
                Box(
                    modifier = Modifier
                        .height(2.dp)
                        .width(12.dp)
                        .padding(horizontal = 2.dp)
                        .background(lineColor)
                )
            }
        }
    }
}
