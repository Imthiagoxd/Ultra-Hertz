package com.devthiago.ultrahertz

import android.content.Context
import android.os.PowerManager
import android.provider.Settings

/** Decide qual perfil de Hz vale agora (app > situação > modo geral). */
object Engine {

    /** Brilho em % (0–100). */
    fun brightnessPercent(ctx: Context): Float {
        return try {
            val r = ctx.contentResolver
            val f = Settings.System.getFloat(r, "screen_brightness_float", -1f)
            if (f >= 0f) f * 100f
            else Settings.System.getFloat(r, "screen_brightness", 125f) / 255f * 100f
        } catch (_: Throwable) { 100f }
    }

    fun activeSituation(ctx: Context, store: Store): Situation {
        val pm = ctx.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isInteractive && store.situation(Situation.SCREEN_OFF).enabled &&
            !(store.aodIgnoreCharging && SystemTweaks.isCharging(ctx))
        ) return Situation.SCREEN_OFF
        if (pm.isPowerSaveMode && store.situation(Situation.POWER_SAVER).enabled) return Situation.POWER_SAVER
        if (store.situation(Situation.LOW_BRIGHTNESS).enabled &&
            brightnessPercent(ctx) < store.lbThreshold
        ) return Situation.LOW_BRIGHTNESS
        return Situation.NORMAL
    }

    fun compute(ctx: Context, store: Store): Profile {
        val s = activeSituation(ctx, store)

        if (s == Situation.SCREEN_OFF) {
            val hz = store.situation(s).highHz
            return Profile(Mode.ADAPTIVE, hz, hz, s.label)
        }

        val pkg = ForegroundApp.pkg
        if (store.perAppEnabled && pkg != null) {
            val rule = store.appRule(pkg)
            if (rule != null) return withCamera(store, Profile(rule.mode, rule.min, rule.max, "App $pkg"))
        }

        val cfg = store.situation(s)
        val base = when (store.mode) {
            Mode.STANDARD -> Profile(Mode.STANDARD, Config.STANDARD_HZ, Config.STANDARD_HZ, s.label)
            Mode.ADAPTIVE -> Profile(Mode.ADAPTIVE, cfg.adpMin, cfg.adpMax, s.label)
            Mode.HIGH -> Profile(Mode.HIGH, cfg.highHz, cfg.highHz, s.label)
        }
        return withCamera(store, base)
    }

    /** Câmera aberta: no Adaptativo o mínimo sobe para CAMERA_MIN_HZ (a câmera fica fluida). */
    private fun withCamera(store: Store, p: Profile): Profile {
        if (!store.cameraBoost || !CameraState.inUse || p.mode != Mode.ADAPTIVE) return p
        val min = maxOf(p.min, minOf(Config.CAMERA_MIN_HZ, p.max))
        return if (min == p.min) p else Profile(p.mode, min, p.max, p.source + " + câmera")
    }

    /** Calcula e grava agora. Devolve o perfil, ou null se o interruptor principal está desligado. */
    fun applyNow(ctx: Context): Profile? {
        val store = Store(ctx)
        if (!store.enabled) return null
        val p = compute(ctx, store)
        RefreshRateManager(ctx).applyProfile(p)
        return p
    }
}
