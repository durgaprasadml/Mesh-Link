package com.meshlink.core.permissions

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.net.wifi.WifiManager
import com.meshlink.ui.components.areRadiosAndPermissionsReady
import com.meshlink.ui.components.isBluetoothEnabled
import com.meshlink.ui.components.isWifiEnabled
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RadioAndPermissionGateTest {

    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var wifiManager: WifiManager

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        bluetoothManager = mockk(relaxed = true)
        bluetoothAdapter = mockk(relaxed = true)
        wifiManager = mockk(relaxed = true)

        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns bluetoothManager
        every { context.applicationContext } returns context
        every { context.getSystemService(Context.WIFI_SERVICE) } returns wifiManager
        every { bluetoothManager.adapter } returns bluetoothAdapter
    }

    @Test
    fun testBluetoothEnabled_returnsTrueWhenAdapterIsEnabled() {
        every { bluetoothAdapter.isEnabled } returns true
        assertTrue(isBluetoothEnabled(context))
    }

    @Test
    fun testBluetoothEnabled_returnsFalseWhenAdapterIsDisabled() {
        every { bluetoothAdapter.isEnabled } returns false
        assertFalse(isBluetoothEnabled(context))
    }

    @Test
    fun testWifiEnabled_returnsTrueWhenWifiIsEnabled() {
        every { wifiManager.isWifiEnabled } returns true
        assertTrue(isWifiEnabled(context))
    }

    @Test
    fun testWifiEnabled_returnsFalseWhenWifiIsDisabled() {
        every { wifiManager.isWifiEnabled } returns false
        assertFalse(isWifiEnabled(context))
    }

    @Test
    fun testMatrixState_BothOff_returnsNotReady() {
        every { bluetoothAdapter.isEnabled } returns false
        every { wifiManager.isWifiEnabled } returns false
        assertFalse(areRadiosAndPermissionsReady(context))
    }

    @Test
    fun testMatrixState_BluetoothOff_WifiOn_returnsNotReady() {
        every { bluetoothAdapter.isEnabled } returns false
        every { wifiManager.isWifiEnabled } returns true
        assertFalse(areRadiosAndPermissionsReady(context))
    }

    @Test
    fun testMatrixState_BluetoothOn_WifiOff_returnsNotReady() {
        every { bluetoothAdapter.isEnabled } returns true
        every { wifiManager.isWifiEnabled } returns false
        assertFalse(areRadiosAndPermissionsReady(context))
    }
}
