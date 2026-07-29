package com.meshlink.ui.landing

import androidx.compose.ui.graphics.Color

/**
 * Design system tokens, starlight monochrome color palette, timing constants, and configuration parameters
 * for the Mesh Link Cinematic Constellation Experience.
 */
object AnimationConstants {

    // Monochrome Starlight Color Palette
    val DeepSpaceNavy = Color(0xFF050710)
    val SpaceDarkCharcoal = Color(0xFF0A0E1A)
    val StarlightWhite = Color(0xFFFAFAFA)
    val StarlightWhiteGlow = Color(0x66FFFFFF)
    val StarlightSilver = Color(0xFFCBD5E1)
    val StarlightSilverGlow = Color(0x44CBD5E1)
    val StarGreyDim = Color(0xFF475569)
    val StarGreyDimTransparent = Color(0x22475569)
    val SubtleStarlightBlue = Color(0xFFE0F2FE)
    val SoftWhiteTransparent = Color(0x80F0F4F8)
    val DimBackground = Color(0xCC050710)

    // Connection Type Colors (Monochrome Starlight Silver Variants)
    val ConnectionDiscovery = Color(0x88CBD5E1)
    val ConnectionConnected = Color(0xEEFAFAFA)
    val ConnectionConstellation = Color(0xFFFFFFFF)
    val ConnectionInactive = Color(0x11475569)

    // Timing Durations
    const val STARTUP_ANIMATION_DURATION_MS = 7000L
    const val WELCOME_ANIMATION_DURATION_MS = 8000L

    // Phase Time Boundaries (Normalized 0.0f to 1.0f)
    const val PHASE_1_TWINKLE_END = 0.12f
    const val PHASE_2_SEED_IGNITION_END = 0.25f
    const val PHASE_3_WAVE_PROPAGATION_END = 0.50f
    const val PHASE_4_CONSTELLATION_MIGRATION_END = 0.72f
    const val PHASE_5_LIVING_CONSTELLATION_END = 0.88f
    const val PHASE_6_FINAL_PULSE_ZOOM_END = 1.00f

    // Starfield & Graph Parameters
    const val TOTAL_STAR_COUNT = 140
    const val MAX_CONNECTIONS_PER_NODE = 3
    const val STAR_CONNECT_RADIUS_RATIO = 0.22f // Normalized distance threshold

    // Node & Star Sizes
    const val MIN_STAR_RADIUS_DP = 1.5f
    const val MAX_STAR_RADIUS_DP = 3.5f
    const val SEED_STAR_RADIUS_DP = 6.0f
    const val USER_AVATAR_STAR_RADIUS_DP = 32.0f
}

/**
 * Visual classification of connection edges in the constellation graph.
 */
enum class ConnectionType {
    DISCOVERY,
    CONNECTED,
    CONSTELLATION,
    INACTIVE
}
