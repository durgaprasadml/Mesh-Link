package com.meshlink.ui.designsystem.theme.colors

import androidx.compose.ui.graphics.Color

/**
 * Primitive Color Tokens for Mesh Link 2026 Design System.
 * Tailored palette inspired by Material 3 Expressive, Nothing OS, Linear, Tesla App & Signal.
 */
object MeshColorTokens {
    // ── Brand & Accent Palette ──
    val CyberMint = Color(0xFF00F59B)
    val CyberMintDark = Color(0xFF00C875)
    val ElectricCyan = Color(0xFF00E5FF)
    val NeonBlue = Color(0xFF007AFF)
    val SignalTeal = Color(0xFF00F5A8)
    val PlasmaPurple = Color(0xFF9D4EDD)

    // ── Neutral Palette (Light) ──
    val PureWhite = Color(0xFFFFFFFF)
    val NeutralLightBg = Color(0xFFF8FAFC)
    val NeutralLightSurface = Color(0xFFF1F5F9)
    val NeutralLightElevated = Color(0xFFE2E8F0)
    val NeutralLightBorder = Color(0xFFCBD5E1)
    val NeutralLightOutline = Color(0xFF94A3B8)
    val NeutralLightTextPrimary = Color(0xFF0F172A)
    val NeutralLightTextSecondary = Color(0xFF475569)
    val NeutralLightTextTertiary = Color(0xFF64748B)

    // ── Neutral Palette (Dark) ──
    val NeutralDarkBg = Color(0xFF0B0F17)
    val NeutralDarkSurface = Color(0xFF141A26)
    val NeutralDarkElevated = Color(0xFF1E293B)
    val NeutralDarkBorder = Color(0xFF2D3A4F)
    val NeutralDarkOutline = Color(0xFF475569)
    val NeutralDarkTextPrimary = Color(0xFFF8FAFC)
    val NeutralDarkTextSecondary = Color(0xFF94A3B8)
    val NeutralDarkTextTertiary = Color(0xFF64748B)

    // ── Neutral Palette (AMOLED) ──
    val PitchBlack = Color(0xFF000000)
    val AmoledSurface = Color(0xFF070B10)
    val AmoledElevated = Color(0xFF0E1520)
    val AmoledBorder = Color(0xFF182232)
    val AmoledOutline = Color(0xFF334155)

    // ── Semantic Raw Colors ──
    val SuccessGreen = Color(0xFF30D158)
    val SuccessGreenLight = Color(0xFF16A34A)
    val WarningAmber = Color(0xFFFF9F0A)
    val DangerRed = Color(0xFFFF453A)
    val DangerRedLight = Color(0xFFDC2626)
    val InfoBlue = Color(0xFF64D2FF)

    // ── Mesh Connectivity Raw Colors ──
    val MeshConnected = CyberMint
    val MeshSearching = WarningAmber
    val MeshOffline = Color(0xFF8E8E93)

    // ── Signal Strength Raw Colors ──
    val SignalExcellent = CyberMint
    val SignalMedium = WarningAmber
    val SignalWeak = DangerRed

    // ── Glass Surfaces ──
    val GlassSurfaceLight = Color(0xB8FFFFFF) // 72% opacity
    val GlassSurfaceDark = Color(0xB8141A26)
    val GlassSurfaceAmoled = Color(0xB8070B10)
    val GlassBorderLight = Color(0x33000000)
    val GlassBorderDark = Color(0x33FFFFFF)
    val GlassBorderAmoled = Color(0x4000F59B)
}
