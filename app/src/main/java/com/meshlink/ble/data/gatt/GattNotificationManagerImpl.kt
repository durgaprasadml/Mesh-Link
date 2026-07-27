package com.meshlink.ble.data.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.os.Build
import com.meshlink.common.logger.MeshLogger

class GattNotificationManagerImpl(
    private val gattServerProvider: () -> BluetoothGattServer?
) : GattNotificationManager {
    
    override fun notifyCharacteristic(device: BluetoothDevice, char: BluetoothGattCharacteristic, value: ByteArray) {
        try {
            val server = gattServerProvider()
            if (server != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    server.notifyCharacteristicChanged(device, char, false, value)
                } else {
                    @Suppress("DEPRECATION")
                    char.value = value
                    server.notifyCharacteristicChanged(device, char, false)
                }
            } else {
                MeshLogger.w("GattNotificationManager", "GATT server is null, cannot notify ${device.address}")
            }
        } catch (e: Exception) {
            MeshLogger.e("GattNotificationManager", "Error sending notification to ${device.address}: ${e.message}")
        }
    }
}
