package com.meshlink.ble.data.gatt

import com.meshlink.common.logger.MeshLogger
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacketReassemblerImpl @Inject constructor() : PacketReassembler {
    private val reassemblyBuffers = ConcurrentHashMap<String, ByteArrayOutputStream>()

    override fun handleFragment(address: String, fragment: ByteArray): String? {
        if (fragment.isEmpty()) return null
        
        val type = fragment[0]
        val payload = fragment.copyOfRange(1, fragment.size)
        
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
                reassemblyBuffers[address]?.write(payload)
                null
            }
            PacketFragmenterImpl.TYPE_END -> {
                val bos = reassemblyBuffers.remove(address)
                if (bos != null) {
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
