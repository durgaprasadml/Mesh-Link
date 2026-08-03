package com.meshlink.trust

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommunityMembershipManagerTest {

    private val trustStore: TrustStore = mockk(relaxed = true)
    private val auditLog: IdentityAuditLog = mockk(relaxed = true)
    private lateinit var membershipManager: CommunityMembershipManager

    @Before
    fun setup() {
        membershipManager = CommunityMembershipManager(trustStore, auditLog)
    }

    @Test
    fun testJoinCommunitySuccess() {
        every { trustStore.getTrustLevel("user-1") } returns TrustLevel.VERIFIED

        val result = membershipManager.joinCommunity(
            meshId = "user-1",
            communityId = "hostel-101",
            requestedRole = CommunityRole.MEMBER
        )

        assertTrue(result)
        val member = membershipManager.getMember("hostel-101", "user-1")
        assertTrue(member != null)
        assertEquals("user-1", member?.meshId)
    }

    @Test
    fun testJoinRestrictedPolicyFailsForUnverifiedUser() {
        every { trustStore.getTrustLevel("user-2") } returns TrustLevel.UNKNOWN
        membershipManager.setCommunityPolicy("hostel-strict", CommunityPolicy.hostelPolicy())

        val result = membershipManager.joinCommunity(
            meshId = "user-2",
            communityId = "hostel-strict"
        )

        assertFalse(result)
    }
}
