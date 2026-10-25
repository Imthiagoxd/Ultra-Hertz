package com.devthiago.ultrahertz

enum class Mode { STANDARD, ADAPTIVE, HIGH }

enum class Situation(val label: String) {
    NORMAL("Uso normal"),
    POWER_SAVER("Economia de energia"),
    LOW_BRIGHTNESS("Brilho baixo"),
    SCREEN_OFF("Tela desligada")
}

/** Faixas de uma situação: adaptativo (min..max) e taxa fixa da Alta. */
data class SituationCfg(val enabled: Boolean, val adpMin: Float, val adpMax: Float, val highHz: Float)

/** Regra de um app específico. */
data class AppRule(val mode: Mode, val min: Float, val max: Float)

/** O que realmente será gravado no sistema agora. */
data class Profile(val mode: Mode, val min: Float, val max: Float, val source: String)

fun modeLabel(m: Mode): String = when (m) {
    Mode.STANDARD -> "Padrão"
    Mode.ADAPTIVE -> "Adaptativo"
    Mode.HIGH -> "Alta"
}

/** App em primeiro plano, preenchido pelo serviço de acessibilidade. */
object ForegroundApp {
    @Volatile
    var pkg: String? = null
}

/** Câmera em uso por algum app (preenchido pelo serviço). */
object CameraState {
    @Volatile
    var inUse: Boolean = false
}
