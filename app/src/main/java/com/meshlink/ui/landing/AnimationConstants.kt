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

    // Node radii in dp
    const val NODE_RADIUS_DP = 7.0f
    const val CENTER_NODE_RADIUS_DP = 8.5f
    const val NODE_GLOW_RADIUS_DP = 22.0f

    // Line widths in dp
    const val BEAM_CORE_WIDTH_DP = 2.0f
    const val BEAM_GLOW_WIDTH_DP = 6.0f

    // ── Discovery Sequence Timings (ms) ──────────────────────────────────────
    const val START_PAUSE_MS = 300L
    const val STAGE_DISCOVERY_DURATION_MS = 350L // Per node discovery stage
    const val LOGO_HOLD_MS = 900L               // Unified breathing hold
    const val WELCOME_TEXT_HOLD_MS = 1000L        // First-time welcome text hold
    const val CENTER_ZOOM_DURATION_MS = 1200L     // Camera zoom into center node

    // Standard total duration for returning users (~3.45s)
    const val STARTUP_ANIMATION_DURATION_MS =
        START_PAUSE_MS + (STAGE_DISCOVERY_DURATION_MS * 6) + LOGO_HOLD_MS + CENTER_ZOOM_DURATION_MS

    // Total duration for first-time users (~4.45s)
    const val WELCOME_ANIMATION_DURATION_MS =
        STARTUP_ANIMATION_DURATION_MS + WELCOME_TEXT_HOLD_MS

    // Normalized progress phase boundaries (0.0f -> 1.0f)
    const val PROGRESS_PAUSE_END = 0.08f
    const val PROGRESS_DISCOVERY_START = 0.08f
    const val PROGRESS_DISCOVERY_END = 0.62f
    const val PROGRESS_HOLD_END = 0.80f
}

/**
 * Representation of a beam connection pair in the 6-node Mesh Link logo.
 */
data class BeamConnectionPair(
    val fromNodeId: Int,
    val toNodeId: Int,
    val discoveryStage: Int // Stage (1..6) when this beam travels
)

