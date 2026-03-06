package com.bordrotakip.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Boot receiver for rescheduling notifications after device restart
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // TODO: Bildirim planlamasını yeniden başlat
        }
    }
}
