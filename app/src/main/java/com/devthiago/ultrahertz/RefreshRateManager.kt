package com.devthiago.ultrahertz

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.Display
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** Fala com o sistema: detecta o painel e grava as configurações de taxa. */
class RefreshRateManager(private val context: Context) {

    val store = Store(context)
    private val resolver: ContentResolver get() = context.contentResolver
    private val systemUri: Uri = Uri.parse("content://settings/system")
    private val probeValues = listOf(0f, 1f, 3f, 10f, 24f, 30f, 48f, 60f, 80f, 96f, 120f)

    fun hasPermission(): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    // ---------------- Detecção do aparelho ----------------

    private fun display(): Display? =
        (context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager)
            .getDisplay(Display.DEFAULT_DISPLAY)

    fun currentDisplayHz(): Float = display()?.refreshRate ?: 0f

    private fun floatingString(key: String): String? = try {
        val c = Class.forName("com.samsung.android.feature.SemFloatingFeature")
        val inst = c.getMethod("getInstance").invoke(null)
        c.getMethod("getString", String::class.java).invoke(inst, key) as? String
    } catch (_: Throwable) { null }

    /** Taxas que o painel informa (modos de tela + flags da Samsung). */
    fun detectedRates(): List<Float> {
        val set = sortedSetOf<Float>()
        display()?.supportedModes?.forEach {
            val v = Math.round(it.refreshRate).toFloat()
            if (v >= Config.MIN_SELECTABLE_HZ) set.add(v)
        }
        Config.FLOATING_KEYS.forEach { key ->
            floatingString(key)?.let { s ->
                Regex("\\d+").findAll(s).forEach { m ->
                    val v = m.value.toFloat()
                    if (v in Config.MIN_SELECTABLE_HZ..240f) set.add(v)
                }
            }
        }
        return set.toList()
    }

    /** O painel tem modo Adaptativo (taxa variável até ~30 Hz ou menos)? */
    fun adaptiveCapable(): Boolean = when (store.adaptiveOverride) {
        1 -> true
        2 -> false
        else -> (detectedRates().firstOrNull() ?: 60f) <= Config.ADAPTIVE_LOWEST_HZ
    }

    /** Lista usada nos seletores: só taxas que fazem sentido neste aparelho. */
    fun supportedRates(): List<Float> {
        val det = detectedRates()
        if (!adaptiveCapable()) return if (det.isEmpty()) Config.FALLBACK_RATES else det
        val top = det.maxOrNull() ?: 120f
        return (det + Config.LTPO_RATES.filter { it <= top }).distinct().sorted()
    }

    // ---------------- Valor de refresh_rate_mode por modo ----------------

    private fun hasThreeModes() = adaptiveCapable()

    fun rrmFor(m: Mode): Int {
        val saved = store.learnedRrm(m)
        if (saved >= 0) return saved
        return if (hasThreeModes()) {
            when (m) { Mode.STANDARD -> 0; Mode.ADAPTIVE -> 1; Mode.HIGH -> 2 }
        } else {
            when (m) { Mode.STANDARD -> 0; else -> 1 }
        }
    }

    fun currentRrm(): Int = try {
        Settings.Secure.getInt(resolver, Config.SAMSUNG_MODE, -1)
    } catch (_: Exception) { -1 }

    /** Guarda o valor atual do sistema como o valor do modo informado. */
    fun learn(m: Mode): Boolean {
        val v = currentRrm()
        if (v < 0) return false
        store.setLearnedRrm(m, v)
        return true
    }

    fun resetLearned() = store.clearLearnedRrm()

    // ---------------- Escrita robusta ----------------

    class WriteResult(val ok: Boolean, val method: String, val detail: String)

    private fun errText(e: Throwable): String {
        var root: Throwable = e
        while (root.cause != null && root.cause !== root) root = root.cause!!
        val base = "${e.javaClass.simpleName}: ${e.message?.take(220)}"
        return if (root !== e) "$base ← ${root.javaClass.simpleName}: ${root.message?.take(120)}" else base
    }

