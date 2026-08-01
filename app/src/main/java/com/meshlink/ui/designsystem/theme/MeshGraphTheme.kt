package com.meshlink.ui.designsystem.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.meshlink.ui.designsystem.theme.colors.MeshSemanticColors

@Immutable
data class MeshNodeColors(
    val hubGlow: Color,
    val hubSurface: Color,
    val hubOutline: Color,
    val hubCore: Color,
    val hubCoreDot: Color,
    val connectedGlow: Color,
    val connectedSurface: Color,
    val connectedCore: Color,
    val relayGlow: Color,
    val relaySurface: Color,
    val relayCore: Color,
    val discoveredGlow: Color,
    val discoveredSurface: Color,
    val discoveredCore: Color,
    val weakGlow: Color,
    val weakSurface: Color,
    val weakCore: Color,
    val selectedOutline: Color,
    val selectedGlow: Color,
    val labelText: Color,
    val glowAlpha: Float
)

@Immutable
data class MeshConnectionColors(
    val connectedLine: Color,
    val relayLine: Color,
    val discoveredLine: Color,
    val weakLine: Color,
    val radarPulse: Color,
    val beamSparkCore: Color,
    val beamSparkGlow: Color,
    val packetText: Color,
    val packetMedia: Color,
    val packetLocation: Color,
    val packetVoice: Color
)

@Immutable
data class MeshGraphColors(
    val background: Color,
    val backgroundGradientStart: Color,
    val backgroundGradientEnd: Color,
    val containerBorder: Color,
    val containerBackground: Color,
    val nodes: MeshNodeColors,
    val connections: MeshConnectionColors
)

object MeshGraphTheme {
    fun from(
        colorScheme: ColorScheme,
        semanticColors: MeshSemanticColors
    ): MeshGraphColors {
        val bgLuminance = colorScheme.background.luminance()
        val isAmoled = colorScheme.background == BackgroundAmoled
        val isDark = bgLuminance < 0.2f

        val glowAlpha = when {
            isAmoled -> 0.45f
            isDark -> 0.30f
            else -> 0.15f
        }

        val gradientStart = when {
            isAmoled -> colorScheme.primary.copy(alpha = 0.10f)
            isDark -> colorScheme.primary.copy(alpha = 0.14f)
            else -> colorScheme.primary.copy(alpha = 0.08f)
        }

        val gradientEnd = when {
            isAmoled -> Color.Transparent
            isDark -> colorScheme.secondary.copy(alpha = 0.04f)
            else -> colorScheme.surfaceVariant.copy(alpha = 0.30f)
        }

        val containerBg = when {
            isAmoled -> Color.Black
            isDark -> colorScheme.surface
            else -> colorScheme.surface
        }

        return MeshGraphColors(
            background = colorScheme.background,
            backgroundGradientStart = gradientStart,
            backgroundGradientEnd = gradientEnd,
            containerBorder = colorScheme.outlineVariant.copy(alpha = if (isDark) 0.35f else 0.50f),
            containerBackground = containerBg,
            nodes = MeshNodeColors(
                hubGlow = colorScheme.primary.copy(alpha = glowAlpha),
                hubSurface = colorScheme.primaryContainer,
                hubOutline = colorScheme.primary,
                hubCore = colorScheme.primaryContainer,
                hubCoreDot = colorScheme.onPrimaryContainer,
                connectedGlow = colorScheme.secondary.copy(alpha = glowAlpha),
                connectedSurface = colorScheme.surfaceVariant,
                connectedCore = colorScheme.secondary,
                relayGlow = colorScheme.tertiary.copy(alpha = glowAlpha),
                relaySurface = colorScheme.surfaceVariant,
                relayCore = colorScheme.tertiary,
                discoveredGlow = colorScheme.outline.copy(alpha = glowAlpha * 0.7f),
                discoveredSurface = colorScheme.surfaceVariant,
                discoveredCore = colorScheme.outline,
                weakGlow = semanticColors.signalWeak.copy(alpha = glowAlpha * 0.7f),
                weakSurface = colorScheme.surfaceVariant,
                weakCore = semanticColors.signalWeak,
                selectedOutline = colorScheme.onSurface,
                selectedGlow = colorScheme.primary.copy(alpha = glowAlpha * 1.3f),
                labelText = colorScheme.onSurface,
                glowAlpha = glowAlpha
            ),
            connections = MeshConnectionColors(
                connectedLine = colorScheme.primary.copy(alpha = if (isDark) 0.85f else 0.65f),
                relayLine = colorScheme.tertiary.copy(alpha = if (isDark) 0.80f else 0.60f),
                discoveredLine = colorScheme.secondary.copy(alpha = if (isDark) 0.60f else 0.45f),
                weakLine = semanticColors.signalWeak.copy(alpha = 0.50f),
                radarPulse = colorScheme.primary.copy(alpha = if (isDark) 0.35f else 0.25f),
                beamSparkCore = colorScheme.onPrimary,
                beamSparkGlow = colorScheme.primary,
                packetText = colorScheme.primary,
                packetMedia = colorScheme.secondary,
                packetLocation = colorScheme.tertiary,
                packetVoice = semanticColors.warning
            )
        )
    }
}

