package com.meshlink.ble.data.gatt

import com.meshlink.common.config.BleConfig
import com.meshlink.common.pool.BufferPool
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.yield

@Singleton
class PacketFragmenterImpl @Inject constructor() : PacketFragmenter {
    override suspend fun fragment(data: ByteArray, mtu: Int, onFragment: suspend (ByteArray) -> Unit) {
        val maxPayload = (minOf(mtu - BleConfig.GATT_HEADER_SIZE, BleConfig.MAX_ATTRIBUTE_VALUE_SIZE) - BleConfig.FRAG_HEADER_SIZE).coerceAtLeast(1)
        if (data.size <= maxPayload) {
            val packet = BufferPool.borrowBuffer(data.size + 1)
            packet[0] = TYPE_FULL
            System.arraycopy(data, 0, packet, 1, data.size)
            onFragment(packet)
        } else {
            var offset = 0
            while (offset < data.size) {
                val isFirst = offset == 0
                val remaining = data.size - offset
                val chunkSize = minOf(remaining, maxPayload)
                val isLast = offset + chunkSize >= data.size
                
                val packet = BufferPool.borrowBuffer(chunkSize + 1)
                packet[0] = when {
                    isFirst -> TYPE_START
                    isLast -> TYPE_END
                    else -> TYPE_CONT
                }
                System.arraycopy(data, offset, packet, 1, chunkSize)
                onFragment(packet)
                offset += chunkSize
                
                // Yield to allow other coroutines to process the queue/network
                yield()
            }
        }
    }
    
    companion object {
        const val TYPE_FULL = 0x00.toByte()
        const val TYPE_START = 0x01.toByte()
        const val TYPE_CONT = 0x02.toByte()
        const val TYPE_END = 0x03.toByte()
    }
}
