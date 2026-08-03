package com.meshlink.trust

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrustEvaluatorTest {

    private val trustStore: TrustStore = mockk(relaxed = true)
    private lateinit var evaluator: TrustEvaluator

    @Before
    fun setup() {
        evaluator = TrustEvaluator(trustStore)
    }

    @Test
    fun testUnknownUserEvaluation() {
        every { trustStore.getTrustLevel("peer-1") } returns TrustLevel.UNKNOWN

        val capabilities = evaluator.evaluate("peer-1")
        assertTrue(capabilities.showWarning)
        assertFalse(capabilities.canJoinCommunity)
        assertFalse(capabilities.canAdministerCommunity)
    }

    @Test
    fun testVerifiedUserEvaluation() {
        every { trustStore.getTrustLevel("peer-2") } returns TrustLevel.VERIFIED

        val capabilities = evaluator.evaluate("peer-2")
        assertFalse(capabilities.showWarning)
        assertTrue(capabilities.canBroadcast)
        assertTrue(capabilities.canDirectMessage)
        assertTrue(capabilities.canJoinCommunity)
    }

    @Test
    fun testBlockedUserEvaluation() {
        every { trustStore.getTrustLevel("peer-3") } returns TrustLevel.BLOCKED

        val capabilities = evaluator.evaluate("peer-3")
        assertTrue(capabilities.showWarning)
        assertFalse(capabilities.canBroadcast)
        assertFalse(capabilities.canDirectMessage)
    }

    @Test
    fun testAdminRoleEvaluation() {
        every { trustStore.getTrustLevel("peer-4") } returns TrustLevel.VERIFIED

        val capabilities = evaluator.evaluate(
            peerMeshId = "peer-4",
            role = CommunityRole.ADMIN
        )
        assertTrue(capabilities.canAdministerCommunity)
        assertTrue(capabilities.canModerateCommunity)
    }
}
