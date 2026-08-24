package com.fortuneroulette.auto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder

class CaptureService : Service() {

    override fun onCreate() {
        super.onCreate()

        val channelId = "fortune_capture"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fortune Roulette",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setContentTitle("Fortune Roulette")
                .setContentText("Captura automática ativa")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
        } else {
            Notification.Builder(this)
                .setContentTitle("Fortune Roulette")
                .setContentText("Captura automática ativa")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
        }

        startForeground(1, notification)
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
