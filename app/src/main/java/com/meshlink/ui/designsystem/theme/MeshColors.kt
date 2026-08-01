package com.meshlink.ui.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.meshlink.ui.designsystem.theme.colors.AmoledSemanticColors
import com.meshlink.ui.designsystem.theme.colors.DarkSemanticColors
import com.meshlink.ui.designsystem.theme.colors.LightSemanticColors
import com.meshlink.ui.designsystem.theme.colors.LocalMeshSemanticColors
import com.meshlink.ui.designsystem.theme.colors.MeshColorTokens

const val GlassAlpha = 0.72f

val BrandPrimary = MeshColorTokens.CyberMint
val BrandPrimaryDark = MeshColorTokens.CyberMintDark
val BrandSecondary = MeshColorTokens.QuantumCyan

val SurfaceLight = MeshColorTokens.NeutralLightSurface
val SurfaceDark = MeshColorTokens.NeutralDarkSurface
val BackgroundLight = MeshColorTokens.NeutralLightBg
val BackgroundDark = MeshColorTokens.NeutralDarkBg
val BackgroundAmoled = MeshColorTokens.PitchBlack

val ErrorColor = MeshColorTokens.DangerRedLight
val ErrorColorDark = MeshColorTokens.EmergencyCrimson

val SuccessColor = MeshColorTokens.SuccessGreenLight
val SuccessColorDark = MeshColorTokens.SuccessGreen
val WarningColor = MeshColorTokens.WarningAmber
val InfoColor = MeshColorTokens.InfoSkyBlue

val MeshLightColorScheme = lightColorScheme(
    primary = LightSemanticColors.primary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF003820),
    secondary = LightSemanticColors.secondary,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F7FA),
    onSecondaryContainer = Color(0xFF00363D),
    background = LightSemanticColors.background,
    onBackground = LightSemanticColors.textPrimary,
    surface = LightSemanticColors.surface,
    onSurface = LightSemanticColors.textPrimary,
    surfaceVariant = LightSemanticColors.surfaceVariant,
    onSurfaceVariant = LightSemanticColors.textSecondary,
    outline = LightSemanticColors.outline,
    outlineVariant = LightSemanticColors.border,
    error = LightSemanticColors.danger,
    onError = Color.White
)

val MeshDarkColorScheme = darkColorScheme(
    primary = DarkSemanticColors.primary,
    onPrimary = Color(0xFF003820),
    primaryContainer = Color(0xFF005230),
    onPrimaryContainer = Color(0xFF99FFE0),
    secondary = DarkSemanticColors.secondary,
    onSecondary = Color(0xFF00363D),
    secondaryContainer = Color(0xFF004F59),
    onSecondaryContainer = Color(0xFFB5F5FF),
    background = DarkSemanticColors.background,
    onBackground = DarkSemanticColors.textPrimary,
    surface = DarkSemanticColors.surface,
    onSurface = DarkSemanticColors.textPrimary,
    surfaceVariant = DarkSemanticColors.surfaceVariant,
    onSurfaceVariant = DarkSemanticColors.textSecondary,
    outline = DarkSemanticColors.outline,
    outlineVariant = DarkSemanticColors.border,
    error = DarkSemanticColors.danger,
    onError = Color.White
)

val MeshAmoledColorScheme = MeshDarkColorScheme.copy(
    background = AmoledSemanticColors.background,
    surface = AmoledSemanticColors.surface,
    surfaceContainer = AmoledSemanticColors.surfaceVariant
)

val LocalMeshSemanticColorsExport = LocalMeshSemanticColors
