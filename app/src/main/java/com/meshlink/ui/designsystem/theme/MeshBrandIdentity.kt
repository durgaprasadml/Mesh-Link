package com.meshlink.ui.designsystem.theme

import androidx.compose.runtime.Immutable

/**
 * # Mesh-Link Brand Identity & Design System Philosophy
 * **Identity**: "Aether Grid / Tactile Mesh Protocol" (2026 Edition)
 *
 * Mesh-Link is a mission-critical, offline-first tactical mesh communication platform.
 * It delivers high-contrast readability, immediate status clarity under emergency stress,
 * and elegant telemetry feedback (monospaced RSSI metrics, hop counters, cryptographic status).
 */
@Immutable
object MeshBrandIdentity {
    const val BRAND_NAME = "Mesh-Link"
    const val IDENTITY_CODENAME = "Aether Grid 2026"
    const val TAGLINE = "Off-Grid Decentralized Tactical Mesh Network"

    object Principles {
        const val TACTILE_TELEMETRY = "Every network state (RSSI, SNR, Hops, Encryption) is explicit, visible, and tactile."
        const val CLARITY_UNDER_STRESS = "Emergency alerts, peer discovery, and messaging must be instantly recognizable at high contrast."
        const val LUMINOUS_KEYLINES = "Subtle 0.5dp luminous keyline borders separate elevated glass surfaces from dark canvas voids."
        const val EMPOWERED_OFFLINE = "Zero dependence on cloud servers; visual feedback focuses on local peer-to-peer topology."
    }

    object ColorPhilosophy {
        const val SUMMARY = "Deep obsidian backgrounds (#070B12 & #000000 AMOLED) accented by Luminous Signal Mint (#00F59B), Quantum Cyan (#00E5FF), and High-Vis Emergency Crimson (#FF2A4B)."
    }

    object TypographyPhilosophy {
        const val SUMMARY = "Customized weight scaling paired with dedicated Monospace Telemetry metrics for hardware-grade data display."
    }

    object MotionPhilosophy {
        const val SUMMARY = "Snappy, spring-driven micro-interactions (200-300ms) with distinct radar scan pulses, signal ripples, and emergency alerts."
    }

    object LayoutPhilosophy {
        const val SUMMARY = "Unified 4dp grid scale with responsive breakpoints accommodating single-hand phones, foldables, and tactical tablets."
    }

    object AccessibilityPhilosophy {
        const val SUMMARY = "Strict WCAG AA contrast compliance, minimum 48dp touch targets, full dynamic font scaling, and reduced-motion fallback support."
    }
}
