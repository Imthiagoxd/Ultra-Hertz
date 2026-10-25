package com.devthiago.ultrahertz

import android.graphics.Color

// ============================================================
// CONFIGURAÇÕES AJUSTÁVEIS (tudo que você pode querer mudar fica aqui)
// ============================================================
object Config {

    // ---- Taxas padrão (Hz) ----
    const val STANDARD_HZ = 60f            // Modo Padrão
    const val DEFAULT_MIN_HZ = 10f         // Mínimo do Adaptativo em uso normal
    const val DEFAULT_MAX_HZ = 120f        // Máximo do Adaptativo / taxa da Alta em uso normal
    const val MIN_SELECTABLE_HZ = 10f      // abaixo disso a taxa não serve (ignora 1, 3 etc.)
    const val ADAPTIVE_LOWEST_HZ = 30f     // painel com taxa mínima até aqui = tem modo Adaptativo (LTPO)
    val FALLBACK_RATES = listOf(60f, 120f)                                  // se o painel não informar
    val LTPO_RATES = listOf(10f, 24f, 30f, 48f, 60f, 80f, 96f, 120f)        // degraus usados em painéis LTPO

    // ---- Valores iniciais das situações ----
    const val PSM_MIN_HZ = 10f             // economia de energia
    const val PSM_MAX_HZ = 60f
    const val LB_MIN_HZ = 10f              // brilho baixo
    const val LB_MAX_HZ = 60f
    const val LB_THRESHOLD_PERCENT = 30    // abaixo disso (% de brilho) vale "brilho baixo"
    val LB_THRESHOLD_OPTIONS = listOf(10, 20, 30, 40, 50)
    const val SCREEN_OFF_HZ = 10f          // taxa com a tela desligada / AOD

    // ---- Tempos (ms) ----
    const val OBSERVER_DEBOUNCE_MS = 250L  // espera antes de reaplicar após o sistema mexer
    const val SCREEN_STATE_DELAY_MS = 120L // espera após ligar/desligar a tela
    const val APP_SWITCH_DELAY_MS = 150L   // espera após trocar de app
    const val AOD_RESTART_DELAY_MS = 450L  // pausa ao reiniciar o AOD
    const val MONITOR_UPDATE_MS = 500L     // atualização do monitor de Hz
    const val NET_UPDATE_MS = 1000L        // atualização do medidor de rede
    const val UI_REFRESH_MS = 2000L        // atualização da tela principal
    const val HEARTBEAT_MS = 4000L         // reaplica sozinho de tempos em tempos (cobre economia de energia etc.)
    const val POWER_SAVE_RECHECK_MS = 1500L // nova checagem depois de ligar/desligar a economia de energia

    // ---- Tela desligada ----
    const val SCREEN_OFF_ACTION_DELAY_MS = 1000L   // espera depois de apagar a tela antes de ligar PSM / desligar sincronização
    val NET_UNIT_NAMES = listOf("Bytes/s", "Bits/s")
    val NET_MODE_NAMES = listOf("Separado", "Combinado")
    val THEME_NAMES = listOf("Sistema", "Claro", "Escuro")

    // ---- Câmera ----
    const val CAMERA_MIN_HZ = 60f          // ao abrir a câmera, o mínimo sobe para isso

    // ---- Animações do sistema ----
    val ANIMATION_STEPS = listOf(0f, 0.25f, 0.5f, 0.75f, 1f, 1.5f, 2f)

    // ---- Monitor na tela ----
    const val MONITOR_TEXT_SIZE_SP = 12f
    val MONITOR_SIZES = listOf(10f, 12f, 15f, 18f)   // tamanhos do texto do monitor (sp)
    val MONITOR_SIZE_NAMES = listOf("Pequeno", "Médio", "Grande", "Enorme")
    const val MONITOR_TEXT_COLOR = Color.GREEN
    const val MONITOR_BG_COLOR = 0x88000000.toInt()

    // ---- Detector de app em primeiro plano ----
    val IGNORED_PACKAGES = setOf(
        "com.android.systemui",
        "android",
        "com.samsung.android.app.cocktailbarservice",
        "com.samsung.android.app.taskedge",
        "com.samsung.android.service.peoplestripe",
        "com.samsung.android.bixby.agent"
    )

    // ---- Samsung ----
    const val DISABLE_PSM_REFRESH_LIMIT = true   // desliga o limite de taxa da economia de energia

    // ---- Armazenamento e chaves do sistema (não precisa mexer) ----
    const val PREFS = "ultrahz_prefs"
    const val SYS_MIN = "min_refresh_rate"
    const val SYS_PEAK = "peak_refresh_rate"
    const val SAMSUNG_MODE = "refresh_rate_mode"  // Settings.Secure
    // Só a lista de taxas. (A chave HFR_MODE traz códigos 1/3, não taxas.)
    val FLOATING_KEYS = listOf(
        "SEC_FLOATING_FEATURE_LCD_CONFIG_HFR_SUPPORTED_REFRESH_RATE"
    )
    val ADAPTIVE_MODE_NAMES = listOf("Automático", "Sempre ligado", "Desligado")
}
