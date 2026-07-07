package com.meshlink.data.ble

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.io.ByteArrayOutputStream
import android.os.Build

@SuppressLint("MissingPermission")
class BleGattManager(private val context: Context) {
    private companion object {
        const val DEFAULT_MTU = 23
        const val GATT_HEADER_SIZE = 3
        const val FRAG_HEADER_SIZE = 1
        const val MAX_ATTRIBUTE_VALUE_SIZE = 512 // GATT hard limit

        const val TYPE_FULL = 0x00.toByte()
        const val TYPE_START = 0x01.toByte()
        const val TYPE_CONT = 0x02.toByte()
        const val TYPE_END = 0x03.toByte()
    }

    private data class PendingClientWrite(
        val address: String,
        val bytes: ByteArray
    )

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private var gattServer: BluetoothGattServer? = null
    
    // Multi-Point Ad-Hoc Connections
    val connectedServers = ConcurrentHashMap<String, BluetoothDevice>()
    val activeClients = ConcurrentHashMap<String, BluetoothGatt>()
    private val deviceMtus = ConcurrentHashMap<String, Int>()
    private val reassemblyBuffers = ConcurrentHashMap<String, ByteArrayOutputStream>()
    private val pendingClientWrites = mutableListOf<PendingClientWrite>()
    private var activeWriteAddress: String? = null

