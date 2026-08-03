package com.meshlink.trust

import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CommunityInvitationManagerTest {

    private val identityManager: MeshIdentityManager = mockk(relaxed = true)
    private val trustStore: TrustStore = mockk(relaxed = true)
    private val membershipManager: CommunityMembershipManager = mockk(relaxed = true)
    private lateinit var invitationManager: CommunityInvitationManager

    @Before
    fun setup() {
        val adminIdentity = MeshIdentity(
            meshId = "admin-mesh-1",
            publicKey = "admin_pubkey",
            displayName = "Hostel Admin"
        )
        every { identityManager.getOrCreateIdentity() } returns adminIdentity
        every { identityManager.signIdentityPayload(any()) } returns "admin_sig_123"

        invitationManager = CommunityInvitationManager(
            identityManager = identityManager,
            trustStore = trustStore,
            membershipManager = membershipManager
        )
    }

    @Test
    fun testCreateAndSerializeInvitation() {
        val invitation = invitationManager.createSignedInvitation(
            communityId = "hostel-block-a",
            communityName = "Hostel Block A",
            targetMeshId = "student-mesh-99",
            role = CommunityRole.MEMBER
        )

        assertNotNull(invitation)
        assertEquals("hostel-block-a", invitation.communityId)
        assertEquals("student-mesh-99", invitation.targetMeshId)
        assertEquals("admin_sig_123", invitation.signature)

        val jsonStr = invitation.toJson()
        val deserialized = CommunityInvitation.fromJson(jsonStr)

        assertEquals(invitation.communityId, deserialized.communityId)
        assertEquals(invitation.targetMeshId, deserialized.targetMeshId)
        assertEquals(invitation.signature, deserialized.signature)
    }
}
