package com.meshlink.simulator.tests

import com.meshlink.domain.model.PacketType
import com.meshlink.simulator.core.MeshSimulator
import com.meshlink.simulator.profile.NetworkProfile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertTrue

/**
 * Parameterized test suite validating packet routing for every [PacketType] value.
 *
 * Verifies:
 * - Packet type is preserved through relay hops
 * - Payload arrives intact at destination
 * - All 22 PacketType values are covered
 *
 * Uses JUnit 4 @Parameterized (compatible with Gradle Android test runner).
 */
@RunWith(Parameterized::class)
class PacketTypeTest(
    private val packetType: PacketType,
    private val typeName: String
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "type={1}")
        fun data(): Collection<Array<Any>> = PacketType.values().map { type ->
            arrayOf(type, type.name)
        }
    }

    @Test
    fun `all packet types route and deliver correctly`() {
        val sim = MeshSimulator.build {
            nodes(listOf("sender", "relay", "receiver"))
            topology { ids -> com.meshlink.simulator.topology.TopologyBuilder.line(ids) }
            profile(NetworkProfile.PerfectNetwork)
        }
        val payload = "type-test-${packetType.name}"
        sim.node("sender").sendPacket("receiver", payload, type = packetType)
        sim.runUntilQuiet()

        val received = sim.node("receiver").receivedPackets()
        val matchingPacket = received.firstOrNull { it.second.payload == payload }

        assertTrue(
            matchingPacket != null,
            "PacketType.$typeName was not delivered to receiver. Drops: " +
            sim.recorder.getDroppedPackets().filter { it.packetType == typeName }
                .joinToString { "[${it.nodeId}: ${it.dropReason}]" }
        )

        // Verify packet type is preserved through relay
        assertTrue(
            matchingPacket!!.second.type == packetType,
            "PacketType.$typeName was altered during relay: got ${matchingPacket.second.type}"
        )
    }
}
