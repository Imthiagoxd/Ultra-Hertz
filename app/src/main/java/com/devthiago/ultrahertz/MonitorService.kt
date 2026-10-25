package com.devthiago.ultrahertz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.TrafficStats
import android.os.Handler
import android.os.Looper
import android.view.Choreographer
import android.view.Gravity
import android.view.WindowManager
import android.widget.TextView
import java.util.Locale

private const val CHANNEL_ID = "ultrahz_monitor"

/** Mostra na tela a taxa real (Hz) e/ou a velocidade da rede. */
class MonitorService : Service() {

    private lateinit var wm: WindowManager
    private val handler = Handler(Looper.getMainLooper())
    private var view: TextView? = null

    private var showHz = true
    private var showNet = false
    private var hzText = "-- Hz"
    private var netText = ""

    private var frames = 0
    private var lastFrameMs = 0L
    private var lastRx = 0L
    private var lastTx = 0L
    private var lastNetMs = 0L

    private val frameCallback = object : Choreographer.FrameCallback {
        override fun doFrame(frameTimeNanos: Long) {
            if (!showHz) return
            frames++
            val now = frameTimeNanos / 1_000_000
            if (lastFrameMs == 0L) lastFrameMs = now
            if (now - lastFrameMs >= Config.MONITOR_UPDATE_MS) {
                val fps = frames * 1000f / (now - lastFrameMs)
                hzText = "${fps.toInt()} Hz"
                frames = 0
                lastFrameMs = now
                render()
            }
            Choreographer.getInstance().postFrameCallback(this)
        }
    }

    private val netTask = object : Runnable {
        override fun run() {
            if (!showNet) return
            val now = System.currentTimeMillis()
            val rx = TrafficStats.getTotalRxBytes()
            val tx = TrafficStats.getTotalTxBytes()
            if (lastNetMs != 0L && now > lastNetMs) {
                val sec = (now - lastNetMs) / 1000f
                val down = ((rx - lastRx) / sec).toLong().coerceAtLeast(0L)
                val up = ((tx - lastTx) / sec).toLong().coerceAtLeast(0L)
                val st = Store(this@MonitorService)
                netText = if (st.netCombined) "⇅ ${fmtNet(down + up, st.netBits)}"
                else "↓ ${fmtNet(down, st.netBits)}  ↑ ${fmtNet(up, st.netBits)}"
                render()
            }
            lastRx = rx
            lastTx = tx
            lastNetMs = now
            handler.postDelayed(this, Config.NET_UPDATE_MS)
        }
    }

    private fun fmt(bytesPerSec: Long): String = when {
        bytesPerSec >= 1_048_576L -> String.format(Locale.US, "%.1f MB/s", bytesPerSec / 1_048_576f)
        bytesPerSec >= 1024L -> "${bytesPerSec / 1024L} KB/s"
        else -> "$bytesPerSec B/s"
    }

    private fun fmtNet(bytesPerSec: Long, bits: Boolean): String {
        if (!bits) return fmt(bytesPerSec)
        val b = bytesPerSec * 8
        return when {
            b >= 1_000_000L -> String.format(Locale.US, "%.1f Mb/s", b / 1_000_000f)
            b >= 1000L -> "${b / 1000L} kb/s"
            else -> "$b b/s"
        }
    }

    private fun render() {
        val parts = mutableListOf<String>()
        if (showHz) parts.add(hzText)
        if (showNet && netText.isNotEmpty()) parts.add(netText)
        view?.text = parts.joinToString("   ")
    }

    private fun buildView() {
        view?.let { try { wm.removeView(it) } catch (_: Throwable) {} }
        view = TextView(this).apply {
            textSize = Config.MONITOR_SIZES[Store(this@MonitorService).monitorSize]
            setTextColor(Config.MONITOR_TEXT_COLOR)
            setBackgroundColor(Config.MONITOR_BG_COLOR)
            setPadding(16, 4, 16, 4)
            text = "-- Hz"
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = (if (Store(this@MonitorService).monitorBottom) Gravity.BOTTOM else Gravity.TOP) or
                Gravity.CENTER_HORIZONTAL
        }
        wm.addView(view, params)
    }

    /** Lê as opções salvas e reinicia o que for preciso. */
    private fun reload() {
        val store = Store(this)
        showHz = store.monitorHz
        showNet = store.netSpeed

        Choreographer.getInstance().removeFrameCallback(frameCallback)
        handler.removeCallbacks(netTask)
        frames = 0
        lastFrameMs = 0L
        lastNetMs = 0L
        netText = ""

        buildView()
        render()
        if (showHz) Choreographer.getInstance().postFrameCallback(frameCallback)
        if (showNet) handler.post(netTask)
    }

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Monitor", NotificationManager.IMPORTANCE_LOW)
        )
        startForeground(
            1,
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Ultra Hertz")
                .setContentText("Monitor ativo")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        reload()
        return START_STICKY
    }

    override fun onDestroy() {
        Choreographer.getInstance().removeFrameCallback(frameCallback)
        handler.removeCallbacksAndMessages(null)
        view?.let { try { wm.removeView(it) } catch (_: Throwable) {} }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        /** Liga, atualiza ou desliga o serviço conforme as opções salvas. */
        fun refresh(ctx: Context) {
            val s = Store(ctx)
            if (s.monitorHz || s.netSpeed) {
                ctx.startForegroundService(Intent(ctx, MonitorService::class.java))
            } else {
                ctx.stopService(Intent(ctx, MonitorService::class.java))
            }
        }
    }
}