    private val _incomingMessages = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 50)
    val incomingMessages: SharedFlow<Pair<String, String>> = _incomingMessages.asSharedFlow()

    fun startServer() {
        if (gattServer != null) return
        gattServer = bluetoothManager.openGattServer(context, serverCallback)
        val service = BluetoothGattService(BleConstants.MESH_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        
        val msgChar = BluetoothGattCharacteristic(
            BleConstants.MSG_CHAR_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val cccd = BluetoothGattDescriptor(
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
        )
        msgChar.addDescriptor(cccd)
        service.addCharacteristic(msgChar)
        gattServer?.addService(service)
        Log.d("BleGatt", "GATT Server started")
    }

    fun stopServer() {
        gattServer?.close()
        gattServer = null
        connectedServers.clear()
        deviceMtus.clear()
        reassemblyBuffers.clear()
        activeClients.values.forEach { 
            it.disconnect()
            it.close() 
        }
        activeClients.clear()
        pendingClientWrites.clear()
        activeWriteAddress = null
    }

    fun connectToDevice(address: String) {
        if (activeClients.containsKey(address)) return
        val device = bluetoothManager.adapter.getRemoteDevice(address)
        activeClients[address] = device.connectGatt(context, false, clientCallback)
    }

    fun broadcastPacket(jsonPacket: String, excludeAddress: String? = null) {
        val bytes = jsonPacket.toByteArray(Charsets.UTF_8)
        
        // Dispatch to Nodes we initiated connection to
        activeClients.forEach { (address, _) ->
            if (address != excludeAddress) {
                enqueueClientWrite(address, bytes)
            }
        }
        
        // Dispatch to Nodes that connected to us
        synchronized(this) {
            val service = gattServer?.getService(BleConstants.MESH_SERVICE_UUID)
            val char = service?.getCharacteristic(BleConstants.MSG_CHAR_UUID)
            
            if (char != null) {
                connectedServers.forEach { (address, device) ->
                    if (address != excludeAddress) {
                        sendFragmentedNotification(device, char, bytes)
                    }
                }
            }
        }
    }

    private fun sendFragmentedNotification(device: BluetoothDevice, char: BluetoothGattCharacteristic, data: ByteArray) {
        val mtu = deviceMtus[device.address] ?: DEFAULT_MTU
        // Ensure maxPayload respects GATT 512-byte value limit and MTU
        val maxPayload = (minOf(mtu - GATT_HEADER_SIZE, MAX_ATTRIBUTE_VALUE_SIZE) - FRAG_HEADER_SIZE).coerceAtLeast(1)
        
        if (data.size <= maxPayload) {
            val packet = ByteArray(data.size + 1)
            packet[0] = TYPE_FULL
            System.arraycopy(data, 0, packet, 1, data.size)
            notify(device, char, packet)
        } else {
            var offset = 0
            while (offset < data.size) {
                val isFirst = offset == 0
                val remaining = data.size - offset
                val chunkSize = minOf(remaining, maxPayload)
                val isLast = offset + chunkSize >= data.size
                
                val packet = ByteArray(chunkSize + 1)
                packet[0] = when {
                    isFirst -> TYPE_START
                    isLast -> TYPE_END
                    else -> TYPE_CONT
                }
                System.arraycopy(data, offset, packet, 1, chunkSize)
                notify(device, char, packet)
                offset += chunkSize
                
                // Small delay to prevent dropping packets on some devices
                try { Thread.sleep(10) } catch (_: Exception) {}
            }
        }
    }

    private fun notify(device: BluetoothDevice, char: BluetoothGattCharacteristic, value: ByteArray) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gattServer?.notifyCharacteristicChanged(device, char, false, value)
            } else {
                @Suppress("DEPRECATION")
                char.value = value
                gattServer?.notifyCharacteristicChanged(device, char, false)
            }
        } catch (e: Exception) {
            Log.e("BleGatt", "Error sending notification to ${device.address}: ${e.message}")
        }
    }

    private fun handleIncomingFragment(address: String, value: ByteArray) {
        if (value.isEmpty()) return
        
        val type = value[0]
        val payload = value.copyOfRange(1, value.size)
        
        when (type) {
            TYPE_FULL -> {
                reassemblyBuffers.remove(address)
                val message = String(payload, Charsets.UTF_8)
                _incomingMessages.tryEmit(address to message)
            }
            TYPE_START -> {
                val bos = ByteArrayOutputStream()
                bos.write(payload)
                reassemblyBuffers[address] = bos
            }
            TYPE_CONT -> {
                reassemblyBuffers[address]?.write(payload)
            }
            TYPE_END -> {
                val bos = reassemblyBuffers.remove(address)
                if (bos != null) {
                    bos.write(payload)
                    val message = String(bos.toByteArray(), Charsets.UTF_8)
                    _incomingMessages.tryEmit(address to message)
                }
            }
        }
    }

    @Synchronized
    private fun enqueueClientWrite(address: String, bytes: ByteArray) {
        val mtu = deviceMtus[address] ?: DEFAULT_MTU
        val maxPayload = (minOf(mtu - GATT_HEADER_SIZE, MAX_ATTRIBUTE_VALUE_SIZE) - FRAG_HEADER_SIZE).coerceAtLeast(1)
        
        if (bytes.size <= maxPayload) {
            val packet = ByteArray(bytes.size + 1)
            packet[0] = TYPE_FULL
            System.arraycopy(bytes, 0, packet, 1, bytes.size)
            pendingClientWrites.add(PendingClientWrite(address, packet))
        } else {
            var offset = 0
            while (offset < bytes.size) {
                val isFirst = offset == 0
                val remaining = bytes.size - offset
                val chunkSize = minOf(remaining, maxPayload)
                val isLast = offset + chunkSize >= bytes.size
                
                val packet = ByteArray(chunkSize + 1)
                packet[0] = when {
                    isFirst -> TYPE_START
                    isLast -> TYPE_END
                    else -> TYPE_CONT
                }
                System.arraycopy(bytes, offset, packet, 1, chunkSize)
                pendingClientWrites.add(PendingClientWrite(address, packet))
                offset += chunkSize
            }
        }
        flushClientWriteQueueLocked()
    }

    @Synchronized
    private fun flushClientWriteQueue() {
        flushClientWriteQueueLocked()
    }

    private fun flushClientWriteQueueLocked() {
        if (activeWriteAddress != null) return

        val iterator = pendingClientWrites.iterator()
        while (iterator.hasNext()) {
            val pending = iterator.next()
            val gatt = activeClients[pending.address]
            if (gatt == null) {
                iterator.remove()
                continue
            }

            val char = gatt.getService(BleConstants.MESH_SERVICE_UUID)
                ?.getCharacteristic(BleConstants.MSG_CHAR_UUID)
            if (char == null) {
                try {
                    gatt.discoverServices()
                } catch (e: Exception) {
                    Log.w("BleGatt", "Service discovery retry failed for ${pending.address}: ${e.message}")
                    iterator.remove()
                }
                continue
            }

            try {
                val method = char.javaClass.getMethod("setValue", ByteArray::class.java)
                method.invoke(char, pending.bytes)
                val writeStarted = gatt.writeCharacteristic(char)
                iterator.remove()
                if (writeStarted) {
                    activeWriteAddress = pending.address
                    return
                }
                Log.w("BleGatt", "writeCharacteristic returned false for ${pending.address}")
            } catch (e: Exception) {
                iterator.remove()
                Log.e("BleGatt", "Error writing char: ${e.message}")
            }
        }
    }

    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedServers[device.address] = device
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedServers.remove(device.address)
                deviceMtus.remove(device.address)
                reassemblyBuffers.remove(device.address)
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            super.onMtuChanged(device, mtu)
            deviceMtus[device.address] = mtu
            Log.d("BleGatt", "Server MTU changed for ${device.address}: $mtu")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value)
            if (characteristic.uuid == BleConstants.MSG_CHAR_UUID) {
                if (responseNeeded) {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
                handleIncomingFragment(device.address, value)
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    private val clientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // Negotiate massive MTU for entire JSON packets
                val mtuRequested = gatt.requestMtu(512)
                if (!mtuRequested) {
                    gatt.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                activeClients.remove(gatt.device.address)
                deviceMtus.remove(gatt.device.address)
                reassemblyBuffers.remove(gatt.device.address)
                synchronized(this@BleGattManager) {
                    pendingClientWrites.removeAll { it.address == gatt.device.address }
                    if (activeWriteAddress == gatt.device.address) {
                        activeWriteAddress = null
                    }
                }
                gatt.close()
                flushClientWriteQueue()
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                deviceMtus[gatt.device.address] = mtu
                Log.d("BleGatt", "Client MTU changed for ${gatt.device.address}: $mtu")
            }
            gatt.discoverServices()
        }

        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val char = gatt.getService(BleConstants.MESH_SERVICE_UUID)?.getCharacteristic(BleConstants.MSG_CHAR_UUID)
            if (char != null) {
                gatt.setCharacteristicNotification(char, true)
                val descriptor = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
                if (descriptor != null) {
                    try {
                        val method = descriptor.javaClass.getMethod("setValue", ByteArray::class.java)
                        method.invoke(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        gatt.writeDescriptor(descriptor)
                    } catch (e: Exception) {
                        Log.e("BleGatt", "Error writing descriptor: ${e.message}")
                    }
                }
            }
            flushClientWriteQueue()
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BleConstants.MSG_CHAR_UUID) {
                handleIncomingFragment(gatt.device.address, characteristic.value)
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == BleConstants.MSG_CHAR_UUID) {
                handleIncomingFragment(gatt.device.address, value)
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (characteristic.uuid == BleConstants.MSG_CHAR_UUID) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    Log.w("BleGatt", "Characteristic write failed for ${gatt.device.address}: $status")
                }
                synchronized(this@BleGattManager) {
                    if (activeWriteAddress == gatt.device.address) {
                        activeWriteAddress = null
                    }
                }
                flushClientWriteQueue()
            }
        }
    }
}
