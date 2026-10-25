package com.devthiago.ultrahertz

import android.content.ComponentName
import android.content.Context
import android.content.ContentResolver
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/** Ajustes extras do sistema (animações, acessibilidade, AOD). Precisam de WRITE_SECURE_SETTINGS. */
object SystemTweaks {

    private val animKeys = listOf(
        "animator_duration_scale",
        "window_animation_scale",
        "transition_animation_scale"
    )

    // ---------------- Animações ----------------

    fun animationScale(ctx: Context): Float = try {
        Settings.Global.getFloat(ctx.contentResolver, animKeys[0], 1f)
    } catch (_: Throwable) { 1f }

    /** Devolve null se deu certo, ou o texto do erro. */
    fun setAnimationScale(ctx: Context, v: Float): String? = try {
        animKeys.forEach { Settings.Global.putFloat(ctx.contentResolver, it, v) }
        null
    } catch (e: Throwable) {
        "${e.javaClass.simpleName}: ${e.message}"
    }

    // ---------------- Acessibilidade (taxa por app) ----------------

    private fun component(ctx: Context) = ComponentName(ctx, AppWatcherService::class.java)

    private fun listHas(list: String?, cn: ComponentName): Boolean =
        (list ?: "").split(":")
            .filter { it.isNotEmpty() }
            .any { ComponentName.unflattenFromString(it) == cn }

    fun accessibilityEnabled(ctx: Context): Boolean = try {
        listHas(Settings.Secure.getString(ctx.contentResolver, "enabled_accessibility_services"), component(ctx))
    } catch (_: Throwable) { false }

    /** Liga o serviço sem abrir as configurações. Devolve null se deu certo, ou o erro. */
    fun enableAccessibility(ctx: Context): String? = try {
        val r = ctx.contentResolver
        val cn = component(ctx)
        val cur = Settings.Secure.getString(r, "enabled_accessibility_services")
        if (!listHas(cur, cn)) {
            val flat = cn.flattenToString()
            val novo = if (cur.isNullOrEmpty()) flat else "$cur:$flat"
            Settings.Secure.putString(r, "enabled_accessibility_services", novo)
        }
        Settings.Secure.putInt(r, "accessibility_enabled", 1)
        null
    } catch (e: Throwable) {
        "${e.javaClass.simpleName}: ${e.message}"
    }

    // ---------------- AOD ----------------

    /** Desliga e religa o AOD para o sistema reavaliar a taxa (experimental). */
    fun restartAod(ctx: Context) {
        try {
            val r = ctx.contentResolver
            if (Settings.System.getString(r, "aod_mode") != "1") return
            Settings.System.putString(r, "aod_mode", "0")
            Handler(Looper.getMainLooper()).postDelayed({
                try { Settings.System.putString(r, "aod_mode", "1") } catch (_: Throwable) {}
            }, Config.AOD_RESTART_DELAY_MS)
        } catch (_: Throwable) {}
    }

    // ---------------- Extras do sistema ----------------

    fun forceResizable(ctx: Context): Boolean = try {
        Settings.Global.getInt(ctx.contentResolver, "force_resizable_activities", 0) == 1
    } catch (_: Throwable) { false }

    fun setForceResizable(ctx: Context, on: Boolean): String? = try {
        Settings.Global.putInt(ctx.contentResolver, "force_resizable_activities", if (on) 1 else 0)
        null
    } catch (e: Throwable) { "${e.javaClass.simpleName}: ${e.message}" }

    /** "Proteger bateria" da Samsung (limita a carga a 85%). */
    fun protectBattery(ctx: Context): Boolean = try {
        Settings.Global.getInt(ctx.contentResolver, "protect_battery", 0) == 1
    } catch (_: Throwable) { false }

    fun setProtectBattery(ctx: Context, on: Boolean): String? = try {
        Settings.Global.putInt(ctx.contentResolver, "protect_battery", if (on) 1 else 0)
        null
    } catch (e: Throwable) { "${e.javaClass.simpleName}: ${e.message}" }

    // ---------------- Bateria ----------------

    private fun batteryIntent(ctx: Context): Intent? =
        ctx.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

    fun isCharging(ctx: Context): Boolean = try {
        (batteryIntent(ctx)?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0) != 0
    } catch (_: Throwable) { false }

    fun batteryInfo(ctx: Context): String {
        val i: Intent = (try { batteryIntent(ctx) } catch (_: Throwable) { null }) ?: return "Sem dados da bateria"
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        val temp = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)
        val volt = i.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -1)
        val cycles = i.getIntExtra("android.os.extra.CYCLE_COUNT", -1)
        val health = when (i.getIntExtra(BatteryManager.EXTRA_HEALTH, 0)) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Boa"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Superaquecida"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Ruim"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Sobretensão"
            BatteryManager.BATTERY_HEALTH_COLD -> "Fria"
            BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Falha"
            else -> "Desconhecida"
        }
        val status = when (i.getIntExtra(BatteryManager.EXTRA_STATUS, 0)) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Carregando"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Descarregando"
            BatteryManager.BATTERY_STATUS_FULL -> "Cheia"
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Sem carregar"
            else -> "Desconhecido"
        }
        val sb = StringBuilder()
        sb.append("Nível: ${if (level >= 0) level * 100 / scale else -1}%\n")
        sb.append("Estado: $status\n")
        sb.append("Saúde: $health\n")
        if (temp >= 0) sb.append("Temperatura: ${temp / 10f} °C\n")
        if (volt >= 0) sb.append("Tensão: $volt mV\n")
        sb.append("Tecnologia: ${i.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "-"}\n")
        sb.append("Ciclos de carga: ${if (cycles >= 0) cycles.toString() else "indisponível"}")
        return sb.toString()
    }

    // ---------------- Reiniciar mods ----------------

    /** Desfaz o que o app mexeu (animações, redimensionar, proteger bateria, tela desligada). */
    fun resetMods(ctx: Context, store: Store): String {
        val out = StringBuilder()
        setAnimationScale(ctx, 1f)?.let { out.append("animações: $it\n") }
        setForceResizable(ctx, false)?.let { out.append("redimensionar: $it\n") }
        setProtectBattery(ctx, false)?.let { out.append("proteger bateria: $it\n") }
        ScreenOffMods.onScreenOn(ctx, store)
        store.psmOnScreenOff = false
        store.syncOff = false
        store.monitorHz = false
        store.netSpeed = false
        MonitorService.refresh(ctx)
        return if (out.isEmpty()) "Mods reiniciados" else "Reiniciado, com avisos:\n" + out.toString().trim()
    }
}
