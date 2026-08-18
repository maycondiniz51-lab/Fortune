package com.fortuneroulette.auto

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.widget.*

class MainActivity : Activity() {
    private val REQ_CAPTURE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val title = TextView(this).apply {
            text = "Fortune Roulette — Auto Captura"
            textSize = 20f
            setPadding(24,24,24,12)
        }
        val info = TextView(this).apply {
            text = "Regiões OCR configuradas a partir da captura enviada: número grande da roleta e primeiro resultado do histórico inferior."
            setPadding(24,8,24,20)
        }
        val overlay = Button(this).apply {
            text = "Permitir janela flutuante"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")))
            }
        }
        val start = Button(this).apply {
            text = "Iniciar captura automática"
            setOnClickListener {
                val mgr = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                startActivityForResult(mgr.createScreenCaptureIntent(), REQ_CAPTURE)
            }
        }
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(title); addView(info); addView(overlay); addView(start)
        })
    }

    override fun onActivityResult(requestCode:Int, resultCode:Int, data:Intent?) {
        super.onActivityResult(requestCode,resultCode,data)
        if(requestCode==REQ_CAPTURE && resultCode==RESULT_OK && data!=null) {
            val i=Intent(this,CaptureService::class.java)
            i.putExtra("resultCode",resultCode)
            i.putExtra("data",data)
            startForegroundService(i)
        }
    }
}
