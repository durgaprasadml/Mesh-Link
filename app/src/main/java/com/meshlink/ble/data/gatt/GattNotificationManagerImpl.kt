package com.meshlink.ble.data.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.os.Build
import com.meshlink.common.logger.MeshLogger
import com.meshlink.common.pool.BufferPool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GattNotificationManagerImpl(
    private val gattServerProvider: () -> BluetoothGattServer?,
    private val applicationScope: CoroutineScope
) : GattNotificationManager {
    
    private data class QueuedNotification(
        val char: BluetoothGattCharacteristic,
        val value: ByteArray,
        var retryCount: Int = 0
    )

    private val mutex = Mutex()
    private val pendingNotifications = mutableMapOf<String, MutableList<QueuedNotification>>()
    private val activeNotifications = mutableMapOf<String, QueuedNotification>()

    override fun notifyCharacteristic(device: BluetoothDevice, char: BluetoothGattCharacteristic, value: ByteArray) {
        val copy = BufferPool.borrowBuffer(value.size)
        System.arraycopy(value, 0, copy, 0, value.size)
        
        applicationScope.launch {
            mutex.withLock {
                val queue = pendingNotifications.getOrPut(device.address) { mutableListOf() }
                queue.add(QueuedNotification(char, copy))
                flushQueueLocked(device)
            }
        }
    }
    
    override fun onNotificationSent(device: BluetoothDevice, status: Int) {
        MeshLogger.d("GattNotificationManager", "<- onNotificationSent for ${device.address} status=$status")
        applicationScope.launch {
            mutex.withLock {
                val active = activeNotifications.remove(device.address)
                if (active != null) {
                    BufferPool.returnBuffer(active.value)
                } else {
                    MeshLogger.e("GattNotificationManager", "✗ No active notification found for ${device.address}")
                }
                flushQueueLocked(device)
            }
        }
    }

    override fun clear(address: String) {
        applicationScope.launch {
            mutex.withLock {
                pendingNotifications.remove(address)?.forEach { BufferPool.returnBuffer(it.value) }
                activeNotifications.remove(address)?.let { BufferPool.returnBuffer(it.value) }
            }
        }
    }

    private fun flushQueueLocked(device: BluetoothDevice) {
        if (activeNotifications.containsKey(device.address)) return
        
        val queue = pendingNotifications[device.address] ?: return
        if (queue.isEmpty()) return
        
        val pending = queue.removeAt(0)
        
        try {
            val server = gattServerProvider()
            if (server != null) {
                val success: Boolean
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    success = server.notifyCharacteristicChanged(device, pending.char, false, pending.value)
                } else {
                    @Suppress("DEPRECATION")
                    pending.char.value = pending.value
                    success = server.notifyCharacteristicChanged(device, pending.char, false)
                }
                
                MeshLogger.d("GattNotificationManager", "-> notifyCharacteristicChanged for ${device.address} started=$success size=${pending.value.size}")
                
                if (success) {
                    activeNotifications[device.address] = pending
                } else {
                    pending.retryCount++
                    if (pending.retryCount > 10) {
                        MeshLogger.e("GattNotificationManager", "⚠ notifyCharacteristicChanged permanently failed for ${device.address}, dropping packet.")
                        BufferPool.returnBuffer(pending.value)
                        // continue flushing remaining
                        applicationScope.launch {
                            mutex.withLock { flushQueueLocked(device) }
                        }
                    } else {
                        MeshLogger.w("GattNotificationManager", "⚠ notifyCharacteristicChanged returned false for ${device.address}. Retrying...")
                        queue.add(0, pending)
                        applicationScope.launch {
                            delay(50L * (1 shl pending.retryCount.coerceAtMost(6)))
                            mutex.withLock { flushQueueLocked(device) }
                        }
                    }
                }
            } else {
                MeshLogger.w("GattNotificationManager", "GATT server is null, cannot notify ${device.address}")
                BufferPool.returnBuffer(pending.value)
            }
        } catch (e: Exception) {
            MeshLogger.e("GattNotificationManager", "Error sending notification to ${device.address}: ${e.message}")
            BufferPool.returnBuffer(pending.value)
        }
    }
}
