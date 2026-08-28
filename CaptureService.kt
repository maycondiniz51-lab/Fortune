package com.fortuneroulette.auto

import android.app.*
import android.content.*
import android.graphics.*
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.*
import android.view.*
import android.widget.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.math.roundToInt

class CaptureService : Service() {
    private lateinit var projection: MediaProjection
    private lateinit var reader: ImageReader
    private var display: VirtualDisplay? = null
    private lateinit var wm: WindowManager
    private lateinit var panel: TextView
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private var lastCandidate = -1
    private var stableCount = 0
    private var lastAccepted = -1
    private var lastAcceptedAt = 0L

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForeground(7, notification())
        createOverlay()
    }

    override fun onStartCommand(intent: Intent?, flags:Int, startId:Int):Int {
        val resultCode=intent?.getIntExtra("resultCode", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val data=intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY
        val mgr=getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        projection=mgr.getMediaProjection(resultCode,data)

        val metrics=resources.displayMetrics
        val w=metrics.widthPixels
        val h=metrics.heightPixels
        reader=ImageReader.newInstance(w,h,PixelFormat.RGBA_8888,2)
        display=projection.createVirtualDisplay(
            "FortuneRouletteCapture",w,h,metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            reader.surface,null,null
        )
        reader.setOnImageAvailableListener({ r ->
            val image=r.acquireLatestImage() ?: return@setOnImageAvailableListener
            try { process(image) } finally { image.close() }
        }, Handler(Looper.getMainLooper()))

        panel.text="FORTUNE\nCaptura ativa\nLendo resultado..."
        return START_STICKY
    }

    private fun process(image:Image) {
        val plane=image.planes[0]
        val buffer=plane.buffer
        val pixelStride=plane.pixelStride
        val rowStride=plane.rowStride
        val width=image.width
        val height=image.height
        val rowPadding=rowStride-pixelStride*width
        val bmp=Bitmap.createBitmap(
            width+rowPadding/pixelStride,height,Bitmap.Config.ARGB_8888
        )
        bmp.copyPixelsFromBuffer(buffer)

        val mainCrop=cropNormalized(bmp,.40f,.28f,.60f,.38f)
        val historyCrop=cropNormalized(bmp,.00f,.93f,.95f,.985f)
        recognize(mainCrop)
        recognize(historyCrop)
        mainCrop.recycle()
        historyCrop.recycle()
        bmp.recycle()
    }

    private fun cropNormalized(b:Bitmap,l:Float,t:Float,r:Float,bt:Float):Bitmap {
        val x=(b.width*l).roundToInt().coerceIn(0,b.width-1)
        val y=(b.height*t).roundToInt().coerceIn(0,b.height-1)
        val rr=(b.width*r).roundToInt().coerceIn(x+1,b.width)
        val bb=(b.height*bt).roundToInt().coerceIn(y+1,b.height)
        return Bitmap.createBitmap(b,x,y,rr-x,bb-y)
    }

    private fun recognize(bmp:Bitmap) {
        recognizer.process(InputImage.fromBitmap(bmp,0)).addOnSuccessListener { result ->
            val nums=Regex("""(?<!\d)(3[0-6]|[12]?\d|0)(?!\d)""")
                .findAll(result.text)
                .mapNotNull { it.value.toIntOrNull() }
                .filter { it in 0..36 }
                .toList()
            nums.firstOrNull()?.let { acceptCandidate(it) }
        }
    }

    private fun acceptCandidate(n:Int) {
        if(n==lastCandidate) stableCount++ else {
            lastCandidate=n
            stableCount=1
        }
        if(stableCount>=3 && (n!=lastAccepted ||
            System.currentTimeMillis()-lastAcceptedAt>2500)) {
            lastAccepted=n
            lastAcceptedAt=System.currentTimeMillis()
            panel.text="FORTUNE\nNÚMERO DETECTADO: $n"
            // Ponto de integração com o analisador HTML/JS.
        }
    }

    private fun createOverlay() {
        panel=TextView(this).apply {
            text="FORTUNE\nAguardando captura..."
            textSize=15f
            setTextColor(Color.WHITE)
            setBackgroundColor(0xAA101216.toInt())
            setPadding(18,14,18,14)
        }
        wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val lp=WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        lp.gravity=Gravity.TOP or Gravity.START
        lp.x=18; lp.y=120
        wm.addView(panel,lp)
    }

    private fun createChannel() {
        if(Build.VERSION.SDK_INT>=26) {
            val ch=NotificationChannel("capture","Captura Fortune",NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun notification():Notification =
        Notification.Builder(this,"capture")
            .setContentTitle("Fortune Roulette")
            .setContentText("Captura automática ativa")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .build()

    override fun onDestroy() {
        display?.release()
        if(::reader.isInitialized) reader.close()
        if(::projection.isInitialized) projection.stop()
        recognizer.close()
        if(::panel.isInitialized) wm.removeView(panel)
        super.onDestroy()
    }
    override fun onBind(intent:Intent?):IBinder?=null
}
