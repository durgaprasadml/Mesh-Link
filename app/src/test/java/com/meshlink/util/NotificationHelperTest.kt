package com.meshlink.util

import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import com.meshlink.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class NotificationHelperTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        NotificationHelper.resetForTesting()
    }

    @After
    fun tearDown() {
        NotificationHelper.resetForTesting()
    }

    @Test
    fun test_showSosNotification_inBackground_createsChannelAndPostsNotification() {
        NotificationHelper.setAppForeground(false)

        val result = NotificationHelper.showSosNotification(
            context = context,
            sosEventId = "sos_test_001",
            senderId = "peer_alice",
            senderName = "Alice",
            alertText = "🚨 SOS EMERGENCY: Lat 12.34, Lng 56.78 — Battery: 75%",
            latitude = 12.34,
            longitude = 56.78,
            batteryPercent = 75
        )

        assertTrue("Expected notification to be posted when in background", result)

        val channel = requireNotNull(NotificationManagerCompat.from(context).getNotificationChannel(NotificationHelper.CHANNEL_ID_SOS))
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue(channel.shouldVibrate())
        assertArrayEquals(longArrayOf(0, 250), channel.vibrationPattern)

        val shadowNm = Shadows.shadowOf(notificationManager)
        assertEquals(1, shadowNm.allNotifications.size)
        val notification = shadowNm.allNotifications.first()

        assertEquals(NotificationHelper.CHANNEL_ID_SOS, notification.channelId)
        val extras = notification.extras
        assertEquals("🚨 Emergency SOS from Alice", extras.getString("android.title"))
        assertEquals("🚨 SOS EMERGENCY: Lat 12.34, Lng 56.78 — Battery: 75%", extras.getString("android.text"))
    }

    @Test
    fun test_showSosNotification_deduplication_suppressesDuplicateAlert() {
        NotificationHelper.setAppForeground(false)

        val firstResult = NotificationHelper.showSosNotification(
            context = context,
            sosEventId = "sos_test_dup",
            senderId = "peer_bob",
            senderName = "Bob",
            alertText = "Emergency 1"
        )
        assertTrue("First notification must be posted", firstResult)

        val secondResult = NotificationHelper.showSosNotification(
            context = context,
            sosEventId = "sos_test_dup",
            senderId = "peer_bob",
            senderName = "Bob",
            alertText = "Emergency 1 (Duplicate packet)"
        )
        assertFalse("Duplicate notification with same sosEventId must be suppressed", secondResult)

        val shadowNm = Shadows.shadowOf(notificationManager)
        assertEquals("Exactly one notification should have been posted", 1, shadowNm.allNotifications.size)
    }

    @Test
    fun test_showSosNotification_inForeground_differentChat_emitsInAppNotification() = runTest {
        NotificationHelper.setAppForeground(true)
        NotificationHelper.setCurrentChatId("peer_other")

        var received: InAppNotification? = null
        val job = launch(kotlinx.coroutines.Dispatchers.Unconfined) {
            NotificationHelper.inAppNotifications.collect {
                received = it
            }
        }

        val result = NotificationHelper.showSosNotification(
            context = context,
            sosEventId = "sos_fg_001",
            senderId = "peer_carol",
            senderName = "Carol",
            alertText = "Need rescue"
        )

        assertTrue(result)
        job.cancel()

        assertNotNull(received)
        assertEquals("🚨 Carol", received?.senderName)
        assertEquals("Need rescue", received?.message)

        val shadowNm = Shadows.shadowOf(notificationManager)
        assertEquals("System notification must NOT be posted when in foreground", 0, shadowNm.allNotifications.size)
    }

    @Test
    fun test_showSosNotification_inForeground_sameChat_suppressesNotification() {
        NotificationHelper.setAppForeground(true)
        NotificationHelper.setCurrentChatId("peer_carol")

        val result = NotificationHelper.showSosNotification(
            context = context,
            sosEventId = "sos_fg_002",
            senderId = "peer_carol",
            senderName = "Carol",
            alertText = "Need rescue"
        )

        assertFalse("When user is already viewing the same chat, notification must be suppressed", result)

        val shadowNm = Shadows.shadowOf(notificationManager)
        assertEquals(0, shadowNm.allNotifications.size)
    }

    @Test
    fun test_showSosNotification_tapIntentNavigatesToMainActivityWithExtras() {
        NotificationHelper.setAppForeground(false)

        NotificationHelper.showSosNotification(
            context = context,
            sosEventId = "sos_intent_001",
            senderId = "peer_dan",
            senderName = "Dan",
            alertText = "SOS Alert"
        )

        val shadowNm = Shadows.shadowOf(notificationManager)
        val notification = shadowNm.allNotifications.first()

        val pendingIntent = notification.contentIntent
        assertNotNull(pendingIntent)

        val shadowPi = Shadows.shadowOf(pendingIntent)
        val savedIntent = shadowPi.savedIntent

        assertEquals(MainActivity::class.java.name, savedIntent.component?.className)
        assertEquals("peer_dan", savedIntent.getStringExtra("address"))
        assertEquals("🚨 Dan", savedIntent.getStringExtra("name"))
    }

    @Test
    fun test_showSosNotification_permissionDenied_doesNotPostNotification() {
        NotificationHelper.setAppForeground(false)
        val app = ApplicationProvider.getApplicationContext<android.app.Application>()
        Shadows.shadowOf(app).denyPermissions(android.Manifest.permission.POST_NOTIFICATIONS)

        val result = NotificationHelper.showSosNotification(
            context = context,
            sosEventId = "sos_denied_001",
            senderId = "peer_dan",
            senderName = "Dan",
            alertText = "SOS Alert"
        )

        assertFalse("When permission is denied, notification must not be posted", result)
        val shadowNm = Shadows.shadowOf(notificationManager)
        assertEquals(0, shadowNm.allNotifications.size)
    }
}

