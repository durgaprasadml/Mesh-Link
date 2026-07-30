package com.meshlink.ui.landing

import androidx.compose.ui.graphics.Color

/**
 * Design system tokens and timing constants for the minimal, premium Mesh Link
 * Wireless Network Discovery Landing Animation.
 */
object AnimationConstants {

    // ── Single Accent Design System: Soft Neon Green ─────────────────────────
    val DeepCharcoalBg           = Color(0xFF0B0B0B)
    val RadialVignetteCenter     = Color(0xFF03140A)
    val SoftNeonGreen            = Color(0xFF00FF66)
    val SoftNeonGreenBright      = Color(0xFF33FF85)
    val SoftNeonGreenGlowOuter   = Color(0x3000FF66)
    val SoftNeonGreenGlowMedium  = Color(0x6000FF66)
    val SoftNeonGreenGlowInner   = Color(0x9900FF66)
    val BeamGlowAura             = Color(0x4000FF66)
    val StarCoreWhite            = Color(0xFFFFFFFF)
    val FaintStarColor           = Color(0x2600FF66) // ~15% opacity faint star in deep space

    // ── Node & Beam Geometry Parameters ──────────────────────────────────────
    const val NODE_COUNT = 6
    const val BEAM_COUNT = 10

    // Normalized angles (degrees) for the 5 outer nodes around center (Node 0)
    // Node 0: Center (0, 0)
    // Node 1: Top-Left (-126°)
    // Node 2: Top-Right (-54°)
    // Node 3: Bottom-Right (18°)
    // Node 4: Bottom-Center (90°)
    // Node 5: Bottom-Left (162°)
    val OUTER_NODE_ANGLES_DEG = floatArrayOf(-126f, -54f, 18f, 90f, 162f)

    // Node radii in dp (All 6 nodes are visually identical)
    const val NODE_RADIUS_DP = 7.0f
    const val CENTER_NODE_RADIUS_DP = 7.0f
    const val NODE_GLOW_RADIUS_DP = 22.0f

    // Line widths in dp
    const val BEAM_CORE_WIDTH_DP = 2.0f
    const val BEAM_GLOW_WIDTH_DP = 6.0f

    // ── Accelerating Discovery Sequence Timings (ms) ─────────────────────────
    const val START_PAUSE_MS = 100L
    val NODE_STAGE_DURATIONS_MS = longArrayOf(180L, 170L, 160L, 150L, 140L, 140L)
    const val DISCOVERY_TOTAL_MS = 940L          // 180 + 170 + 160 + 150 + 140 + 140
    const val LOGO_HOLD_MS = 400L                // Unified breathing hold
    const val WELCOME_TEXT_HOLD_MS = 600L        // First-time welcome text hold
    const val CENTER_ZOOM_DURATION_MS = 780L    // Camera zoom into center node

    // Standard total duration for returning users (~2.22s)
    const val STARTUP_ANIMATION_DURATION_MS =
        START_PAUSE_MS + DISCOVERY_TOTAL_MS + LOGO_HOLD_MS + CENTER_ZOOM_DURATION_MS

    // Total duration for first-time users (~2.82s)
    const val WELCOME_ANIMATION_DURATION_MS =
        STARTUP_ANIMATION_DURATION_MS + WELCOME_TEXT_HOLD_MS
}

/**
 * Representation of a beam connection pair in the 6-node Mesh Link logo.
 */
data class BeamConnectionPair(
    val fromNodeId: Int,
    val toNodeId: Int,
    val discoveryStage: Int // Stage (1..6) when this beam travels
)

