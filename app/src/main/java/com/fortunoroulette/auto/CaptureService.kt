package com.fortuneroulette.auto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView

class CaptureService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: TextView? = null

    override fun onCreate() {
        super.onCreate()

        val channelId = "fortune_capture"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fortune Roulette",
                NotificationManager.IMPORTANCE_LOW
            )

            val manager =
                getSystemService(NotificationManager::class.java)

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

        criarJanelaFlutuante()
    }

    private fun criarJanelaFlutuante() {

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val texto = TextView(this)

        texto.text = "FORTUNE\nCAPTURA ATIVA"
        texto.textSize = 14f
        texto.setTextColor(Color.WHITE)
        texto.setBackgroundColor(Color.argb(220, 0, 120, 0))
        texto.setPadding(20, 15, 20, 15)

        val tipoJanela =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                WindowManager.LayoutParams.TYPE_PHONE
            }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            tipoJanela,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP or Gravity.START
        params.x = 20
        params.y = 120

        windowManager.addView(texto, params)

        floatingView = texto
    }

    override fun onDestroy() {
        super.onDestroy()

        floatingView?.let {
            windowManager.removeView(it)
        }

        floatingView = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
