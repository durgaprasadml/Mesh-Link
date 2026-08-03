package com.meshlink.alarm

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class EmergencyAlarmManagerTest {

    private val context: Context = mockk(relaxed = true)

    @Before
    fun setup() {
        EmergencyAlarmManager.stopAlarm(context)
    }

    @Test
    fun `initial alarm playing state is false`() {
        assertFalse(EmergencyAlarmManager.isAlarmPlaying.value)
    }

    @Test
    fun `stopAlarm when not playing does not throw exception and keeps state false`() {
        EmergencyAlarmManager.stopAlarm(context)
        assertFalse(EmergencyAlarmManager.isAlarmPlaying.value)
    }
}
