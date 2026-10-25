package com.devthiago.ultrahertz

import android.content.Context
import android.content.SharedPreferences

/** Tudo que o usuário escolhe, guardado em SharedPreferences. */
class Store(context: Context) {

    private val p: SharedPreferences =
        context.applicationContext.getSharedPreferences(Config.PREFS, Context.MODE_PRIVATE)

    // ---------------- Geral ----------------

    var enabled: Boolean
        get() = p.getBoolean("enabled", true)
        set(v) { p.edit().putBoolean("enabled", v).apply() }

    var mode: Mode
        get() = try {
            Mode.valueOf(p.getString("mode", Mode.ADAPTIVE.name) ?: Mode.ADAPTIVE.name)
        } catch (_: Exception) { Mode.ADAPTIVE }
        set(v) { p.edit().putString("mode", v.name).apply() }

    var lastLog: String
        get() = p.getString("last_log", "Nenhuma aplicação ainda") ?: ""
        set(v) { p.edit().putString("last_log", v).apply() }

    // ---------------- Situações ----------------

    private fun defaults(s: Situation): SituationCfg = when (s) {
        Situation.NORMAL -> SituationCfg(true, Config.DEFAULT_MIN_HZ, Config.DEFAULT_MAX_HZ, Config.DEFAULT_MAX_HZ)
        Situation.POWER_SAVER -> SituationCfg(false, Config.PSM_MIN_HZ, Config.PSM_MAX_HZ, Config.PSM_MAX_HZ)
        Situation.LOW_BRIGHTNESS -> SituationCfg(false, Config.LB_MIN_HZ, Config.LB_MAX_HZ, Config.LB_MAX_HZ)
        Situation.SCREEN_OFF -> SituationCfg(false, Config.SCREEN_OFF_HZ, Config.SCREEN_OFF_HZ, Config.SCREEN_OFF_HZ)
    }

    fun situation(s: Situation): SituationCfg {
        val d = defaults(s)
        val k = "s_${s.name}_"
        return SituationCfg(
            if (s == Situation.NORMAL) true else p.getBoolean(k + "en", d.enabled),
            p.getFloat(k + "amin", d.adpMin),
            p.getFloat(k + "amax", d.adpMax),
            p.getFloat(k + "hi", d.highHz)
        )
    }

    fun setSituation(s: Situation, c: SituationCfg) {
        val k = "s_${s.name}_"
        p.edit()
            .putBoolean(k + "en", c.enabled)
            .putFloat(k + "amin", c.adpMin)
            .putFloat(k + "amax", c.adpMax)
            .putFloat(k + "hi", c.highHz)
            .apply()
    }

    var lbThreshold: Int
        get() = p.getInt("lb_threshold", Config.LB_THRESHOLD_PERCENT)
        set(v) { p.edit().putInt("lb_threshold", v).apply() }

    var aodRestart: Boolean
        get() = p.getBoolean("aod_restart", false)
        set(v) { p.edit().putBoolean("aod_restart", v).apply() }

    var cameraBoost: Boolean
        get() = p.getBoolean("camera_boost", true)
        set(v) { p.edit().putBoolean("camera_boost", v).apply() }

    var preventPsm: Boolean
        get() = p.getBoolean("prevent_psm", Config.DISABLE_PSM_REFRESH_LIMIT)
        set(v) { p.edit().putBoolean("prevent_psm", v).apply() }

    var aodIgnoreCharging: Boolean
        get() = p.getBoolean("aod_ignore_charging", true)
        set(v) { p.edit().putBoolean("aod_ignore_charging", v).apply() }

    // ---------------- Ao desligar a tela ----------------

    var psmOnScreenOff: Boolean
        get() = p.getBoolean("psm_off", false)
        set(v) { p.edit().putBoolean("psm_off", v).apply() }

    var psmIgnoreCharging: Boolean
        get() = p.getBoolean("psm_off_ign", true)
        set(v) { p.edit().putBoolean("psm_off_ign", v).apply() }

    var syncOff: Boolean
        get() = p.getBoolean("sync_off", false)
        set(v) { p.edit().putBoolean("sync_off", v).apply() }

