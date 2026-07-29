package com.meshlink.ui.landing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LandingPhysicsTest {

    @Test
    fun testGenerateNodesCountAndUserNode() {
        val nodes = NodePhysics.generateNodes(isWelcomeMode = true)
        
        assertEquals(AnimationConstants.REGULAR_NODE_COUNT, nodes.size)
        val userNode = nodes.firstOrNull { it.isUserNode }
        assertNotNull(userNode)
        assertTrue(userNode!!.isUserNode)
        assertEquals(0, userNode.id)
    }

    @Test
    fun testGenerateAmbientDustParticles() {
        val dust = NodePhysics.generateAmbientDust()
        assertEquals(AnimationConstants.AMBIENT_DUST_PARTICLE_COUNT, dust.size)
    }

    @Test
    fun testBuildConnections() {
        val nodes = NodePhysics.generateNodes(isWelcomeMode = false)
        val connectionAnimator = MeshConnectionAnimator()
        connectionAnimator.buildConnections(nodes)

        assertTrue(connectionAnimator.links.isNotEmpty())
    }

    @Test
    fun testNodePhysicsPositionUpdate() {
        val nodes = NodePhysics.generateNodes(isWelcomeMode = false)
        val initialX = nodes[1].currentX
        val initialY = nodes[1].currentY

        NodePhysics.updatePositions(
            nodes = nodes,
            width = 1000f,
            height = 2000f,
            timeMs = 1000L,
            overallProgress = 0.5f,
            reduceMotion = false
        )

        assertTrue(nodes[1].currentX != initialX || nodes[1].currentY != initialY)
    }
}
