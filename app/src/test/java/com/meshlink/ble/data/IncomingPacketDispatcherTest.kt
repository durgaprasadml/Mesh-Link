package com.meshlink.ble.data

import com.meshlink.ble.data.handlers.AckManager
import com.meshlink.ble.data.handlers.BeaconHandler
import com.meshlink.ble.data.handlers.BroadcastHandler
import com.meshlink.ble.data.handlers.KeyExchangeHandler
import com.meshlink.ble.data.handlers.LocationMessageHandler
import com.meshlink.ble.data.handlers.MediaMessageHandler
import com.meshlink.ble.data.handlers.TextMessageHandler
import com.meshlink.ble.data.handlers.VoiceMessageHandler
import com.meshlink.domain.model.MeshPacket
import com.meshlink.domain.model.PacketType
import com.meshlink.domain.model.User
import com.meshlink.domain.repository.SettingsRepository
import com.meshlink.domain.repository.UserRepository
import com.meshlink.profile.ProfileSyncManager
import com.meshlink.security.data.MeshCryptoManager
import com.meshlink.security.data.RekeyManager
import com.meshlink.security.data.SessionManager
import com.meshlink.security.data.TrustManager
import com.meshlink.transfer.TransferManager
import com.meshlink.voice.transport.VoiceTransport
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import javax.inject.Provider

class IncomingPacketDispatcherTest {

    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val cryptoManager = mockk<MeshCryptoManager>(relaxed = true)
    private val sessionManager = mockk<SessionManager>(relaxed = true)
    private val trustManager = mockk<TrustManager>(relaxed = true)
    private val rekeyManager = mockk<RekeyManager>(relaxed = true)
    private val transferManager = mockk<TransferManager>(relaxed = true)
    private val voiceTransport = mockk<VoiceTransport>(relaxed = true)
    private val keyExchangeHandler = mockk<KeyExchangeHandler>(relaxed = true)
    private val textMessageHandler = mockk<TextMessageHandler>(relaxed = true)
    private val mediaMessageHandler = mockk<MediaMessageHandler>(relaxed = true)
    private val locationMessageHandler = mockk<LocationMessageHandler>(relaxed = true)
    private val voiceMessageHandler = mockk<VoiceMessageHandler>(relaxed = true)
    private val broadcastHandler = mockk<BroadcastHandler>(relaxed = true)
    private val ackManager = mockk<AckManager>(relaxed = true)
    private val beaconHandler = mockk<BeaconHandler>(relaxed = true)
    private val profileSyncManager = mockk<ProfileSyncManager>(relaxed = true)
    private val profileSyncManagerProvider = Provider { profileSyncManager }

    private lateinit var dispatcher: IncomingPacketDispatcher

    private val myMeshId = "00:11:22:33:44:55"
    private val senderId = "aa:bb:cc:dd:ee:ff"

    @Before
    fun setup() {
        coEvery { userRepository.getLocalUser() } returns User(
            meshId = myMeshId,
            name = "Local User"
        )
        coEvery { settingsRepository.advancedEncryptionEnforcement } returns flowOf(false)
        coEvery { sessionManager.getSession(any()) } returns null

        dispatcher = IncomingPacketDispatcher(
            userRepository = userRepository,
            settingsRepository = settingsRepository,
            cryptoManager = cryptoManager,
            sessionManager = sessionManager,
            trustManager = trustManager,
            rekeyManager = rekeyManager,
            transferManager = transferManager,
            voiceTransport = voiceTransport,
            keyExchangeHandler = keyExchangeHandler,
            textMessageHandler = textMessageHandler,
            mediaMessageHandler = mediaMessageHandler,
            locationMessageHandler = locationMessageHandler,
            voiceMessageHandler = voiceMessageHandler,
            broadcastHandler = broadcastHandler,
            ackManager = ackManager,
            beaconHandler = beaconHandler,
            profileSyncManagerProvider = profileSyncManagerProvider
        )
    }

