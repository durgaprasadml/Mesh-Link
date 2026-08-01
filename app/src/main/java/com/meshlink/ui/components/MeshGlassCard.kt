package com.meshlink.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.meshlink.ui.designsystem.theme.GlassAlpha
import com.meshlink.ui.designsystem.theme.MeshTheme

/**
 * Glassmorphism card component for the "Signal" design language.
 *
 * Implements a pseudo-glass effect that works on all API levels (no BlurMaskFilter needed):
 * - Semi-transparent surface color fill (configurable alpha)
 * - Gradient border: lighter top-left edge → transparent bottom-right
 * - Optional inner glow drawn via a radial gradient on the background canvas
 * - Rounded corners consistent with the MeshShapes scale
 *
 * Use this for hero cards, canvas containers, and SOS status cards where depth
 * and visual premium-ness matter most.
 */
@Composable
fun MeshGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 20.dp,
    /** Override the base fill color. Defaults to surfaceContainerHigh at [GlassAlpha]. */
    fillColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = GlassAlpha.toFloat()),
    /** Whether to show the gradient border highlight (top-left bright edge). */
    showBorderGradient: Boolean = true,
    /** Optional accent glow color drawn as a radial bloom at the top of the card. */
    glowColor: Color = Color.Transparent,
    glowRadius: Float = 0f,
    content: @Composable BoxScope.() -> Unit
) {
    val glassEffects = MeshTheme.glassEffects
    val borderColor = MaterialTheme.colorScheme.outlineVariant

    // Effective fill: if glass effects disabled, use opaque surfaceContainerHigh
    val effectiveFill = if (glassEffects) {
        fillColor
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }

    val shape = RoundedCornerShape(cornerRadius)

    Box(
        modifier = modifier
            .clip(shape)
            .drawBehind {
                // ── Background Fill ──────────────────────────────────────────
                drawRoundRect(
                    color = effectiveFill,
                    cornerRadius = CornerRadius(cornerRadius.toPx())
                )

                // ── Optional Inner Glow ─────────────────────────────────────
                if (glowColor != Color.Transparent && glowRadius > 0f && glassEffects) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.18f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, 0f),
                            radius = glowRadius
                        ),
                        radius = glowRadius,
                        center = Offset(size.width * 0.5f, 0f)
                    )
                }
            }
            .then(
                if (showBorderGradient) {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = if (glassEffects) listOf(
                                borderColor.copy(alpha = 0.80f),
                                borderColor.copy(alpha = 0.20f),
                                Color.Transparent
                            ) else listOf(
                                borderColor.copy(alpha = 0.40f),
                                borderColor.copy(alpha = 0.40f)
                            )
                        ),
                        shape = shape
                    )
                } else Modifier
            ),
        content = content
    )
}