@Composable
fun animateMeshGraphColors(target: MeshGraphColors): MeshGraphColors {
    val duration = 300
    val bg = animateColorAsState(target.background, tween(duration), label = "GraphBg").value
    val bgStart = animateColorAsState(target.backgroundGradientStart, tween(duration), label = "GraphBgStart").value
    val bgEnd = animateColorAsState(target.backgroundGradientEnd, tween(duration), label = "GraphBgEnd").value
    val border = animateColorAsState(target.containerBorder, tween(duration), label = "GraphBorder").value
    val containerBg = animateColorAsState(target.containerBackground, tween(duration), label = "ContainerBg").value

    val hubGlow = animateColorAsState(target.nodes.hubGlow, tween(duration), label = "HubGlow").value
    val hubSurface = animateColorAsState(target.nodes.hubSurface, tween(duration), label = "HubSurface").value
    val hubOutline = animateColorAsState(target.nodes.hubOutline, tween(duration), label = "HubOutline").value
    val hubCore = animateColorAsState(target.nodes.hubCore, tween(duration), label = "HubCore").value
    val hubCoreDot = animateColorAsState(target.nodes.hubCoreDot, tween(duration), label = "HubCoreDot").value

    val connGlow = animateColorAsState(target.nodes.connectedGlow, tween(duration), label = "ConnGlow").value
    val connSurface = animateColorAsState(target.nodes.connectedSurface, tween(duration), label = "ConnSurface").value
    val connCore = animateColorAsState(target.nodes.connectedCore, tween(duration), label = "ConnCore").value

    val relayGlow = animateColorAsState(target.nodes.relayGlow, tween(duration), label = "RelayGlow").value
    val relaySurface = animateColorAsState(target.nodes.relaySurface, tween(duration), label = "RelaySurface").value
    val relayCore = animateColorAsState(target.nodes.relayCore, tween(duration), label = "RelayCore").value

    val discGlow = animateColorAsState(target.nodes.discoveredGlow, tween(duration), label = "DiscGlow").value
    val discSurface = animateColorAsState(target.nodes.discoveredSurface, tween(duration), label = "DiscSurface").value
    val discCore = animateColorAsState(target.nodes.discoveredCore, tween(duration), label = "DiscCore").value

    val weakGlow = animateColorAsState(target.nodes.weakGlow, tween(duration), label = "WeakGlow").value
    val weakSurface = animateColorAsState(target.nodes.weakSurface, tween(duration), label = "WeakSurface").value
    val weakCore = animateColorAsState(target.nodes.weakCore, tween(duration), label = "WeakCore").value

    val selOutline = animateColorAsState(target.nodes.selectedOutline, tween(duration), label = "SelOutline").value
    val selGlow = animateColorAsState(target.nodes.selectedGlow, tween(duration), label = "SelGlow").value
    val labelText = animateColorAsState(target.nodes.labelText, tween(duration), label = "LabelText").value

    val connLine = animateColorAsState(target.connections.connectedLine, tween(duration), label = "ConnLine").value
    val relayLine = animateColorAsState(target.connections.relayLine, tween(duration), label = "RelayLine").value
    val discLine = animateColorAsState(target.connections.discoveredLine, tween(duration), label = "DiscLine").value
    val weakLine = animateColorAsState(target.connections.weakLine, tween(duration), label = "WeakLine").value
    val radarPulse = animateColorAsState(target.connections.radarPulse, tween(duration), label = "RadarPulse").value
    val sparkCore = animateColorAsState(target.connections.beamSparkCore, tween(duration), label = "SparkCore").value
    val sparkGlow = animateColorAsState(target.connections.beamSparkGlow, tween(duration), label = "SparkGlow").value

    val pText = animateColorAsState(target.connections.packetText, tween(duration), label = "PText").value
    val pMedia = animateColorAsState(target.connections.packetMedia, tween(duration), label = "PMedia").value
    val pLoc = animateColorAsState(target.connections.packetLocation, tween(duration), label = "PLoc").value
    val pVoice = animateColorAsState(target.connections.packetVoice, tween(duration), label = "PVoice").value

    return MeshGraphColors(
        background = bg,
        backgroundGradientStart = bgStart,
        backgroundGradientEnd = bgEnd,
        containerBorder = border,
        containerBackground = containerBg,
        nodes = MeshNodeColors(
            hubGlow = hubGlow,
            hubSurface = hubSurface,
            hubOutline = hubOutline,
            hubCore = hubCore,
            hubCoreDot = hubCoreDot,
            connectedGlow = connGlow,
            connectedSurface = connSurface,
            connectedCore = connCore,
            relayGlow = relayGlow,
            relaySurface = relaySurface,
            relayCore = relayCore,
            discoveredGlow = discGlow,
            discoveredSurface = discSurface,
            discoveredCore = discCore,
            weakGlow = weakGlow,
            weakSurface = weakSurface,
            weakCore = weakCore,
            selectedOutline = selOutline,
            selectedGlow = selGlow,
            labelText = labelText,
            glowAlpha = target.nodes.glowAlpha
        ),
        connections = MeshConnectionColors(
            connectedLine = connLine,
            relayLine = relayLine,
            discoveredLine = discLine,
            weakLine = weakLine,
            radarPulse = radarPulse,
            beamSparkCore = sparkCore,
            beamSparkGlow = sparkGlow,
            packetText = pText,
            packetMedia = pMedia,
            packetLocation = pLoc,
            packetVoice = pVoice
        )
    )
}

val MaterialTheme.meshGraphColors: MeshGraphColors
    @Composable
    get() = MeshGraphTheme.from(MaterialTheme.colorScheme, MeshTheme.colors)