    // ────────── Test A: Valid encrypted message ──────────
    @Test
    fun `Test A - Valid encrypted message is decrypted, increases trust, and dispatches to handler`() = runTest {
        val ciphertext = "valid_base64_ciphertext"
        val expectedPlaintext = """{"text":"Hello World","senderName":"Alice"}"""

        coEvery { cryptoManager.decrypt(ciphertext, senderId, null, false) } returns expectedPlaintext

        val packet = MeshPacket(
            packetId = "pkt-1",
            senderId = senderId,
            targetId = myMeshId,
            payload = ciphertext,
            type = PacketType.TEXT,
            encrypted = true
        )

        dispatcher.dispatch(packet)

        // Verifies decryption succeeded and plaintext was passed to handler
        coVerify(exactly = 1) {
            textMessageHandler.receiveMessage(match { it.payload == expectedPlaintext })
        }
        // Verifies trust score incremented
        verify(exactly = 1) {
            trustManager.increaseTrustScore(senderId, 1)
        }
    }

    // ────────── Test B: Invalid AES-GCM authentication tag ──────────
    @Test
    fun `Test B - Invalid AES-GCM authentication tag drops packet without dispatch or trust gain`() = runTest {
        val corruptedCiphertext = "corrupted_base64_ciphertext"

        // Simulating AEADBadTagException / tag mismatch leading to null
        coEvery { cryptoManager.decrypt(corruptedCiphertext, senderId, any(), any()) } returns null

        val packet = MeshPacket(
            packetId = "pkt-tampered",
            senderId = senderId,
            targetId = myMeshId,
            payload = corruptedCiphertext,
            type = PacketType.TEXT,
            encrypted = true
        )

        dispatcher.dispatch(packet)

        // Must NOT dispatch to any message handler
        coVerify(exactly = 0) { textMessageHandler.receiveMessage(any()) }
        coVerify(exactly = 0) { broadcastHandler.receiveBroadcastTextMessage(any()) }
        coVerify(exactly = 0) { transferManager.handleIncomingPacket(any()) }
        // Must NOT increase trust score
        verify(exactly = 0) { trustManager.increaseTrustScore(any(), any()) }
    }

    // ────────── Test C: Plaintext JSON marked as encrypted (ML-AUDIT-001 exploit) ──────────
    @Test
    fun `Test C - Plaintext JSON marked as encrypted is rejected and cannot bypass authentication`() = runTest {
        val forgedPlaintextJson = """{"text":"Forged Message","senderName":"Attacker"}"""

        // Since it's plaintext JSON and not valid ciphertext, decryption fails and returns null
        coEvery { cryptoManager.decrypt(forgedPlaintextJson, senderId, any(), any()) } returns null

        val packet = MeshPacket(
            packetId = "pkt-exploit",
            senderId = senderId,
            targetId = myMeshId,
            payload = forgedPlaintextJson,
            type = PacketType.TEXT,
            encrypted = true // Attacker marks as encrypted to attempt bypass
        )

        dispatcher.dispatch(packet)

        // The exploit relied on !finalPayload.startsWith("{") failing to drop.
        // With the fix, it MUST be dropped.
        coVerify(exactly = 0) { textMessageHandler.receiveMessage(any()) }
        verify(exactly = 0) { trustManager.increaseTrustScore(any(), any()) }
    }

    // ────────── Test D: Decryption failure followed by JSON parsing ──────────
    @Test
    fun `Test D - Decryption failure on JSON-like payload drops packet before JSON parsing`() = runTest {
        val jsonPayload = """{"type":"TEXT","content":"Some JSON"}"""

        coEvery { cryptoManager.decrypt(jsonPayload, senderId, any(), any()) } returns null

        val packet = MeshPacket(
            packetId = "pkt-json-fail",
            senderId = senderId,
            targetId = myMeshId,
            payload = jsonPayload,
            type = PacketType.TEXT,
            encrypted = true
        )

        dispatcher.dispatch(packet)

        coVerify(exactly = 0) { textMessageHandler.receiveMessage(any()) }
        verify(exactly = 0) { trustManager.increaseTrustScore(any(), any()) }
    }

    // ────────── Test E: Missing key or encryption metadata ──────────
    @Test
    fun `Test E - Missing peer key causes decryption failure and packet drop`() = runTest {
        val ciphertext = "some_ciphertext"

        // When peer key is missing, decrypt returns null
        coEvery { cryptoManager.decrypt(ciphertext, senderId, any(), any()) } returns null

        val packet = MeshPacket(
            packetId = "pkt-no-key",
            senderId = senderId,
            targetId = myMeshId,
            payload = ciphertext,
            type = PacketType.TEXT,
            encrypted = true
        )

        dispatcher.dispatch(packet)

        coVerify(exactly = 0) { textMessageHandler.receiveMessage(any()) }
        verify(exactly = 0) { trustManager.increaseTrustScore(any(), any()) }
    }