    /** Grava um float em Settings.System tentando 4 caminhos. */
    fun writeSystemFloat(key: String, value: Float): WriteResult {
        val attempts: List<Pair<String, () -> Unit>> = listOf(
            "putFloat" to { Settings.System.putFloat(resolver, key, value); Unit },
            "putString($value)" to {
                if (!Settings.System.putString(resolver, key, value.toString())) error("retornou false")
            },
            "putString(${value.toInt()})" to {
                if (!Settings.System.putString(resolver, key, value.toInt().toString())) error("retornou false")
            },
            "insert" to {
                val cv = ContentValues().apply { put("name", key); put("value", value.toString()) }
                resolver.insert(systemUri, cv) ?: error("insert retornou null")
                Unit
            }
        )
        val errors = StringBuilder()
        for ((name, block) in attempts) {
            try { block(); return WriteResult(true, name, "") }
            catch (e: Throwable) { errors.append("[$name] ${errText(e)} ") }
        }
        return WriteResult(false, "nenhum", errors.toString().trim())
    }

    private fun readFloat(key: String): Float = try {
        Settings.System.getFloat(resolver, key, -1f)
    } catch (_: Throwable) { -1f }

    /** Se a sondagem mostrou que o valor é recusado, usa o aceito mais próximo. */
    private fun snap(key: String, v: Float, preferUp: Boolean): Float {
        val ok = store.accepted(key)
        if (ok.isEmpty() || v in ok) return v
        val candidates = if (preferUp) ok.filter { it >= v } else ok.filter { it <= v }
        return if (preferUp) (candidates.minOrNull() ?: ok.maxOrNull()!!)
        else (candidates.maxOrNull() ?: ok.minOrNull()!!)
    }

    // ---------------- Aplicação ----------------

    /** Grava o perfil no sistema. Só escreve o que mudou (evita laços com os observadores). */
    fun applyProfile(requested: Profile): Boolean {
        // aparelho sem Adaptativo: usa a taxa alta fixa
        val p = if (requested.mode == Mode.ADAPTIVE && !adaptiveCapable())
            Profile(Mode.HIGH, requested.max, requested.max, requested.source)
        else requested
        if (!hasPermission()) {
            store.lastLog = "Sem permissão WRITE_SECURE_SETTINGS"
            return false
        }
        val lowest = supportedRates().firstOrNull() ?: Config.DEFAULT_MIN_HZ
        val peak = p.max
        // Mínimo no piso do painel vira 0 e o sistema desce o quanto o painel permitir
        val min = if (p.mode == Mode.ADAPTIVE && p.min <= lowest) 0f else p.min
        val rrm = rrmFor(p.mode)
        val samsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)

        val log = StringBuilder()
        var ok = true
        var changed = false

        if (currentRrm() != rrm) {
            changed = true
            try {
                Settings.Secure.putInt(resolver, Config.SAMSUNG_MODE, rrm)
                log.append("refresh_rate_mode=$rrm ok\n")
            } catch (e: Throwable) {
                ok = false
                log.append("refresh_rate_mode ERRO ${errText(e)}\n")
            }
        }

        if (samsung && store.preventPsm) {
            try {
                val a = Settings.Global.getString(resolver, "pms_settings_refresh_rate_enabled")
                val b = Settings.Global.getString(resolver, "psm_refresh_rate_tag")
                if (a != "0" || b != "0") {
                    changed = true
                    Settings.Global.putString(resolver, "pms_settings_refresh_rate_enabled", "0")
                    Settings.Global.putString(resolver, "psm_refresh_rate_tag", "0")
                    log.append("limite da economia de energia desligado\n")
                }
            } catch (e: Throwable) {
                ok = false
                log.append("economia de energia ERRO ${errText(e)}\n")
            }
        }

        fun write(key: String, wanted: Float, preferUp: Boolean) {
            val v = snap(key, wanted, preferUp)
            if (abs(readFloat(key) - v) < 0.01f) return
            changed = true
            val adj = if (v != wanted) " (ajustado de ${wanted.toInt()})" else ""
            val r = writeSystemFloat(key, v)
            if (r.ok) log.append("$key=${v.toInt()}$adj ok via ${r.method}\n")
            else { ok = false; log.append("$key=${v.toInt()}$adj ERRO ${r.detail}\n") }
        }
        write(Config.SYS_PEAK, peak, true)
        write(Config.SYS_MIN, min, false)

