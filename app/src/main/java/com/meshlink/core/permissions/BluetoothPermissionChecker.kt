package com.meshlink.core.permissions

import android.content.Context

/**
 * Interface for checking Bluetooth-related runtime permissions.
 * Abstracts the UI/Android version specifics away from the BLE data layer.
 */
interface BluetoothPermissionChecker {
    /**
     * Returns true if all required BLE permissions are granted for the current Android version.
     */
    fun hasRequiredPermissions(context: Context): Boolean
}
