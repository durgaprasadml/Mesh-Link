package com.meshlink.ble.data

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Build
import com.meshlink.common.logger.MeshLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import com.meshlink.di.ApplicationScope
import com.meshlink.ble.data.gatt.GattConnectionManager
import com.meshlink.ble.data.gatt.MtuNegotiationManager
import com.meshlink.ble.data.gatt.GattWriteQueue
import com.meshlink.ble.data.gatt.GattNotificationManager
import com.meshlink.ble.data.gatt.ServiceDiscoveryManager
import com.meshlink.ble.data.gatt.PacketFragmenter
import com.meshlink.ble.data.gatt.PacketReassembler
import com.meshlink.ble.data.gatt.PendingClientWrite
import com.meshlink.common.pool.BufferPool
import com.meshlink.core.permissions.BluetoothPermissionChecker
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Orchestrates BLE GATT operations by delegating to specialized managers.
 *
 * Responsibility: Handles Android BLE framework callbacks and routes them to dedicated managers.
 * Thread Ownership: Delegates to appropriate scopes (applicationScope).
 * Lifecycle Ownership: Application scoped.
 * Dependencies: GattConnectionManager, MtuNegotiationManager, GattWriteQueue,
 *               GattNotificationManager, ServiceDiscoveryManager, PacketFragmenter, PacketReassembler.
 */
