package com.meshlink.deployment

import android.content.Context
import com.meshlink.BuildConfig
import com.meshlink.common.logger.FirebaseCrashReporterImpl
import com.meshlink.common.logger.NoOpCrashReporter
import com.meshlink.common.logger.PrivacyLogInterceptor
import com.meshlink.common.logger.SelfHostedCrashReporter
import com.meshlink.security.SecurityHardening
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeploymentVerificationTest {

    @Test
    fun `verify build configuration constants`() {
        assertTrue("versionCode must be positive", BuildConfig.VERSION_CODE > 0)
        assertNotNull("versionName must not be null", BuildConfig.VERSION_NAME)
        assertEquals("applicationId must be com.meshlink", "com.meshlink", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun `verify security hardening checks run without throwing exceptions`() {
        val mockContext = mockk<Context>(relaxed = true)
        every { mockContext.applicationInfo } returns mockk(relaxed = true)
        every { mockContext.packageName } returns "com.meshlink"

        val report = SecurityHardening.performSecurityChecks(mockContext)
        assertNotNull("Security report must not be null", report)
        assertTrue("Integrity check should pass for valid package", report.isIntegrityValid)
    }

    @Test
    fun `verify crash reporter abstraction implementations`() {
        val noOp = NoOpCrashReporter()
        noOp.logBreadcrumb("Test breadcrumb")
        noOp.logNonFatal(RuntimeException("Non-fatal test"))
        noOp.logFatal(RuntimeException("Fatal test"))

        val mockContext = mockk<Context>(relaxed = true)
        val selfHosted = SelfHostedCrashReporter(mockContext)
        selfHosted.logBreadcrumb("Breadcrumb test")
        selfHosted.logNonFatal(RuntimeException("Self hosted non-fatal"))
        selfHosted.setUserId("user_123")

        val logs = selfHosted.getCrashLogs()
        assertTrue("Self hosted crash reporter must record logs", logs.isNotEmpty())
        assertEquals("Self hosted must store user id", "user_123", logs["USER_ID"])

        val firebase = FirebaseCrashReporterImpl(mockContext)
        firebase.logBreadcrumb("Firebase breadcrumb")
    }

    @Test
    fun `verify privacy log interceptor sanitizes sensitive data`() {
        val rawMessage = "Device MAC is 00:11:22:33:44:55 and IP is 192.168.1.50 with mesh_a1b2c3d4e5"
        val redacted = PrivacyLogInterceptor.redact(rawMessage)

        assertFalse("MAC address must be redacted", redacted.contains("00:11:22:33:44:55"))
        assertFalse("IP address must be redacted", redacted.contains("192.168.1.50"))
        assertTrue("MAC replacement tag must be present", redacted.contains("[REDACTED_MAC]"))
        assertTrue("IP replacement tag must be present", redacted.contains("[REDACTED_IP]"))

        val rawMetadata = mapOf(
            "authToken" to "secret_token_123",
            "deviceMac" to "AA:BB:CC:DD:EE:FF"
        )
        val redactedMetadata = PrivacyLogInterceptor.redactMetadata(rawMetadata)
        assertEquals("[REDACTED_SENSITIVE]", redactedMetadata["authToken"])
        assertEquals("[REDACTED_MAC]", redactedMetadata["deviceMac"])
    }
}
