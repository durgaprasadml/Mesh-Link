package com.meshlink.ui.designsystem.theme.colors

import androidx.compose.ui.graphics.Color

/**
 * Primitive Color Tokens for Mesh Link 2026 Original Design System.
 * High-contrast, tactile tactical telemetry palette for offline mesh communications.
 */
object MeshColorTokens {
    // ── Brand & Accent Core Palette ──
    val CyberMint = Color(0xFF00F59B)          // Primary Brand Mint / Signal Active
    val CyberMintDark = Color(0xFF00C875)      // Darkened Primary
    val QuantumCyan = Color(0xFF00E5FF)        // Secondary Core / Encryption Status
    val ElectricCyan = Color(0xFF00E5FF)       // Alias
    val TacticalTeal = Color(0xFF00F5A8)       // Telemetry Accent
    val SignalTeal = Color(0xFF00F5A8)         // Alias
    val PlasmaPurple = Color(0xFF9D4EDD)       // Routing & Special Payload Accent

    // ── Emergency & Status Raw Colors ──
    val EmergencyCrimson = Color(0xFFFF2A4B)   // High-Vis Emergency SOS Alert
    val DangerRed = Color(0xFFFF453A)          // Danger / High Alert
    val DangerRedLight = Color(0xFFDC2626)     // Danger Light Mode
    val SuccessGreen = Color(0xFF22C55E)       // Success / Secure Peer
    val SuccessGreenLight = Color(0xFF16A34A)  // Success Light Mode
    val WarningAmber = Color(0xFFFFB300)       // Warning / Scanning / Syncing
    val InfoSkyBlue = Color(0xFF38BDF8)        // Info / Metadata / Broadcast
    val InfoBlue = Color(0xFF64D2FF)           // Info Dark Mode

    // ── Mesh Telemetry & Connectivity Raw Colors ──
    val MeshConnected = CyberMint
    val MeshDisconnected = Color(0xFF64748B)    // Slate Neutral
    val MeshSearching = WarningAmber
    val MeshBroadcasting = QuantumCyan
    val MeshEmergency = EmergencyCrimson
    val MeshEncrypted = Color(0xFF38BDF8)

    // ── Signal & RSSI Raw Colors ──
    val SignalStrong = CyberMint               // Excellent RSSI (-30 to -65 dBm)
    val SignalExcellent = CyberMint
    val SignalMedium = WarningAmber            // Fair RSSI (-66 to -85 dBm)
    val SignalWeak = DangerRed                 // Poor RSSI (-86 to -110 dBm)
    val RSSIHigh = CyberMint
    val RSSIMedium = WarningAmber
    val RSSILow = DangerRed

    // ── Neutral Palette (Light Mode) ──
    val PureWhite = Color(0xFFFFFFFF)
    val NeutralLightCanvas = Color(0xFFF8FAFC)
    val NeutralLightBg = Color(0xFFF8FAFC)
    val NeutralLightSurface = Color(0xFFFFFFFF)
    val NeutralLightElevated = Color(0xFFF1F5F9)
    val NeutralLightCard = Color(0xFFFFFFFF)
    val NeutralLightBorder = Color(0xFFE2E8F0)
    val NeutralLightDivider = Color(0xFFE2E8F0)
    val NeutralLightOutline = Color(0xFFCBD5E1)
    val NeutralLightTextPrimary = Color(0xFF0F172A)
    val NeutralLightTextSecondary = Color(0xFF475569)
    val NeutralLightTextTertiary = Color(0xFF64748B)

    // ── Neutral Palette (Dark Mode) ──
    val NeutralDarkCanvas = Color(0xFF070B12)  // Deep Obsidian Canvas
    val NeutralDarkBg = Color(0xFF070B12)
    val NeutralDarkSurface = Color(0xFF0F172A)
    val NeutralDarkElevated = Color(0xFF1E293B)
    val NeutralDarkCard = Color(0xFF0F172A)
    val NeutralDarkBorder = Color(0xFF1E293B)
    val NeutralDarkDivider = Color(0xFF1E293B)
    val NeutralDarkOutline = Color(0xFF334155)
    val NeutralDarkTextPrimary = Color(0xFFF8FAFC)
    val NeutralDarkTextSecondary = Color(0xFF94A3B8)
    val NeutralDarkTextTertiary = Color(0xFF64748B)

    // ── Neutral Palette (AMOLED Dark Mode) ──
    val PitchBlack = Color(0xFF000000)          // OLED Pure Black Canvas
    val AmoledCanvas = Color(0xFF000000)
    val AmoledSurface = Color(0xFF05080E)
    val AmoledElevated = Color(0xFF0D131F)
    val AmoledCard = Color(0xFF05080E)
    val AmoledBorder = Color(0xFF162032)
    val AmoledDivider = Color(0xFF162032)
    val AmoledOutline = Color(0xFF26354A)

    // ── Glass, Overlay & Interaction Tokens ──
    val GlassSurfaceLight = Color(0xCCFFFFFF)   // 80% White Glass
    val GlassSurfaceDark = Color(0xCC0F172A)    // 80% Obsidian Glass
    val GlassSurfaceAmoled = Color(0xCC05080E)  // 80% Pure OLED Glass
    val GlassBorderLight = Color(0x26000000)
    val GlassBorderDark = Color(0x3300F59B)    // Luminous Keyline Mint Border
    val GlassBorderAmoled = Color(0x4000F59B)  // Keyline Mint Border for AMOLED

    val OverlayLight = Color(0x40000000)
    val OverlayDark = Color(0x99000000)

    val FocusGlow = CyberMint
    val SelectionGlow = QuantumCyan
    val HoverLight = Color(0x0F000000)
    val HoverDark = Color(0x1AFFFFFF)
    val PressedLight = Color(0x1F000000)
    val PressedDark = Color(0x33FFFFFF)
    val DisabledLight = Color(0x6694A3B8)
    val DisabledDark = Color(0x66334155)
}
