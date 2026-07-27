package com.meshlink.ble.data.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import com.meshlink.ble.data.BleConstants
import com.meshlink.common.logger.MeshLogger
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@SuppressLint("MissingPermission")
class ServiceDiscoveryManagerImpl @Inject constructor() : ServiceDiscoveryManager {
    override fun discoverServices(gatt: BluetoothGatt) {
        try {
            gatt.discoverServices()
        } catch (e: Exception) {
            MeshLogger.e("ServiceDiscoveryManager", "Failed to discover services for ${gatt.device.address}: ${e.message}")
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt) {
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
                    MeshLogger.e("ServiceDiscoveryManager", "Error writing descriptor for ${gatt.device.address}: ${e.message}")
                }
            } else {
                MeshLogger.w("ServiceDiscoveryManager", "CCCD descriptor not found for ${gatt.device.address}")
            }
        } else {
            MeshLogger.w("ServiceDiscoveryManager", "Mesh characteristic not found for ${gatt.device.address}")
        }
    }
}