    // ────────── Test F: Legitimate plaintext mode (broadcast & bootstrap) ──────────
    @Test
    fun `Test F1 - Legitimate plaintext broadcast message is allowed by protocol`() = runTest {
        val broadcastPayload = """{"text":"Emergency Announcement","senderName":"Chief"}"""

        val broadcastPacket = MeshPacket(
            packetId = "pkt-bcast",
            senderId = senderId,
            targetId = "BROADCAST",
            payload = broadcastPayload,
            type = PacketType.TEXT,
            encrypted = false
        )

        dispatcher.dispatch(broadcastPacket)

        // Broadcast handler receives the broadcast message
        coVerify(exactly = 1) {
            broadcastHandler.receiveBroadcastTextMessage(broadcastPacket)
        }
        // Direct text message handler should not receive it
        coVerify(exactly = 0) { textMessageHandler.receiveMessage(any()) }
    }

    @Test
    fun `Test F2 - Direct 1-to-1 plaintext text message is rejected by central encryption policy`() = runTest {
        val directPlaintext = """{"text":"Secret direct message"}"""

        val packet = MeshPacket(
            packetId = "pkt-direct-unenc",
            senderId = senderId,
            targetId = myMeshId,
            payload = directPlaintext,
            type = PacketType.TEXT,
            encrypted = false // 1-to-1 TEXT requires encryption
        )

        dispatcher.dispatch(packet)

        // Central policy drops plaintext 1-to-1 TEXT
        coVerify(exactly = 0) { textMessageHandler.receiveMessage(any()) }
        coVerify(exactly = 0) { broadcastHandler.receiveBroadcastTextMessage(any()) }
        verify(exactly = 0) { trustManager.increaseTrustScore(any(), any()) }
    }

    // ────────── Test G: No trust or persistence side effects on rejection ──────────
    @Test
    fun `Test G - Dropped packets trigger zero side effects across all handlers and trust manager`() = runTest {
        val badPayload = "malformed_garbage"
        coEvery { cryptoManager.decrypt(badPayload, senderId, any(), any()) } returns null

        val packet = MeshPacket(
            packetId = "pkt-bad",
            senderId = senderId,
            targetId = myMeshId,
            payload = badPayload,
            type = PacketType.TEXT,
            encrypted = true
        )

        dispatcher.dispatch(packet)

        verify(exactly = 0) { trustManager.increaseTrustScore(any(), any()) }
        coVerify(exactly = 0) { textMessageHandler.receiveMessage(any()) }
        coVerify(exactly = 0) { mediaMessageHandler.insertPlaceholderIncomingMedia(any()) }
        coVerify(exactly = 0) { locationMessageHandler.receiveLocationMessage(any()) }
        coVerify(exactly = 0) { ackManager.handleDeliveryAck(any()) }
        coVerify(exactly = 0) { ackManager.handleReadReceipt(any()) }
    }

    // ────────── Test H: Handshake/control and encrypted SOS/Media packets ──────────
    @Test
    fun `Test H1 - Legitimate handshake and control packets still work`() = runTest {
        val keyExchangePacket = MeshPacket(
            packetId = "pkt-kex",
            senderId = senderId,
            targetId = myMeshId,
            payload = "v3|2|3|1|features|pubkey|1000|nonce|sig|signkey|false",
            type = PacketType.KEY_EXCHANGE,
            encrypted = false
        )
        dispatcher.dispatch(keyExchangePacket)
        coVerify(exactly = 1) { keyExchangeHandler.handleKeyExchange(keyExchangePacket) }

        val beaconPacket = MeshPacket(
            packetId = "pkt-beacon",
            senderId = senderId,
            targetId = "BROADCAST",
            payload = """{"meshId":"aa:bb:cc:dd:ee:ff"}""",
            type = PacketType.BEACON,
            encrypted = false
        )
        dispatcher.dispatch(beaconPacket)
        coVerify(exactly = 1) { beaconHandler.handleBeaconPacket(beaconPacket) }
    }