        if (changed) {
            val time = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
            val head = "$time ${p.source}: ${modeLabel(p.mode)} ${p.min.toInt()}–${p.max.toInt()} Hz\n"
            store.lastLog = (head + log.toString()).trim()
        }
        return ok
    }

    // ---------------- Sondagem e restauração ----------------

    private fun restoreSystemKey(key: String, original: String?) {
        try {
            if (original == null) resolver.delete(systemUri, "name =?", arrayOf(key))
            else Settings.System.putString(resolver, key, original)
        } catch (_: Throwable) {}
    }

    /** Testa quais valores o sistema aceita para peak/min e guarda o resultado. */
    fun probeWritableValues(): String {
        if (!hasPermission()) return "Sem permissão WRITE_SECURE_SETTINGS"
        val origPeak = Settings.System.getString(resolver, Config.SYS_PEAK)
        val origMin = Settings.System.getString(resolver, Config.SYS_MIN)
        val sb = StringBuilder("— Teste de valores aceitos —\n")
        for (key in listOf(Config.SYS_PEAK, Config.SYS_MIN)) {
            val accepted = mutableListOf<Float>()
            sb.append("$key:\n")
            for (v in probeValues) {
                val r = writeSystemFloat(key, v)
                if (r.ok) { accepted.add(v); sb.append("  ${v.toInt()} ok (${r.method})\n") }
                else sb.append("  ${v.toInt()} RECUSADO ${r.detail.take(260)}\n")
            }
            store.setAccepted(key, accepted)
        }
        restoreSystemKey(Config.SYS_PEAK, origPeak)
        restoreSystemKey(Config.SYS_MIN, origMin)
        sb.append("(valores originais restaurados)")
        return sb.toString()
    }

    /** Apaga as chaves para o sistema voltar ao comportamento dele. */
    fun resetSystem(): String {
        val sb = StringBuilder("— Restaurar padrão do sistema —\n")
        for (k in listOf(Config.SYS_PEAK, Config.SYS_MIN, "user_refresh_rate")) {
            try { resolver.delete(systemUri, "name =?", arrayOf(k)); sb.append("$k removido\n") }
            catch (e: Throwable) { sb.append("$k ERRO ${errText(e)}\n") }
        }
        return sb.toString()
    }

    fun sepVersion(): Int = try {
        Build.VERSION::class.java.getField("SEM_PLATFORM_INT").getInt(null)
    } catch (_: Throwable) { -1 }

    private fun listText(l: List<Float>): String =
        l.joinToString { it.toInt().toString() }.ifEmpty { "não testado" }

    /** Texto de diagnóstico mostrado no app. */
    fun status(): String {
        val prof = Engine.compute(context, store)
        return buildString {
            appendLine("Tela agora: ${currentDisplayHz().toInt()} Hz")
            appendLine("Adaptativo disponível: ${if (adaptiveCapable()) "sim" else "não"} (${Config.ADAPTIVE_MODE_NAMES[store.adaptiveOverride]})")
            appendLine("Taxas detectadas: ${detectedRates().joinToString { it.toInt().toString() }.ifEmpty { "nenhuma" }}")
            appendLine("Sistema: min=${readFloat(Config.SYS_MIN).toInt()} peak=${readFloat(Config.SYS_PEAK).toInt()} refresh_rate_mode=${currentRrm()}")
            appendLine("Alvo: ${modeLabel(prof.mode)} ${prof.min.toInt()}–${prof.max.toInt()} Hz (${prof.source})")
            appendLine("Valores de modo: Padrão=${rrmFor(Mode.STANDARD)} Adaptativo=${rrmFor(Mode.ADAPTIVE)} Alta=${rrmFor(Mode.HIGH)}")
            appendLine("WRITE_SECURE_SETTINGS: ${if (hasPermission()) "concedida" else "FALTANDO"}")
            appendLine("targetSdk do app: ${context.applicationInfo.targetSdkVersion} (precisa ser 22)")
            appendLine("Acessibilidade (taxa por app): ${if (SystemTweaks.accessibilityEnabled(context)) "ligada" else "desligada"}")
            appendLine("App em primeiro plano: ${ForegroundApp.pkg ?: "-"}")
            appendLine("Android ${Build.VERSION.SDK_INT} | SEP ${sepVersion()} | ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Aceitos (sondagem): peak=${listText(store.accepted(Config.SYS_PEAK))} min=${listText(store.accepted(Config.SYS_MIN))}")
            append("— Última aplicação —\n${store.lastLog}")
        }
    }
}
