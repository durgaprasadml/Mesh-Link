package com.meshlink.ble.data.gatt

import com.meshlink.common.logger.MeshLogger
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketReassemblerImpl @Inject constructor() : PacketReassembler {
    companion object {
        const val MAX_REASSEMBLED_PACKET_BYTES = 512 * 1024 // 512 KB max reassembly size
    }

    private val reassemblyBuffers = ConcurrentHashMap<String, ByteArrayOutputStream>()

    override fun handleFragment(address: String, fragment: ByteArray): String? {
        if (fragment.isEmpty()) return null
        
        val type = fragment[0]
        val payload = fragment.copyOfRange(1, fragment.size)
        if (payload.size > MAX_REASSEMBLED_PACKET_BYTES) {
            reassemblyBuffers.remove(address)
            MeshLogger.e("PacketReassembler", "Fragment from $address exceeds MAX_REASSEMBLED_PACKET_BYTES (${payload.size}). Dropping.")
            return null
        }
        
        return when (type) {
            PacketFragmenterImpl.TYPE_FULL -> {
                reassemblyBuffers.remove(address)
                String(payload, Charsets.UTF_8)
            }
            PacketFragmenterImpl.TYPE_START -> {
                val bos = ByteArrayOutputStream()
                bos.write(payload)
                reassemblyBuffers[address] = bos
                null
            }
            PacketFragmenterImpl.TYPE_CONT -> {
                val bos = reassemblyBuffers[address] ?: return null
                if (bos.size() + payload.size > MAX_REASSEMBLED_PACKET_BYTES) {
                    reassemblyBuffers.remove(address)
                    MeshLogger.e("PacketReassembler", "Reassembly buffer for $address exceeded MAX_REASSEMBLED_PACKET_BYTES ($MAX_REASSEMBLED_PACKET_BYTES). Aborting.")
                    return null
                }
                bos.write(payload)
                null
            }
            PacketFragmenterImpl.TYPE_END -> {
                val bos = reassemblyBuffers.remove(address)
                if (bos != null) {
                    if (bos.size() + payload.size > MAX_REASSEMBLED_PACKET_BYTES) {
                        MeshLogger.e("PacketReassembler", "Final reassembled packet for $address exceeded MAX_REASSEMBLED_PACKET_BYTES ($MAX_REASSEMBLED_PACKET_BYTES). Dropping.")
                        return null
                    }
                    bos.write(payload)
                    String(bos.toByteArray(), Charsets.UTF_8)
                } else {
                    MeshLogger.w("PacketReassembler", "END fragment from $address but NO reassembly buffer! Packet lost.")
                    null
                }
            }
            else -> null
        }
    }

    override fun clear(address: String) {
        reassemblyBuffers.remove(address)
    }

    override fun clearAll() {
        reassemblyBuffers.clear()
    }
}
