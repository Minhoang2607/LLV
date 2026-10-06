package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Watchdog Alarm Receiver that ensures periodic checks continue even under Doze mode
 * or if the service process was temporarily reclaimed by OS memory management.
 */
class AlarmSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (BackgroundSyncService.isEnabledInPrefs(context)) {
            val interval = BackgroundSyncService.getSavedIntervalSeconds(context)
            BackgroundSyncService.startService(context, interval)
        }
    }
}