    var syncIgnoreCharging: Boolean
        get() = p.getBoolean("sync_off_ign", true)
        set(v) { p.edit().putBoolean("sync_off_ign", v).apply() }

    /** Marcas internas: só desfazemos o que nós mesmos ligamos/desligamos. */
    var psmByUs: Boolean
        get() = p.getBoolean("psm_by_us", false)
        set(v) { p.edit().putBoolean("psm_by_us", v).apply() }

    var syncByUs: Boolean
        get() = p.getBoolean("sync_by_us", false)
        set(v) { p.edit().putBoolean("sync_by_us", v).apply() }

    /** 0 = automático, 1 = forçar Adaptativo disponível, 2 = esconder Adaptativo */
    var adaptiveOverride: Int
        get() = p.getInt("adaptive_override", 0).coerceIn(0, 2)
        set(v) { p.edit().putInt("adaptive_override", v).apply() }

    // ---------------- Tema e rede ----------------

    /** 0 = sistema, 1 = claro, 2 = escuro */
    var themeMode: Int
        get() = p.getInt("theme_mode", 0).coerceIn(0, 2)
        set(v) { p.edit().putInt("theme_mode", v).apply() }

    var netBits: Boolean
        get() = p.getBoolean("net_bits", false)
        set(v) { p.edit().putBoolean("net_bits", v).apply() }

    var netCombined: Boolean
        get() = p.getBoolean("net_combined", false)
        set(v) { p.edit().putBoolean("net_combined", v).apply() }

    // ---------------- Monitor ----------------

    var monitorBottom: Boolean
        get() = p.getBoolean("monitor_bottom", false)
        set(v) { p.edit().putBoolean("monitor_bottom", v).apply() }

    var monitorSize: Int
        get() = p.getInt("monitor_size", 1).coerceIn(0, Config.MONITOR_SIZES.size - 1)
        set(v) { p.edit().putInt("monitor_size", v).apply() }

    var monitorHz: Boolean
        get() = p.getBoolean("monitor_hz", false)
        set(v) { p.edit().putBoolean("monitor_hz", v).apply() }

    var netSpeed: Boolean
        get() = p.getBoolean("net_speed", false)
        set(v) { p.edit().putBoolean("net_speed", v).apply() }

    // ---------------- Taxa por app ----------------

    var perAppEnabled: Boolean
        get() = p.getBoolean("per_app", true)
        set(v) { p.edit().putBoolean("per_app", v).apply() }

    fun appRule(pkg: String): AppRule? {
        val s = p.getString("app_$pkg", null) ?: return null
        val parts = s.split("|")
        if (parts.size != 3) return null
        return try {
            AppRule(Mode.valueOf(parts[0]), parts[1].toFloat(), parts[2].toFloat())
        } catch (_: Exception) { null }
    }

    fun setAppRule(pkg: String, r: AppRule?) {
        val set = (p.getStringSet("app_pkgs", emptySet<String>()) ?: emptySet<String>()).toMutableSet()
        val e = p.edit()
        if (r == null) {
            set.remove(pkg)
            e.remove("app_$pkg")
        } else {
            set.add(pkg)
            e.putString("app_$pkg", "${r.mode.name}|${r.min}|${r.max}")
        }
        e.putStringSet("app_pkgs", set).apply()
    }

    fun ruleCount(): Int = p.getStringSet("app_pkgs", emptySet<String>())?.size ?: 0

    // ---------------- Valores aprendidos do sistema ----------------

    fun learnedRrm(m: Mode): Int = p.getInt("rrm_${m.name}", -1)

    fun setLearnedRrm(m: Mode, v: Int) { p.edit().putInt("rrm_${m.name}", v).apply() }

    fun clearLearnedRrm() {
        val e = p.edit()
        Mode.values().forEach { e.remove("rrm_${it.name}") }
        e.apply()
    }

    fun accepted(key: String): List<Float> =
        (p.getString("accepted_$key", "") ?: "").split(",").mapNotNull { it.toFloatOrNull() }

    fun setAccepted(key: String, values: List<Float>) {
        p.edit().putString("accepted_$key", values.joinToString(",")).apply()
    }
}
