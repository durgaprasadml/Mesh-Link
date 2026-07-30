package com.meshlink.ui.landing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LandingPhysicsTest {

    @Test
    fun testExactSixNodesAndLogoGeometry() {
        val logoNodes = listOf(
            MeshLogoNode(id = 0, isCenter = true, discoveryStage = 1),
            MeshLogoNode(id = 1, isCenter = false, discoveryStage = 2, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[0]),
            MeshLogoNode(id = 2, isCenter = false, discoveryStage = 3, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[1]),
            MeshLogoNode(id = 3, isCenter = false, discoveryStage = 4, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[2]),
            MeshLogoNode(id = 4, isCenter = false, discoveryStage = 5, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[3]),
            MeshLogoNode(id = 5, isCenter = false, discoveryStage = 6, angleDeg = AnimationConstants.OUTER_NODE_ANGLES_DEG[4])
        )

        // 1. Verify exactly 6 nodes
        assertEquals(6, logoNodes.size)
        assertEquals(AnimationConstants.NODE_COUNT, logoNodes.size)

        // 2. Verify 1 center node and 5 outer nodes
        val centerNode = logoNodes.first { it.isCenter }
        val outerNodes = logoNodes.filter { !it.isCenter }

        assertEquals(0, centerNode.id)
        assertEquals(5, outerNodes.size)

        // 3. Verify screen position computation for center and outer nodes
        val outPosCenter = FloatArray(2)
        centerNode.computePosition(centerX = 500f, centerY = 1000f, logoRadius = 200f, outPos = outPosCenter)
        assertEquals(500f, outPosCenter[0], 0.001f)
        assertEquals(1000f, outPosCenter[1], 0.001f)

        val outPosOuter = FloatArray(2)
        outerNodes[0].computePosition(centerX = 500f, centerY = 1000f, logoRadius = 200f, outPos = outPosOuter)
        assertTrue(outPosOuter[0] != 500f || outPosOuter[1] != 1000f)
    }

    @Test
    fun testTenLightBeamConnections() {
        val logoBeams = listOf(
            MeshLogoBeam(id = 0, fromNodeId = 0, toNodeId = 1, discoveryStage = 2),
            MeshLogoBeam(id = 1, fromNodeId = 0, toNodeId = 2, discoveryStage = 3),
            MeshLogoBeam(id = 2, fromNodeId = 1, toNodeId = 2, discoveryStage = 3),
            MeshLogoBeam(id = 3, fromNodeId = 0, toNodeId = 3, discoveryStage = 4),
            MeshLogoBeam(id = 4, fromNodeId = 2, toNodeId = 3, discoveryStage = 4),
            MeshLogoBeam(id = 5, fromNodeId = 0, toNodeId = 4, discoveryStage = 5),
            MeshLogoBeam(id = 6, fromNodeId = 3, toNodeId = 4, discoveryStage = 5),
            MeshLogoBeam(id = 7, fromNodeId = 0, toNodeId = 5, discoveryStage = 6),
            MeshLogoBeam(id = 8, fromNodeId = 4, toNodeId = 5, discoveryStage = 6),
            MeshLogoBeam(id = 9, fromNodeId = 5, toNodeId = 1, discoveryStage = 6)
        )

        // Verify exactly 10 beams matching the logo
        assertEquals(10, logoBeams.size)
        assertEquals(AnimationConstants.BEAM_COUNT, logoBeams.size)

        // Verify 5 radial beams from Center (Node 0)
        val radialBeams = logoBeams.filter { it.fromNodeId == 0 }
        assertEquals(5, radialBeams.size)

        // Verify 5 outer ring beams
        val ringBeams = logoBeams.filter { it.fromNodeId != 0 }
        assertEquals(5, ringBeams.size)
    }

    @Test
    fun testTimingSequenceBoundaries() {
        assertTrue(AnimationConstants.STARTUP_ANIMATION_DURATION_MS > 3000L)
        assertTrue(AnimationConstants.WELCOME_ANIMATION_DURATION_MS > AnimationConstants.STARTUP_ANIMATION_DURATION_MS)

        assertTrue(AnimationConstants.PROGRESS_DISCOVERY_START < AnimationConstants.PROGRESS_DISCOVERY_END)
        assertTrue(AnimationConstants.PROGRESS_DISCOVERY_END < AnimationConstants.PROGRESS_HOLD_END)
    }

    @Test
    fun testIndependentStarTwinklingPhases() {
        val node1 = MeshLogoNode(id = 1, isCenter = false, discoveryStage = 2)
        val node2 = MeshLogoNode(id = 2, isCenter = false, discoveryStage = 3)

        // Verify different twinkle phase offsets so stars don't blink synchronously
        assertTrue(node1.twinklePhase != node2.twinklePhase)
    }
}

