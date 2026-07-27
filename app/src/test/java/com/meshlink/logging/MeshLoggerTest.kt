package com.meshlink.logging

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.CopyOnWriteArrayList

class MeshLoggerTest {

    private lateinit var testSink: TestLogSink

    class TestLogSink : LogSink {
        val events = CopyOnWriteArrayList<LogEvent>()
        override fun log(event: LogEvent) {
            events.add(event)
        }
    }

    @Before
    fun setup() {
        testSink = TestLogSink()
        MeshLogger.clearSinks()
        MeshLogger.addSink(testSink)
        MeshLogger.config = LoggerConfig(
            minLevel = LogLevel.VERBOSE,
            enabledCategories = LogCategory.values().toSet(),
            isEnabled = true
        )
    }

    @After
    fun teardown() {
        MeshLogger.clearSinks()
    }

    @Test
    fun `test basic logging and category filtering`() {
        MeshLogger.config = LoggerConfig(
            minLevel = LogLevel.DEBUG,
            enabledCategories = setOf(LogCategory.ROUTING)
        )

        MeshLogger.v(LogCategory.ROUTING) { "Verbose message" }
        MeshLogger.d(LogCategory.ROUTING) { "Debug message" }
        MeshLogger.i(LogCategory.BLE) { "Info message BLE" }

        assertEquals(1, testSink.events.size)
        val event = testSink.events[0]
        assertEquals(LogLevel.DEBUG, event.level)
        assertEquals(LogCategory.ROUTING, event.category)
        assertEquals("Debug message", event.message)
    }

    @Test
    fun `test context propagation`() {
        val ctx = LogContext.EMPTY
            .withNode("NodeA")
            .withPeer("PeerB")
            .withPacket("Pack123")

        MeshLogger.i(LogCategory.TRANSPORT, context = ctx) { "Contextual log" }

        assertEquals(1, testSink.events.size)
        val event = testSink.events[0]
        assertEquals("NodeA", event.context.nodeId)
        assertEquals("PeerB", event.context.peerId)
        assertEquals("Pack123", event.context.packetId)
    }

    @Test
    fun `test pretty formatter output`() {
        val formatter = PrettyFormatter()
        val event = LogEvent(
            timestamp = 1700000000000L,
            level = LogLevel.ERROR,
            category = LogCategory.SECURITY,
            message = LogEventNames.SECURITY_HANDSHAKE_FAILED,
            context = LogContext.EMPTY.withSession("Sess1"),
            threadName = "TestThread",
            exception = RuntimeException("Auth failed"),
            metadata = mapOf("reason" to "timeout")
        )

        val output = formatter.format(event)
        assertTrue(output.contains("[SECURITY]"))
        assertTrue(output.contains("[TestThread]"))
        assertTrue(output.contains("{session=Sess1}"))
        assertTrue(output.contains("- SECURITY_HANDSHAKE_FAILED"))
        assertTrue(output.contains("metadata=[reason=timeout]"))
        assertTrue(output.contains("Exception: java.lang.RuntimeException: Auth failed"))
    }

    @Test
    fun `test json formatter output`() {
        val formatter = JsonFormatter()
        val event = LogEvent(
            timestamp = 1700000000000L,
            level = LogLevel.WARN,
            category = LogCategory.MESSAGING,
            message = "Message dropped",
            context = LogContext.EMPTY.withTrace("Trace99"),
            threadName = "main",
            exception = null,
            metadata = mapOf("retryCount" to 3)
        )

        val output = formatter.format(event)
        assertTrue(output.contains("\"level\":\"WARN\""))
        assertTrue(output.contains("\"category\":\"MESSAGING\""))
        assertTrue(output.contains("\"message\":\"Message dropped\""))
        assertTrue(output.contains("\"traceId\":\"Trace99\""))
        assertTrue(output.contains("\"retryCount\":3"))
    }

    @Test
    fun `test concurrent logging`() = runBlocking(Dispatchers.Default) {
        val threadCount = 100
        val logsPerThread = 100

        val jobs = List(threadCount) {
            launch {
                repeat(logsPerThread) {
                    MeshLogger.d(LogCategory.SYSTEM) { "Concurrent log $it" }
                }
            }
        }
        
        jobs.forEach { it.join() }

        assertEquals(threadCount * logsPerThread, testSink.events.size)
    }
    
    @Test
    fun `test lazy evaluation`() {
        MeshLogger.config = LoggerConfig(isEnabled = false)
        var messageEvaluated = false
        var metadataEvaluated = false
        
        MeshLogger.d(
            category = LogCategory.SYSTEM,
            metadata = { 
                metadataEvaluated = true
                mapOf("key" to "val") 
            }
        ) {
            messageEvaluated = true
            "Should not be evaluated"
        }
        
        assertFalse("Message lambda should not be evaluated when logging is disabled", messageEvaluated)
        assertFalse("Metadata lambda should not be evaluated when logging is disabled", metadataEvaluated)
    }
}
