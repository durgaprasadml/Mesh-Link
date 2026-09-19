package com.meshlink.ui.chat

import com.meshlink.domain.model.DeliveryStatus
import com.meshlink.domain.model.Message
import com.meshlink.domain.model.MessageType
import com.meshlink.ui.components.chat.DeliveryUiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class LocationMessageCardTest {

    @Test
    fun outgoingLocationMessage_retainsAccurateDataAndSenderIdentity() {
        val lat = 12.779922
        val lng = 75.184170
        val timestamp = 1700000000000L
        val message = Message(
            messageId = "msg-loc-1",
            chatId = "peer-b",
            text = "📍 Location: $lat, $lng",
            senderId = "my-peer-id",
            timestamp = timestamp,
            isFromMe = true,
            status = DeliveryStatus.SENT,
            messageType = MessageType.LOCATION,
            latitude = lat,
            longitude = lng,
            batteryPercent = 47
        )

        assertTrue(message.isFromMe)
        assertEquals(lat, message.latitude!!, 0.000001)
        assertEquals(lng, message.longitude!!, 0.000001)
        assertEquals(47, message.batteryPercent)
        assertEquals(DeliveryStatus.SENT, message.status)

        val formattedLat = String.format(Locale.US, "%.6f", message.latitude)
        val formattedLng = String.format(Locale.US, "%.6f", message.longitude)
        assertEquals("12.779922", formattedLat)
        assertEquals("75.184170", formattedLng)
    }

    @Test
    fun incomingLocationMessage_retainsAccurateDataAndReceiverIdentity() {
        val lat = 12.779922
        val lng = 75.184170
        val timestamp = 1700000000000L
        val message = Message(
            messageId = "msg-loc-2",
            chatId = "peer-b",
            text = "📍 Location: $lat, $lng",
            senderId = "peer-b",
            timestamp = timestamp,
            isFromMe = false,
            status = DeliveryStatus.DELIVERED,
            messageType = MessageType.LOCATION,
            latitude = lat,
            longitude = lng,
            batteryPercent = 88
        )

        assertFalse(message.isFromMe)
        assertEquals(lat, message.latitude!!, 0.000001)
        assertEquals(lng, message.longitude!!, 0.000001)
        assertEquals(88, message.batteryPercent)
        assertEquals(DeliveryStatus.DELIVERED, message.status)
    }

    @Test
    fun deliveryUiState_mapsAllDomainStatusesCorrectly() {
        // Sending states
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(DeliveryStatus.PENDING))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(DeliveryStatus.QUEUED))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(DeliveryStatus.SENDING))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(DeliveryStatus.WAITING_FOR_ROUTE))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(DeliveryStatus.WAITING_FOR_ACK))
        assertEquals(DeliveryUiState.Sending, DeliveryUiState.fromDomain(DeliveryStatus.RETRYING))

        // Sent
        assertEquals(DeliveryUiState.Sent, DeliveryUiState.fromDomain(DeliveryStatus.SENT))

        // Delivered
        assertEquals(DeliveryUiState.Delivered, DeliveryUiState.fromDomain(DeliveryStatus.DELIVERED))
        assertEquals(DeliveryUiState.Delivered, DeliveryUiState.fromDomain(DeliveryStatus.RELAYED))

        // Seen
        assertEquals(DeliveryUiState.Seen, DeliveryUiState.fromDomain(DeliveryStatus.SEEN))

        // Failed states
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(DeliveryStatus.FAILED))
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(DeliveryStatus.PERMANENT_FAILURE))
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(DeliveryStatus.CANCELLED))
        assertEquals(DeliveryUiState.Failed, DeliveryUiState.fromDomain(DeliveryStatus.EXPIRED))
    }

    @Test
    fun coordinateFormatting_handlesNullAndDecimalPrecision() {
        val lat: Double? = null
        val lng: Double? = null
        val nullLatFormatted = lat?.let { String.format(Locale.US, "%.6f", it) } ?: "Unavailable"
        val nullLngFormatted = lng?.let { String.format(Locale.US, "%.6f", it) } ?: "Unavailable"
        assertEquals("Unavailable", nullLatFormatted)
        assertEquals("Unavailable", nullLngFormatted)

        val preciseLat = 37.4220656
        val preciseLng = -122.0840897
        val formattedLat = String.format(Locale.US, "%.6f", preciseLat)
        val formattedLng = String.format(Locale.US, "%.6f", preciseLng)
        assertEquals("37.422066", formattedLat)
        assertEquals("-122.084090", formattedLng)
    }
}