    @Test
    fun `Test H2 - Valid encrypted SOS and media packets follow expected processing paths`() = runTest {
        val sosCiphertext = "encrypted_sos"
        val sosPlaintext = """{"sosId":"sos-1","lat":12.34,"lng":56.78}"""
        coEvery { cryptoManager.decrypt(sosCiphertext, senderId, null, false) } returns sosPlaintext

        val sosPacket = MeshPacket(
            packetId = "pkt-sos",
            senderId = senderId,
            targetId = "BROADCAST",
            payload = sosCiphertext,
            type = PacketType.SOS,
            encrypted = true
        )
        dispatcher.dispatch(sosPacket)
        coVerify(exactly = 1) {
            broadcastHandler.receiveSosMessage(match { it.payload == sosPlaintext })
        }

        val mediaCiphertext = "encrypted_media_meta"
        val mediaPlaintext = """{"fileName":"photo.jpg","fileSize":1024}"""
        coEvery { cryptoManager.decrypt(mediaCiphertext, senderId, null, false) } returns mediaPlaintext

        val mediaPacket = MeshPacket(
            packetId = "pkt-media",
            senderId = senderId,
            targetId = myMeshId,
            payload = mediaCiphertext,
            type = PacketType.MEDIA_META,
            transferId = "tx-1",
            encrypted = true
        )
        dispatcher.dispatch(mediaPacket)
        coVerify(exactly = 1) {
            mediaMessageHandler.insertPlaceholderIncomingMedia(match { it.payload == mediaPlaintext })
        }
        coVerify(exactly = 1) {
            transferManager.handleIncomingPacket(match { it.payload == mediaPlaintext })
        }
    }

    // ────────── Test I: Session state commit only after authenticated decryption (ML-AUDIT-002) ──────────
    @Test
    fun `Test I - v2 packet commits session state only when AES-GCM decryption succeeds`() = runTest {
        val aadBytes = """{"sid":"s1","seq":1,"ts":1000,"kv":2}""".toByteArray()
        val wrappedInvalid = "v2|dGVzdA==|tampered_cipher"
        val wrappedValid = "v2|dGVzdA==|valid_cipher"
        val decryptedPlaintext = """{"text":"Authenticated","senderName":"Alice"}"""

        coEvery { sessionManager.validateAndUnwrap(senderId, wrappedInvalid) } returns Triple(aadBytes, "tampered_cipher", 2)
        coEvery { cryptoManager.decrypt("tampered_cipher", senderId, aadBytes, false) } returns null

        val invalidPacket = MeshPacket(
            packetId = "pkt-v2-bad",
            senderId = senderId,
            targetId = myMeshId,
            payload = wrappedInvalid,
            type = PacketType.TEXT,
            encrypted = true
        )
        dispatcher.dispatch(invalidPacket)

        // Failed decryption MUST NOT commit session state
        verify(exactly = 0) { sessionManager.commitAuthenticatedPacket(any(), any()) }
        coVerify(exactly = 0) { textMessageHandler.receiveMessage(any()) }

        // Now test valid decryption
        coEvery { sessionManager.validateAndUnwrap(senderId, wrappedValid) } returns Triple(aadBytes, "valid_cipher", 2)
        coEvery { cryptoManager.decrypt("valid_cipher", senderId, aadBytes, false) } returns decryptedPlaintext

        val validPacket = MeshPacket(
            packetId = "pkt-v2-ok",
            senderId = senderId,
            targetId = myMeshId,
            payload = wrappedValid,
            type = PacketType.TEXT,
            encrypted = true
        )
        dispatcher.dispatch(validPacket)

        verify(exactly = 1) { sessionManager.commitAuthenticatedPacket(senderId, aadBytes) }
        coVerify(exactly = 1) { textMessageHandler.receiveMessage(match { it.payload == decryptedPlaintext }) }
    }

