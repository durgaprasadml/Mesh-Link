package com.meshlink.ui.landing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandingPhysicsTest {

    @Test
    fun testGenerateStarNodesAndUserSeedNode() {
        val nodes = NodePhysics.generateNodes(isWelcomeMode = true)

        assertEquals(AnimationConstants.TOTAL_STAR_COUNT, nodes.size)
        val userNode = nodes.firstOrNull { it.isUserNode }
        assertNotNull(userNode)
        assertTrue(userNode!!.isUserNode)
        assertEquals(0, userNode.id)
    }

    @Test
    fun testConstellationTextLayoutPointsAndEdges() {
        val layout = ConstellationTextLayout.generateLayout()
        assertTrue(layout.points.isNotEmpty())
        assertTrue(layout.edges.isNotEmpty())
    }

    @Test
    fun testBuildWaveConnectionsAndConstellationStrokes() {
        val nodes = NodePhysics.generateNodes(isWelcomeMode = false)
        val connectionAnimator = MeshConnectionAnimator()
        connectionAnimator.buildConnections(nodes)

        assertTrue(connectionAnimator.links.isNotEmpty())
        val constellationStrokes = connectionAnimator.links.filter { it.isConstellationStroke }
        assertTrue(constellationStrokes.isNotEmpty())
    }

    @Test
    fun testIndependentStarTwinklingAndMigrationPositionUpdate() {
        val nodes = NodePhysics.generateNodes(isWelcomeMode = false)
        val initialX = nodes[1].currentX
        val initialY = nodes[1].currentY

        NodePhysics.updatePositions(
            nodes = nodes,
            width = 1000f,
            height = 2000f,
            timeMs = 1000L,
            overallProgress = 0.6f,
            reduceMotion = false
        )

        assertTrue(nodes[1].currentX != initialX || nodes[1].currentY != initialY)
        assertTrue(nodes[1].currentBrightness > 0f)
    }
}
