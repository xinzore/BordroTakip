package com.bordrotakip.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val CHANNEL_REMINDERS = "reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(NotificationManager::class.java) ?: return

        val channel = NotificationChannel(
            CHANNEL_REMINDERS,
            "Hatırlatmalar",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Günlük hatırlatmalar ve bilgilendirmeler"
        }

        manager.createNotificationChannel(channel)
    }
}

