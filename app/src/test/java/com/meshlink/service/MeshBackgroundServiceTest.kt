package com.meshlink.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.meshlink.ui.components.isBluetoothEnabled
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class MeshBackgroundServiceTest {

    @Test
    fun testServiceStates() {
        val states = MeshBackgroundService.ServiceState.values()
        assertEquals(5, states.size)
        assertEquals(MeshBackgroundService.ServiceState.STOPPED, MeshBackgroundService.ServiceState.valueOf("STOPPED"))
    }

    @Test
    fun `isBluetoothEnabled safely catches SecurityException when permission is revoked`() {
        val context = mockk<Context>(relaxed = true)
        val btManager = mockk<BluetoothManager>()
        val btAdapter = mockk<BluetoothAdapter>()
        every { context.getSystemService(Context.BLUETOOTH_SERVICE) } returns btManager
        every { btManager.adapter } returns btAdapter
        every { btAdapter.isEnabled } throws SecurityException("Need BLUETOOTH_CONNECT permission")

        assertFalse(isBluetoothEnabled(context))
    }
}