    // ────────── Test J: Video & Emergency/Incident packet routing (ML-AUDIT-011) ──────────
    @Test
    fun `Test J - Video and Incident packets route to their domain handlers when authenticated`() = runTest {
        val videoTransport = mockk<com.meshlink.video.transport.VideoTransport>(relaxed = true)
        val incidentManager = mockk<com.meshlink.emergency.incident.IncidentManager>(relaxed = true)
        val emergencyFormSync = mockk<com.meshlink.emergency.incident.EmergencyFormSync>(relaxed = true)
        val resourceManager = mockk<com.meshlink.emergency.logistics.ResourceManager>(relaxed = true)
        val teamTracker = mockk<com.meshlink.emergency.rescue.TeamTracker>(relaxed = true)

        val fullDispatcher = IncomingPacketDispatcher(
            userRepository = userRepository,
            settingsRepository = settingsRepository,
            cryptoManager = cryptoManager,
            sessionManager = sessionManager,
            trustManager = trustManager,
            rekeyManager = rekeyManager,
            transferManager = transferManager,
            voiceTransport = voiceTransport,
            keyExchangeHandler = keyExchangeHandler,
            textMessageHandler = textMessageHandler,
            mediaMessageHandler = mediaMessageHandler,
            locationMessageHandler = locationMessageHandler,
            voiceMessageHandler = voiceMessageHandler,
            broadcastHandler = broadcastHandler,
            ackManager = ackManager,
            beaconHandler = beaconHandler,
            profileSyncManagerProvider = profileSyncManagerProvider,
            videoTransport = videoTransport,
            incidentManager = incidentManager,
            emergencyFormSync = emergencyFormSync,
            resourceManager = resourceManager,
            teamTracker = teamTracker
        )

        val videoSignalPlain = """{"sdp":"offer"}"""
        coEvery { cryptoManager.decrypt("enc_video_sig", senderId, null, false) } returns videoSignalPlain
        fullDispatcher.dispatch(
            MeshPacket(
                packetId = "pkt-vid-1",
                senderId = senderId,
                targetId = myMeshId,
                payload = "enc_video_sig",
                type = PacketType.VIDEO_SIGNAL,
                encrypted = true
            )
        )
        verify(exactly = 1) { videoTransport.handleIncomingPacket(match { it.payload == videoSignalPlain }) }

        val incidentPlain = """{"id":"inc-1","type":"FLOOD","severity":4,"locationDescription":"Bridge","commanderId":"cmd-1","status":"ACTIVE"}"""
        coEvery { cryptoManager.decrypt("enc_inc", senderId, null, false) } returns incidentPlain
        fullDispatcher.dispatch(
            MeshPacket(
                packetId = "pkt-inc-1",
                senderId = senderId,
                targetId = myMeshId,
                payload = "enc_inc",
                type = PacketType.INCIDENT_REPORT,
                encrypted = true
            )
        )
        verify(exactly = 1) { incidentManager.syncIncomingIncident(match { it.id == "inc-1" && it.severity == 4 }) }

        val checkInPlain = """{"id":"m-1","name":"Rohan","role":"MEDIC","status":"SAFE","batteryPct":85}"""
        coEvery { cryptoManager.decrypt("enc_chk", senderId, null, false) } returns checkInPlain
        fullDispatcher.dispatch(
            MeshPacket(
                packetId = "pkt-chk-1",
                senderId = senderId,
                targetId = myMeshId,
                payload = "enc_chk",
                type = PacketType.CHECK_IN,
                encrypted = true
            )
        )
        verify(exactly = 1) {
            teamTracker.processCheckIn(
                "m-1",
                "Rohan",
                com.meshlink.emergency.rescue.TeamRole.MEDIC,
                com.meshlink.emergency.rescue.TeamMemberStatus.SAFE,
                85
            )
        }

        val formPlain = """{"formId":"f-1","type":"SITREP","authorId":"a-1","jsonData":"{}","timestamp":1000}"""
        coEvery { cryptoManager.decrypt("enc_form", senderId, null, false) } returns formPlain
        fullDispatcher.dispatch(
            MeshPacket(
                packetId = "pkt-form-1",
                senderId = senderId,
                targetId = myMeshId,
                payload = "enc_form",
                type = PacketType.FORM_SYNC,
                encrypted = true
            )
        )
        verify(exactly = 1) { emergencyFormSync.receiveSync(formPlain) }

        val resourcePlain = """{"id":"res-1","category":"WATER","name":"Bottled Water","quantity":50,"location":"Camp A","ownerId":"o-1"}"""
        coEvery { cryptoManager.decrypt("enc_res", senderId, null, false) } returns resourcePlain
        fullDispatcher.dispatch(
            MeshPacket(
                packetId = "pkt-res-1",
                senderId = senderId,
                targetId = myMeshId,
                payload = "enc_res",
                type = PacketType.RESOURCE_SYNC,
                encrypted = true
            )
        )
        verify(exactly = 1) { resourceManager.syncResource(match { it.id == "res-1" && it.quantity == 50 }) }
    }
}
