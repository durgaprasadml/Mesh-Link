package com.meshlink.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.meshlink.common.logger.MeshLogger

class EmergencyAlarmReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_STOP_ALARM = "com.meshlink.alarm.ACTION_STOP_ALARM"
        const val ACTION_ACKNOWLEDGE = "com.meshlink.alarm.ACTION_ACKNOWLEDGE"
        const val ACTION_DISMISS = "com.meshlink.alarm.ACTION_DISMISS"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        MeshLogger.d("EmergencyAlarmReceiver", "Received action: ${intent?.action}")
        when (intent?.action) {
            ACTION_STOP_ALARM, ACTION_ACKNOWLEDGE, ACTION_DISMISS -> {
                EmergencyAlarmManager.stopAlarm(context)
            }
        }
    }
}
