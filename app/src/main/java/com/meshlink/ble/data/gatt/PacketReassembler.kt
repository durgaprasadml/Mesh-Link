package com.meshlink.ble.data.gatt

/**
 * Handles reassembly of incoming BLE byte fragments into full JSON strings.
 *
 * Responsibility: Collects START/CONT/END fragments and reassembles them.
 * Thread Ownership: Thread-safe.
 * Lifecycle Ownership: Application scoped.
 * Dependencies: None.
 */
interface PacketReassembler {
    /**
     * Reassembles fragments. Returns the complete JSON string when reassembly is finished,
     * or null if the packet is still incomplete.
     */
    fun handleFragment(address: String, fragment: ByteArray): String?
    fun clear(address: String)
    fun clearAll()
}