@Singleton
class BleGattManager @Inject constructor(
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    private val connectionManager: GattConnectionManager,
    private val mtuManager: MtuNegotiationManager,
    private val writeQueue: GattWriteQueue,
    private val notificationManager: GattNotificationManager,
    private val discoveryManager: ServiceDiscoveryManager,
    private val fragmenter: PacketFragmenter,
    private val reassembler: PacketReassembler,
    private val permissionChecker: BluetoothPermissionChecker
) {
    private val mutex = Mutex()
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private var gattServer: BluetoothGattServer? = null

    enum class BleConnectionState {
        DISCONNECTED, CONNECTING, CONNECTED, SERVICES_DISCOVERED, READY
    }

    sealed class GattEvent {
        data class Connected(val address: String) : GattEvent()
        data class Disconnected(val address: String) : GattEvent()
        data class MtuChanged(val address: String, val mtu: Int) : GattEvent()
        data class ServicesDiscovered(val address: String) : GattEvent()
        data class QueueEmpty(val address: String) : GattEvent()
    }

    private val _gattEvents = MutableSharedFlow<GattEvent>(extraBufferCapacity = 100)
    val gattEvents: SharedFlow<GattEvent> = _gattEvents.asSharedFlow()

    private val _incomingMessages = MutableSharedFlow<Pair<String, String>>(extraBufferCapacity = 50)
    val incomingMessages: SharedFlow<Pair<String, String>> = _incomingMessages.asSharedFlow()

    // Delegated properties to maintain compatibility
    val connectedServers: Map<String, BluetoothDevice> get() = connectionManager.connectedServers
    val activeClients: Map<String, BluetoothGatt> get() = connectionManager.activeClients
    val deviceStates: Map<String, BleConnectionState> get() = connectionManager.deviceStates

    fun getGattServer(): BluetoothGattServer? = gattServer

    fun startServer() {
        if (!permissionChecker.hasRequiredPermissions(context)) {
            MeshLogger.w("BleGatt", "Missing permissions for starting GATT server")
            return
        }
        if (gattServer != null) return
        try {
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
            val newGattServer = bluetoothManager.openGattServer(context, serverCallback)
            gattServer = newGattServer
            val service = BluetoothGattService(BleConstants.MESH_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            
            val msgChar = BluetoothGattCharacteristic(
                BleConstants.MSG_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_NOTIFY or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val cccd = BluetoothGattDescriptor(
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"),
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            msgChar.addDescriptor(cccd)
            service.addCharacteristic(msgChar)
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
            val ignored = gattServer?.addService(service)
            MeshLogger.d("BleGatt", "GATT Server started")
        } catch (e: SecurityException) {
            MeshLogger.e("BleGatt", "SecurityException starting GATT server", e)
        } catch (e: Exception) {
            MeshLogger.e("BleGatt", "Exception starting GATT server: ${e.message}", e)
        }
    }

    fun stopServer() {
        if (permissionChecker.hasRequiredPermissions(context)) {
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
            val ignoredClose = gattServer?.close()
        }
        gattServer = null
        
        connectionManager.activeClients.values.forEach { 
            if (permissionChecker.hasRequiredPermissions(context)) {
                @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
                val ignoredDisconnect = it.disconnect()
                @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
                val ignoredClientClose = it.close() 
            }
        }
        
        connectionManager.clear()
        mtuManager.clear()
        writeQueue.clear()
        reassembler.clearAll()
    }

    fun isQueueEmpty(address: String): Boolean {
        return !writeQueue.hasPendingForDevice(address)
    }

    fun connectToDevice(address: String) {
        if (!permissionChecker.hasRequiredPermissions(context)) {
            MeshLogger.w("BleGatt", "Missing permissions to connect to $address")
            return
        }
        if (connectionManager.getClient(address) != null) return
        try {
            val device = bluetoothManager.adapter.getRemoteDevice(address)
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
            val gatt = device.connectGatt(context, false, clientCallback)
            connectionManager.addActiveClient(address, gatt)
        } catch (e: SecurityException) {
            MeshLogger.e("BleGatt", "SecurityException connecting to $address", e)
        } catch (e: Exception) {
            MeshLogger.e("BleGatt", "Exception connecting to $address: ${e.message}", e)
        }
    }

    fun disconnectDevice(address: String) {
        if (!permissionChecker.hasRequiredPermissions(context)) {
            MeshLogger.w("BleGatt", "Missing permissions to disconnect from $address")
            return
        }
        connectionManager.getClient(address)?.let {
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
            val ignoredDisconnect = it.disconnect()
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
            val ignoredClose = it.close()
        }
        connectionManager.removeActiveClient(address)
    }

    fun broadcastPacket(jsonPacket: String, excludeAddress: String? = null, includeAddress: String? = null) {
        val bytes = jsonPacket.toByteArray(Charsets.UTF_8)

        MeshLogger.d("BleGatt", "[TRANSPORT-A] ═══ broadcastPacket() ═══")
        MeshLogger.d("BleGatt", "[TRANSPORT-A]   totalBytes      : ${bytes.size} B")
        
        // Dispatch to Nodes we initiated connection to
        connectionManager.activeClients.forEach { (address, _) ->
            if (includeAddress != null) {
                if (address == includeAddress) enqueueClientWrite(address, bytes)
            } else if (address != excludeAddress) {
                enqueueClientWrite(address, bytes)
            }
        }
        
        // Dispatch to Nodes that connected to us
        applicationScope.launch {
            val service = gattServer?.getService(BleConstants.MESH_SERVICE_UUID)
            val char = service?.getCharacteristic(BleConstants.MSG_CHAR_UUID)
            
            if (char != null) {
                connectionManager.connectedServers.forEach { (address, device) ->
                    if (includeAddress != null) {
                        if (address == includeAddress) sendFragmentedNotification(device, char, bytes)
                    } else if (address != excludeAddress) {
                        sendFragmentedNotification(device, char, bytes)
                    }
                }
            }
        }
    }

    private fun sendFragmentedNotification(device: BluetoothDevice, char: BluetoothGattCharacteristic, data: ByteArray) {
        applicationScope.launch {
            val mtu = mtuManager.getMtu(device.address)
            fragmenter.fragment(data, mtu) { fragment ->
                notificationManager.notifyCharacteristic(device, char, fragment)
                BufferPool.returnBuffer(fragment)
            }
        }
    }

    private fun enqueueClientWrite(address: String, bytes: ByteArray) {
        applicationScope.launch {
            mutex.withLock {
                val mtu = mtuManager.getMtu(address)
                fragmenter.fragment(bytes, mtu) { fragment ->
                    // Make a copy since fragmenter re-uses buffers? Actually fragmenter borrows from pool.
                    // We need to keep it until written.
                    // Wait, PacketFragmenter returns a borrowed buffer. The queue will own it until written.
                    val packetCopy = fragment.copyOf() // Safer to copy since fragment might be returned early? No, fragmenter doesn't return it.
                    // But just to be safe with the async nature of the queue. Let's borrow and copy.
                    val queuedPacket = BufferPool.borrowBuffer(fragment.size)
                    System.arraycopy(fragment, 0, queuedPacket, 0, fragment.size)
                    BufferPool.returnBuffer(fragment) // return the fragmenter's buffer
                    
                    writeQueue.enqueue(PendingClientWrite(address, queuedPacket))
                }
                flushClientWriteQueueLocked()
            }
        }
    }

    private fun flushClientWriteQueue() {
        applicationScope.launch {
            mutex.withLock {
                flushClientWriteQueueLocked()
            }
        }
    }

    private fun flushClientWriteQueueLocked() {
        val now = System.currentTimeMillis()
        val pending = writeQueue.dequeueReady(now) { address -> 
            connectionManager.getDeviceState(address) == BleConnectionState.READY 
        } ?: return

        val gatt = connectionManager.getClient(pending.address)
        if (gatt == null) {
            MeshLogger.w("BleGatt", "[TRANSPORT-A]   ⚠ No GATT client for ${pending.address} — dropping fragment")
            BufferPool.returnBuffer(pending.bytes)
            return
        }

        val char = gatt.getService(BleConstants.MESH_SERVICE_UUID)?.getCharacteristic(BleConstants.MSG_CHAR_UUID)
        if (char == null) {
            try {
                MeshLogger.w("BleGatt", "[TRANSPORT-A]   ⚠ Service not discovered for ${pending.address} — retrying discoverServices()")
                discoveryManager.discoverServices(gatt)
            } catch (e: Exception) {
                MeshLogger.w("BleGatt", "Service discovery retry failed for ${pending.address}: ${e.message}")
                BufferPool.returnBuffer(pending.bytes)
            }
            writeQueue.enqueue(pending)
            return
        }

        try {
            if (!permissionChecker.hasRequiredPermissions(context)) {
                MeshLogger.w("BleGatt", "[TRANSPORT-A] ⚠ Missing permissions to write characteristic for ${pending.address}")
                writeQueue.enqueue(pending)
                return
            }
            val writeStarted: Boolean
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
                val statusCode = gatt.writeCharacteristic(char, pending.bytes, BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
                writeStarted = statusCode == android.bluetooth.BluetoothStatusCodes.SUCCESS
            } else {
                @Suppress("DEPRECATION")
                char.value = pending.bytes
                char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                @Suppress("DEPRECATION")
                @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
                val writeRes = gatt.writeCharacteristic(char)
                writeStarted = writeRes
            }
            
            MeshLogger.d("BleGatt", "[TRANSPORT-A]   -> writeCharacteristic for ${pending.address} started=$writeStarted size=${pending.bytes.size}")
            
            if (writeStarted) {
                writeQueue.setActiveWrite(pending)
            } else {
                val backoff = writeQueue.requeueWithBackoff(pending)
                if (backoff < 0) {
                    MeshLogger.e("BleGatt", "[TRANSPORT-A]   ⚠ writeCharacteristic permanently failed for ${pending.address}, dropping packet.")
                    BufferPool.returnBuffer(pending.bytes)
                } else {
                    MeshLogger.w("BleGatt", "[TRANSPORT-A]   ⚠ writeCharacteristic returned false. Retry in ${backoff}ms")
                    applicationScope.launch {
                        delay(backoff)
                        flushClientWriteQueue()
                    }
                }
            }
        } catch (e: Exception) {
            BufferPool.returnBuffer(pending.bytes)
            MeshLogger.e("BleGatt", "[TRANSPORT-A]   ✗ Exception in writeCharacteristic: ${e.message}")
        }
    }

    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionManager.addConnectedServer(device.address, device)
                _gattEvents.tryEmit(GattEvent.Connected(device.address))
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionManager.removeConnectedServer(device.address)
                mtuManager.removeMtu(device.address)
                reassembler.clear(device.address)
                notificationManager.clear(device.address)
                _gattEvents.tryEmit(GattEvent.Disconnected(device.address))
            }
        }

        override fun onMtuChanged(device: BluetoothDevice, mtu: Int) {
            super.onMtuChanged(device, mtu)
            mtuManager.updateMtu(device.address, mtu)
            _gattEvents.tryEmit(GattEvent.MtuChanged(device.address, mtu))
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
                    if (permissionChecker.hasRequiredPermissions(context)) {
                        @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
                        val ignoredResponse = gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                    }
                }
                val reassembled = reassembler.handleFragment(device.address, value)
                if (reassembled != null) {
                    _incomingMessages.tryEmit(device.address to reassembled)
                }
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
                if (permissionChecker.hasRequiredPermissions(context)) {
                    @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
                    val ignoredResponse = gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                }
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            super.onNotificationSent(device, status)
            notificationManager.onNotificationSent(device, status)
        }
    }

    private val clientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectionManager.updateDeviceState(gatt.device.address, BleConnectionState.CONNECTED)
                _gattEvents.tryEmit(GattEvent.Connected(gatt.device.address))
                
                val mtuRequested = mtuManager.requestMtu(gatt)
                if (!mtuRequested) {
                    discoveryManager.discoverServices(gatt)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectionManager.updateDeviceState(gatt.device.address, BleConnectionState.DISCONNECTED)
                connectionManager.removeActiveClient(gatt.device.address)
                mtuManager.removeMtu(gatt.device.address)
                reassembler.clear(gatt.device.address)
                
                applicationScope.launch {
                    mutex.withLock {
                        val dropped = writeQueue.dropAllForDevice(gatt.device.address)
                        dropped.forEach { BufferPool.returnBuffer(it.bytes) }
                        flushClientWriteQueueLocked()
                    }
                }
                
                if (permissionChecker.hasRequiredPermissions(context)) {
                    @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
                    val ignoredClose = gatt.close()
                } else {
                    // Safe fallback if permissions are revoked, gatt connection might be dead anyway
                    try {
                        @SuppressLint("MissingPermission")
                        val ignoredClose = gatt.close()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
                _gattEvents.tryEmit(GattEvent.Disconnected(gatt.device.address))
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mtuManager.updateMtu(gatt.device.address, mtu)
                _gattEvents.tryEmit(GattEvent.MtuChanged(gatt.device.address, mtu))
            }
            discoveryManager.discoverServices(gatt)
        }

        @Suppress("DEPRECATION")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                connectionManager.updateDeviceState(gatt.device.address, BleConnectionState.SERVICES_DISCOVERED)
                _gattEvents.tryEmit(GattEvent.ServicesDiscovered(gatt.device.address))
            }
            discoveryManager.onServicesDiscovered(gatt)
            flushClientWriteQueue()
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (status == BluetoothGatt.GATT_SUCCESS && descriptor.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")) {
                connectionManager.updateDeviceState(gatt.device.address, BleConnectionState.READY)
                flushClientWriteQueue()
            }
        }

        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            if (characteristic.uuid == BleConstants.MSG_CHAR_UUID) {
                characteristic.value?.let { value ->
                    val reassembled = reassembler.handleFragment(gatt.device.address, value)
                    if (reassembled != null) {
                        _incomingMessages.tryEmit(gatt.device.address to reassembled)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray) {
            if (characteristic.uuid == BleConstants.MSG_CHAR_UUID) {
                val reassembled = reassembler.handleFragment(gatt.device.address, value)
                if (reassembled != null) {
                    _incomingMessages.tryEmit(gatt.device.address to reassembled)
                }
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
                MeshLogger.d("BleGatt", "[TRANSPORT-A]   <- onCharacteristicWrite for ${gatt.device.address} status=$status")
                applicationScope.launch {
                    mutex.withLock {
                        val removed = writeQueue.removeActive(gatt.device.address)
                        if (removed != null) {
                            BufferPool.returnBuffer(removed.bytes)
                        } else {
                            MeshLogger.e("BleGatt", "[TRANSPORT-A]   ✗ No active write found for ${gatt.device.address} on write callback!")
                        }
                        
                        if (!writeQueue.hasPendingForDevice(gatt.device.address)) {
                            _gattEvents.tryEmit(GattEvent.QueueEmpty(gatt.device.address))
                        }
                        
                        flushClientWriteQueueLocked()
                    }
                }
            }
        }
    }
}
