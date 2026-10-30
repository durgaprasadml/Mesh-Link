package com.meshlink.ui.chat

import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.ui.components.chat.resolveEmergencyMessage
import com.meshlink.ui.components.chat.resolveSosSenderDisplayName
import com.meshlink.ui.components.chat.resolveSosSenderVerification
import com.meshlink.ui.util.DateTimeUtils
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale

class SosEmergencyCardTest {

    @Test
    fun resolveEmergencyMessage_displaysCustomMessageProminently() {
        val customText = "Trapped in building following collapse, 2 injured"
        val resolved = resolveEmergencyMessage(customText)
        assertEquals("Trapped in building following collapse, 2 injured", resolved)
    }

    @Test
    fun resolveEmergencyMessage_extractsCustomMessageFromDelimitedText() {
        val compositeText = "🚨 SOS EMERGENCY from Alice — Severe asthma attack, inhaler empty — Lat: 12.794620, Lng: 75.174420 — Battery: 45%"
        val resolved = resolveEmergencyMessage(compositeText)
        assertEquals("Severe asthma attack, inhaler empty", resolved)
    }

    @Test
    fun resolveEmergencyMessage_fallsBackToDefaultWhenNoCustomMessage() {
        // Standard boilerplate without custom text
        val standardText = "🚨 SOS EMERGENCY from Bob — Lat: 12.794620, Lng: 75.174420 — Battery: 88%"
        assertEquals("Emergency assistance requested", resolveEmergencyMessage(standardText))

        // Empty string
        assertEquals("Emergency assistance requested", resolveEmergencyMessage(""))

        // Plain SOS keyword
        assertEquals("Emergency assistance requested", resolveEmergencyMessage("SOS"))
        assertEquals("Emergency assistance requested", resolveEmergencyMessage("  sos  "))
    }

    @Test
    fun resolveEmergencyMessage_handlesAnnouncementStageProperly() {
        val announcementText = "🚨 SOS EMERGENCY from Charlie (Unverified Sender) — Direct Range Alert Announced. Awaiting full details via Wi-Fi Direct."
        val resolved = resolveEmergencyMessage(announcementText)
        assertEquals("Emergency assistance requested (Awaiting full details via Wi-Fi Direct)", resolved)
    }

    @Test
    fun resolveSosSenderDisplayName_identifiesLocalAndRemoteSenders() {
        // Local user
        val myMessage = Message(
            messageId = "sos-me",
            chatId = "peer-a",
            text = "🚨 SOS EMERGENCY (Dispatched) — Lat: 12.0, Lng: 75.0 — Battery: 90%",
            senderId = "my-mesh-id",
            timestamp = 1700000000000L,
            isFromMe = true,
            status = DeliveryStatus.SENT,
            messageType = MessageType.SOS
        )
        assertEquals("You", resolveSosSenderDisplayName(myMessage, "Alice"))

        // Remote peer with explicit display name
        val peerMessage = myMessage.copy(
            isFromMe = false,
            senderId = "peer-mesh-id"
        )
        assertEquals("Alice", resolveSosSenderDisplayName(peerMessage, "🚨 Alice"))

        // Extracted from message text
        val textMessage = peerMessage.copy(
            text = "🚨 SOS EMERGENCY from Bob — Lat: 12.0, Lng: 75.0"
        )
        assertEquals("Bob", resolveSosSenderDisplayName(textMessage, null))

        // Extracted from unverified announcement
        val unverifiedMsg = peerMessage.copy(
            text = "🚨 SOS EMERGENCY from Dr. Watson (Unverified Sender) — Direct Range Alert Announced."
        )
        assertEquals("Dr. Watson", resolveSosSenderDisplayName(unverifiedMsg, null))
    }

    @Test
    fun resolveSosSenderVerification_neverFalselyVerifiesUnverifiedIdentity() {
        // Local user is inherently verified
        val myMessage = Message(
            messageId = "sos-1",
            chatId = "peer-1",
            text = "SOS",
            senderId = "me",
            timestamp = 1700000000000L,
            isFromMe = true,
            status = DeliveryStatus.SENT,
            messageType = MessageType.SOS
        )
        assertTrue(resolveSosSenderVerification(myMessage, null))

        // Incoming message with explicit verified parameter
        val incomingMsg = myMessage.copy(isFromMe = false)
        assertTrue(resolveSosSenderVerification(incomingMsg, isSenderVerified = true))

        // Incoming message with explicit unverified parameter
        assertFalse(resolveSosSenderVerification(incomingMsg, isSenderVerified = false))

        // Text contains unverified notice
        val unverifiedTextMsg = incomingMsg.copy(
            text = "🚨 SOS EMERGENCY from Mallory (Unverified Sender) — Direct Range Alert Announced."
        )
        assertFalse(resolveSosSenderVerification(unverifiedTextMsg, null))

        // Text without verification confirmation defaults to unverified for security
        val unconfirmedMsg = incomingMsg.copy(text = "SOS")
        assertFalse("Unconfirmed cryptographic identity must not be marked verified",
            resolveSosSenderVerification(unconfirmedMsg, null))
    }

