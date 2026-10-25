package com.devthiago.ultrahertz

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.provider.Settings

private const val CHANNEL_ID = "ultrahz_enforcer"

/**
 * Mantém a taxa certa: reaplica quando o sistema mexe nas chaves, quando o brilho muda,
 * quando a economia de energia liga/desliga e quando a tela liga/desliga.
 */
class EnforcerService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val applyTask = Runnable { Engine.applyNow(this) }

    private val heartbeat = object : Runnable {
        override fun run() {
            Engine.applyNow(this@EnforcerService)
            handler.postDelayed(this, Config.HEARTBEAT_MS)
        }
    }

    private fun schedule(delayMs: Long = Config.OBSERVER_DEBOUNCE_MS) {
        handler.removeCallbacks(applyTask)
        handler.postDelayed(applyTask, delayMs)
    }

    private val observer = object : ContentObserver(handler) {
        override fun onChange(selfChange: Boolean, uri: Uri?) {
            schedule()
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            schedule(Config.SCREEN_STATE_DELAY_MS)
            if (i?.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                handler.postDelayed({ Engine.applyNow(this@EnforcerService) }, Config.POWER_SAVE_RECHECK_MS)
            }
            when (i?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    maybeRestartAod()
                    handler.postDelayed(screenOffTask, Config.SCREEN_OFF_ACTION_DELAY_MS)
                }
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    handler.removeCallbacks(screenOffTask)
                    ScreenOffMods.onScreenOn(this@EnforcerService, Store(this@EnforcerService))
                }
            }
        }
    }

    private val busyCameras = HashSet<String>()
    private val cameraCallback = object : CameraManager.AvailabilityCallback() {
        override fun onCameraUnavailable(cameraId: String) {
            busyCameras.add(cameraId)
            CameraState.inUse = true
            schedule(Config.SCREEN_STATE_DELAY_MS)
        }
        override fun onCameraAvailable(cameraId: String) {
            busyCameras.remove(cameraId)
            CameraState.inUse = busyCameras.isNotEmpty()
            schedule(Config.SCREEN_STATE_DELAY_MS)
        }
    }

    private val screenOffTask = Runnable {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive) ScreenOffMods.onScreenOff(this, Store(this))
    }

    private fun maybeRestartAod() {
        val store = Store(this)
        if (!store.aodRestart || !store.situation(Situation.SCREEN_OFF).enabled) return
        handler.postDelayed({ SystemTweaks.restartAod(this) }, Config.SCREEN_STATE_DELAY_MS + 300L)
    }

    override fun onCreate() {
        super.onCreate()

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Taxa de atualização", NotificationManager.IMPORTANCE_MIN)
        )
        startForeground(
            2,
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Ultra Hertz")
                .setContentText("Mantendo a taxa de atualização")
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .build()
        )

        val r = contentResolver
        listOf(Config.SYS_MIN, Config.SYS_PEAK, "screen_brightness", "screen_brightness_float").forEach {
            r.registerContentObserver(Settings.System.getUriFor(it), false, observer)
        }
        r.registerContentObserver(Settings.Secure.getUriFor(Config.SAMSUNG_MODE), false, observer)
        listOf("psm_refresh_rate_tag", "pms_settings_refresh_rate_enabled", "low_power").forEach {
            r.registerContentObserver(Settings.Global.getUriFor(it), false, observer)
        }

        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        })

        try {
            (getSystemService(Context.CAMERA_SERVICE) as CameraManager)
                .registerAvailabilityCallback(cameraCallback, handler)
        } catch (_: Throwable) {}

        // sobrou algo de uma tela apagada anterior? desfaz
        if ((getSystemService(Context.POWER_SERVICE) as PowerManager).isInteractive) {
            ScreenOffMods.onScreenOn(this, Store(this))
        }

        Engine.applyNow(this)
        handler.postDelayed(heartbeat, Config.HEARTBEAT_MS)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_STICKY

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(observer)
        unregisterReceiver(receiver)
        try {
            (getSystemService(Context.CAMERA_SERVICE) as CameraManager).unregisterAvailabilityCallback(cameraCallback)
        } catch (_: Throwable) {}
        CameraState.inUse = false
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?) = null

    companion object {
        fun start(ctx: Context) = ctx.startForegroundService(Intent(ctx, EnforcerService::class.java))
        fun stop(ctx: Context) = ctx.stopService(Intent(ctx, EnforcerService::class.java))
    }
}
