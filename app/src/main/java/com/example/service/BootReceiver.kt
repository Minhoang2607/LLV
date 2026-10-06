package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Automatically resumes background monitoring when the phone finishes booting.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            if (BackgroundSyncService.isEnabledInPrefs(context)) {
                val interval = BackgroundSyncService.getSavedIntervalSeconds(context)
                BackgroundSyncService.startService(context, interval)
            }
        }
    }
}
