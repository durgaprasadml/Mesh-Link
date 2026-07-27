package com.meshlink.ble.data.gatt

import android.bluetooth.BluetoothGatt
import com.meshlink.common.config.BleConfig
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages MTU negotiation and caching for BLE connections.
 *
 * Responsibility: Requests MTU changes and stores the negotiated MTU per device.
 * Thread Ownership: Thread-safe (ConcurrentHashMap).
 * Lifecycle Ownership: Application scoped.
 * Dependencies: None.
 */
interface MtuNegotiationManager {
    fun getMtu(address: String): Int
    fun updateMtu(address: String, mtu: Int)
    fun requestMtu(gatt: BluetoothGatt, requestedMtu: Int = BleConfig.MAX_MTU_REQUEST): Boolean
    fun removeMtu(address: String)
    fun clear()
}

@Singleton
class MtuNegotiationManagerImpl @Inject constructor() : MtuNegotiationManager {
    private val deviceMtus = ConcurrentHashMap<String, Int>()

    override fun getMtu(address: String): Int {
        return deviceMtus[address] ?: BleConfig.DEFAULT_MTU
    }

    override fun updateMtu(address: String, mtu: Int) {
        deviceMtus[address] = mtu
    }

    override fun requestMtu(gatt: BluetoothGatt, requestedMtu: Int): Boolean {
        return gatt.requestMtu(requestedMtu)
    }

    override fun removeMtu(address: String) {
        deviceMtus.remove(address)
    }

    override fun clear() {
        deviceMtus.clear()
    }
}
