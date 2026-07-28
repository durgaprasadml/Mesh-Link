package com.meshlink.ble.data.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import com.meshlink.ble.data.BleConstants
import com.meshlink.common.logger.MeshLogger
import com.meshlink.core.permissions.BluetoothPermissionChecker
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ServiceDiscoveryManagerImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionChecker: BluetoothPermissionChecker
) : ServiceDiscoveryManager {
    override fun discoverServices(gatt: BluetoothGatt) {
        if (!permissionChecker.hasRequiredPermissions(context)) {
            MeshLogger.w("ServiceDiscoveryManager", "Missing permissions to discover services for ${gatt.device.address}")
            return
        }
        try {
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
            val ignored = gatt.discoverServices()
        } catch (e: Exception) {
            MeshLogger.e("ServiceDiscoveryManager", "Failed to discover services for ${gatt.device.address}: ${e.message}")
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt) {
        val char = gatt.getService(BleConstants.MESH_SERVICE_UUID)?.getCharacteristic(BleConstants.MSG_CHAR_UUID)
        if (char != null) {
            if (!permissionChecker.hasRequiredPermissions(context)) {
                MeshLogger.w("ServiceDiscoveryManager", "Missing permissions to enable notifications for ${gatt.device.address}")
                return
            }
            @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
            val ignoredNotif = gatt.setCharacteristicNotification(char, true)
            val descriptor = char.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
            if (descriptor != null) {
                try {
                    val method = descriptor.javaClass.getMethod("setValue", ByteArray::class.java)
                    method.invoke(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    @SuppressLint("MissingPermission") // Safe: checked via permissionChecker
                    val ignoredWrite = gatt.writeDescriptor(descriptor)
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
