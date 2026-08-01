package com.meshlink.ui.designsystem.gesture

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.responsive.MeshAdaptiveCard
import com.meshlink.ui.designsystem.theme.haptics.rememberMeshHaptics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Gesture Framework for Mesh-Link 2026.
 * Provides Press Effects, Swipe-to-Dismiss, Draggable Containers, Long-Press Handlers,
 * Edge Back Gestures, and Expandable Cards.
 */

fun Modifier.meshPressEffect(
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null
): Modifier = composed {
    val haptics = rememberMeshHaptics()
    val scope = rememberCoroutineScope()
    val scaleAnim = remember { Animatable(1f) }

    this
        .scale(scaleAnim.value)
        .pointerInput(onClick, onLongClick) {
            detectTapGestures(
                onPress = {
                    scope.launch {
                        scaleAnim.animateTo(
                            targetValue = 0.96f,
                            animationSpec = spring(stiffness = Spring.StiffnessHigh)
                        )
                    }
                    tryAwaitRelease()
                    scope.launch {
                        scaleAnim.animateTo(
                            targetValue = 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                },
                onTap = {
                    haptics.buttonPress()
                    onClick?.invoke()
                },
                onLongPress = {
                    haptics.longPress()
                    onLongClick?.invoke()
                }
            )
        }
}

/**
 * 1. Swipe to Dismiss Modifier
 */
fun Modifier.meshSwipeToDismiss(
    onDismiss: () -> Unit,
    thresholdPx: Float = 300f
): Modifier = composed {
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val haptics = rememberMeshHaptics()

    this
        .offset { IntOffset(offsetX.value.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (kotlin.math.abs(offsetX.value) > thresholdPx) {
                        haptics.heavyClick()
                        onDismiss()
                    } else {
                        scope.launch {
                            offsetX.animateTo(
                                targetValue = 0f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            )
                        }
                    }
                },
                onHorizontalDrag = { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        offsetX.snapTo(offsetX.value + dragAmount)
                    }
                }
            )
        }
}

/**
 * 2. Draggable Container Modifier
 */
fun Modifier.meshDraggableContainer(
    onDrag: (deltaX: Float, deltaY: Float) -> Unit = { _, _ -> }
): Modifier = composed {
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()

    this
        .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
        .pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = {
                    // Reset or snap
                    offsetX = 0f
                    offsetY = 0f
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                    onDrag(dragAmount.x, dragAmount.y)
                }
            )
        }
}

/**
 * 3. Long Press Gesture Handler
 */
fun Modifier.meshLongPressGesture(
    onLongPress: () -> Unit
): Modifier = composed {
    val haptics = rememberMeshHaptics()
    this.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                haptics.longPress()
                onLongPress()
            }
        )
    }
}

/**
 * 4. Edge Swipe Gesture Handler (detects swipes starting from screen edges)
 */
fun Modifier.meshEdgeSwipeGesture(
    onEdgeSwipe: () -> Unit,
    edgeThresholdPx: Float = 50f
): Modifier = composed {
    val haptics = rememberMeshHaptics()
    this.pointerInput(Unit) {
        detectHorizontalDragGestures { change, dragAmount ->
            if (change.position.x < edgeThresholdPx && dragAmount > 15f) {
                change.consume()
                haptics.buttonPress()
                onEdgeSwipe()
            }
        }
    }
}

/**
 * 5. Expandable Card Composable & Gesture
 */
@Composable
fun MeshExpandableCard(
    title: @Composable () -> Unit,
    expandedContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    initialExpanded: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(initialExpanded) }
    val haptics = rememberMeshHaptics()

    MeshAdaptiveCard(
        modifier = modifier
            .fillMaxWidth()
            .meshPressEffect(
                onClick = {
                    haptics.selectionClick()
                    isExpanded = !isExpanded
                }
            )
    ) {
        title()
        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                content = expandedContent
            )
        }
    }
}