    @Test
    fun locationRendering_displaysPreciseCoordinatesOrHonestUnavailable() {
        val lat = 12.794620
        val lng = 75.174420
        val withLocation = Message(
            messageId = "sos-loc",
            chatId = "peer-1",
            text = "SOS",
            senderId = "peer-1",
            timestamp = 1700000000000L,
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            latitude = lat,
            longitude = lng
        )

        assertNotNull(withLocation.latitude)
        assertNotNull(withLocation.longitude)
        assertEquals("12.794620", String.format(Locale.US, "%.6f", withLocation.latitude))
        assertEquals("75.174420", String.format(Locale.US, "%.6f", withLocation.longitude))

        // Unavailable location
        val withoutLocation = withLocation.copy(latitude = null, longitude = null)
        assertNull(withoutLocation.latitude)
        assertNull(withoutLocation.longitude)
    }

    @Test
    fun batteryRendering_displaysAccuratePercentageOrUnavailable() {
        val withBattery = Message(
            messageId = "sos-bat",
            chatId = "peer-1",
            text = "SOS",
            senderId = "peer-1",
            timestamp = 1700000000000L,
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            batteryPercent = 74
        )
        assertEquals(74, withBattery.batteryPercent)
        assertEquals("74%", "${withBattery.batteryPercent}%")

        val withoutBattery = withBattery.copy(batteryPercent = null)
        assertNull(withoutBattery.batteryPercent)
        val negativeBattery = withBattery.copy(batteryPercent = -1)
        assertTrue(negativeBattery.batteryPercent!! < 0)
    }

    @Test
    fun emergencyPhotos_parsesDualCameraJsonAndIdentifiesAvailability() {
        val tempFront = File.createTempFile("front_test", ".jpg").apply { deleteOnExit() }
        val tempRear = File.createTempFile("rear_test", ".jpg").apply { deleteOnExit() }

        val json = JSONObject().apply {
            put("front", tempFront.absolutePath)
            put("rear", tempRear.absolutePath)
        }.toString()

        val messageWithBoth = Message(
            messageId = "sos-img-both",
            chatId = "peer-1",
            text = "SOS",
            senderId = "peer-1",
            timestamp = 1700000000000L,
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            mediaPath = json
        )

        val parsedObj = JSONObject(messageWithBoth.mediaPath!!)
        val frontPath = parsedObj.optString("front")
        val rearPath = parsedObj.optString("rear")

        assertTrue(File(frontPath).exists())
        assertTrue(File(rearPath).exists())

        // Single photo missing does not block the other
        val missingRearJson = JSONObject().apply {
            put("front", tempFront.absolutePath)
            put("rear", "/non/existent/path/rear.jpg")
        }.toString()

        val parsedMissingRear = JSONObject(missingRearJson)
        assertTrue(File(parsedMissingRear.optString("front")).exists())
        assertFalse(File(parsedMissingRear.optString("rear")).exists())
    }

    @Test
    fun timestamp_usesActualStoredTimestampWithoutGeneratingNew() {
        val storedTs = 1698700000000L // Historical timestamp
        val message = Message(
            messageId = "sos-ts",
            chatId = "peer-1",
            text = "SOS",
            senderId = "peer-1",
            timestamp = storedTs,
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS
        )

        assertEquals(storedTs, message.timestamp)
        val formatted = DateTimeUtils.formatTime12Hour(message.timestamp)
        assertNotNull(formatted)
        assertTrue(formatted.isNotBlank())
    }

    @Test
    fun packageLifecycle_updatesExistingSosCardStateWhenFullPackageArrives() {
        // Stage A: Announcement arrival
        val stageAMessage = Message(
            messageId = "sos-lifecycle-1",
            chatId = "peer-x",
            text = "🚨 SOS EMERGENCY from PeerX (Unverified Sender) — Direct Range Alert Announced. Awaiting full details via Wi-Fi Direct.",
            senderId = "peer-x",
            timestamp = 1700000000000L,
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.SOS,
            latitude = null,
            longitude = null,
            batteryPercent = null,
            mediaPath = null
        )

        // Stage A verification
        assertNull(stageAMessage.latitude)
        assertNull(stageAMessage.longitude)
        assertNull(stageAMessage.batteryPercent)
        assertNull(stageAMessage.mediaPath)
        assertEquals("Emergency assistance requested (Awaiting full details via Wi-Fi Direct)",
            resolveEmergencyMessage(stageAMessage.text))

        // Stage B: Full Wi-Fi Direct package arrives updating the same message ID
        val stageBMessage = stageAMessage.copy(
            text = "🚨 SOS EMERGENCY from PeerX — Lat: 12.794620, Lng: 75.174420 — Battery: 65%",
            latitude = 12.794620,
            longitude = 75.174420,
            batteryPercent = 65,
            mediaPath = JSONObject().apply {
                put("front", "/tmp/front.jpg")
                put("rear", "/tmp/rear.jpg")
            }.toString()
        )

        // Stage B verification: same messageId, full data present
        assertEquals(stageAMessage.messageId, stageBMessage.messageId)
        assertEquals(12.794620, stageBMessage.latitude!!, 0.00001)
        assertEquals(75.174420, stageBMessage.longitude!!, 0.00001)
        assertEquals(65, stageBMessage.batteryPercent)
        assertNotNull(stageBMessage.mediaPath)
        assertEquals("Emergency assistance requested", resolveEmergencyMessage(stageBMessage.text))
    }
}

