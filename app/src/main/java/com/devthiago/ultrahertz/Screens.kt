package com.devthiago.ultrahertz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

fun rangeText(min: Float, max: Float): String =
    if (min == max) "${max.toInt()} Hz" else "${min.toInt()}–${max.toInt()} Hz"

@Composable
fun ScreenColumn(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        content = content
    )
}

// ============================================================
// ABA 1 — TAXA
// ============================================================
@Composable
fun RateScreen(mgr: RefreshRateManager, tick: Int, bump: () -> Unit, goTab: (Int) -> Unit) {
    val ctx = LocalContext.current
    val store = mgr.store
    val clipboard = LocalClipboardManager.current

    var refresh by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(Config.UI_REFRESH_MS)
            refresh++
        }
    }

    var enabled by remember { mutableStateOf(store.enabled) }
    var mode by remember {
        mutableStateOf(if (!mgr.adaptiveCapable() && store.mode == Mode.ADAPTIVE) Mode.HIGH else store.mode)
    }
    var normal by remember { mutableStateOf(store.situation(Situation.NORMAL)) }
    var prevent by remember { mutableStateOf(store.preventPsm) }
    var hasPerm by remember { mutableStateOf(mgr.hasPermission()) }
    val rates = remember { mgr.supportedRates() }
    val capable = remember { mgr.adaptiveCapable() }
    LaunchedEffect(Unit) {
        if (!capable && store.mode == Mode.ADAPTIVE) {
            store.mode = Mode.HIGH
            bump()
        }
    }
    val profile = remember(refresh, tick, enabled, mode, normal) { Engine.compute(ctx, store) }

    fun applyAll() {
        store.enabled = enabled
        store.mode = mode
        store.setSituation(Situation.NORMAL, normal)
        hasPerm = mgr.hasPermission()
        if (enabled && hasPerm) {
            Engine.applyNow(ctx)
            EnforcerService.start(ctx)
        } else {
            EnforcerService.stop(ctx)
        }
        bump()
    }

    fun sitText(s: Situation): String {
        val c = store.situation(s)
        if (!c.enabled) return "desligada"
        return when (store.mode) {
            Mode.STANDARD -> "60 Hz"
            Mode.ADAPTIVE -> rangeText(c.adpMin, c.adpMax)
            Mode.HIGH -> "${c.highHz.toInt()} Hz"
        }
    }

    ScreenColumn {
        ScreenTitle("Ultra Hertz", "Taxa de atualização")

        HeroCard(
            label = if (enabled) "ALVO AGORA" else "DESATIVADO",
            big = rangeText(profile.min, profile.max),
            sub = "${modeLabel(profile.mode)} · ${profile.source}",
            min = profile.min,
            max = profile.max,
            panelMax = rates.maxOrNull() ?: Config.DEFAULT_MAX_HZ
        )

        Panel {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                SwitchRow("Interruptor principal", "Liga ou desliga todo o controle", enabled) {
                    enabled = it
                    applyAll()
                }
            }
        }

        if (capable) {
            Segmented(
                listOf("Padrão", "Adaptativo", "Alta"),
                listOf("60 Hz", rangeText(normal.adpMin, normal.adpMax), "${normal.highHz.toInt()} Hz"),
                mode.ordinal
            ) {
                mode = Mode.values()[it]
                applyAll()
            }
        } else {
            Segmented(
                listOf("Padrão", "Alta"),
                listOf("60 Hz", "${normal.highHz.toInt()} Hz"),
                if (mode == Mode.STANDARD) 0 else 1
            ) {
                mode = if (it == 0) Mode.STANDARD else Mode.HIGH
                applyAll()
            }
        }

        if (mode != Mode.STANDARD) {
            Panel {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Caption("Uso normal · ${modeLabel(mode)}")
                    if (mode == Mode.ADAPTIVE && capable) {
                        HzChips("Mínimo", normal.adpMin, rates) {
                            normal = normal.copy(adpMin = it.coerceAtMost(normal.adpMax))
                            applyAll()
                        }
                        HzChips("Máximo", normal.adpMax, rates) {
                            normal = normal.copy(adpMax = it.coerceAtLeast(normal.adpMin))
                            applyAll()
                        }
                    } else {
                        HzChips("Taxa fixa", normal.highHz, rates) {
                            normal = normal.copy(highHz = it)
                            applyAll()
                        }
                    }
                }
            }
        }

        Panel {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SwitchRow(
                    "Evitar o modo Padrão na economia",
                    "Impede que a economia de energia force 60 Hz",
                    prevent
                ) {
                    prevent = it
                    store.preventPsm = it
                    Engine.applyNow(ctx)
                    bump()
                }
                Divider()
                Column(Modifier.fillMaxWidth().clickable { goTab(1) }) {
                    Text("Taxa padrão por situação", color = Pal.textHi, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Spacer(Modifier.height(4.dp))
                    Text("• Normal: ${sitText(Situation.NORMAL)}", color = Pal.textLo, fontSize = 13.sp)
                    Text("• Economia de energia: ${sitText(Situation.POWER_SAVER)}", color = Pal.textLo, fontSize = 13.sp)
                    Text("• Brilho baixo: ${sitText(Situation.LOW_BRIGHTNESS)}", color = Pal.textLo, fontSize = 13.sp)
                }
                Divider()
                ActionRow("Taxa por app", "${store.ruleCount()} regra(s) salva(s)") { goTab(2) }
            }
        }

        Panel {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Caption("Taxas do painel")
                Spacer(Modifier.height(6.dp))
                Text(
                    rates.joinToString(" / ") { it.toInt().toString() } + " Hz",
                    color = Pal.textHi,
                    fontSize = 15.sp
                )
            }
        }

        if (!hasPerm) {
            val cmd = "& \"\$env:LOCALAPPDATA\\Android\\Sdk\\platform-tools\\adb.exe\" shell pm grant ${ctx.packageName} android.permission.WRITE_SECURE_SETTINGS"
            Panel(bg = Pal.surface2) {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text("Falta uma permissão", color = Pal.warm, fontWeight = FontWeight.Bold)
                    Text("No PowerShell do PC, com o celular por USB, rode:", color = Pal.textLo, fontSize = 13.sp)
                    SelectionContainer { Text(cmd, color = Pal.textHi, fontSize = 12.sp) }
                    Row {
                        TextButton(onClick = { clipboard.setText(AnnotatedString(cmd)) }) { Text("Copiar comando") }
                        TextButton(onClick = { applyAll() }) { Text("Verificar de novo") }
                    }
                }
            }
        }
    }
}

// ============================================================
// ABA 2 — SITUAÇÕES (inclui tela desligada)
// ============================================================
@Composable
private fun SituationCard(
    title: String,
    subtitle: String,
    s: Situation,
    store: Store,
    rates: List<Float>,
    capable: Boolean,
    onChanged: () -> Unit,
    extra: @Composable ColumnScope.() -> Unit = {}
) {
    var cfg by remember { mutableStateOf(store.situation(s)) }
    fun save(c: SituationCfg) {
        cfg = c
        store.setSituation(s, c)
        onChanged()
    }
    Panel {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SwitchRow(title, subtitle, cfg.enabled) { save(cfg.copy(enabled = it)) }
            if (cfg.enabled) {
                if (s == Situation.SCREEN_OFF) {
                    HzChips("Taxa com a tela desligada", cfg.highHz, rates) { save(cfg.copy(highHz = it)) }
                } else {
                    if (capable) {
                        Caption("Adaptativo")
                        HzChips("Mínimo", cfg.adpMin, rates) { save(cfg.copy(adpMin = it.coerceAtMost(cfg.adpMax))) }
                        HzChips("Máximo", cfg.adpMax, rates) { save(cfg.copy(adpMax = it.coerceAtLeast(cfg.adpMin))) }
                    }
                    Caption(if (capable) "Alta" else "Taxa fixa")
                    HzChips("Taxa fixa", cfg.highHz, rates) { save(cfg.copy(highHz = it)) }
                }
                extra()
            }
        }
    }
}

@Composable
fun SituationsScreen(mgr: RefreshRateManager, bump: () -> Unit) {
    val ctx = LocalContext.current
    val store = mgr.store
    val rates = remember { mgr.supportedRates() }
    val capable = remember { mgr.adaptiveCapable() }
    var thr by remember { mutableStateOf(store.lbThreshold) }
    var aod by remember { mutableStateOf(store.aodRestart) }
    var aodIgn by remember { mutableStateOf(store.aodIgnoreCharging) }
    var cam by remember { mutableStateOf(store.cameraBoost) }
    var psm by remember { mutableStateOf(store.psmOnScreenOff) }
    var psmIgn by remember { mutableStateOf(store.psmIgnoreCharging) }
    var sync by remember { mutableStateOf(store.syncOff) }
    var syncIgn by remember { mutableStateOf(store.syncIgnoreCharging) }
    val changed: () -> Unit = {
        Engine.applyNow(ctx)
        bump()
    }

    ScreenColumn {
        ScreenTitle("Situações", "Uma faixa de Hz para cada momento")
        Text(
            "Cada situação usa o modo escolhido na aba Taxa. Se uma situação estiver desligada, vale o uso normal.",
            color = Pal.textLo,
            fontSize = 13.sp
        )

        Panel {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                SwitchRow(
                    "Câmera",
                    "Ao abrir a câmera, o mínimo sobe para ${Config.CAMERA_MIN_HZ.toInt()} Hz (só no Adaptativo)",
                    cam
                ) {
                    cam = it
                    store.cameraBoost = it
                    changed()
                }
            }
        }

        SituationCard(
            "Economia de energia",
            "Quando a economia de energia estiver ligada",
            Situation.POWER_SAVER, store, rates, capable, changed
        )

        SituationCard(
            "Brilho baixo",
            "Quando o brilho ficar abaixo de $thr%",
            Situation.LOW_BRIGHTNESS, store, rates, capable, changed
        ) {
            ChipRow(
                "Limite de brilho: $thr%",
                Config.LB_THRESHOLD_OPTIONS.map { "$it%" },
                Config.LB_THRESHOLD_OPTIONS.indexOf(thr)
            ) {
                thr = Config.LB_THRESHOLD_OPTIONS[it]
                store.lbThreshold = thr
                changed()
            }
        }

        SituationCard(
            "Tela desligada / AOD",
            "Taxa usada enquanto a tela está apagada",
            Situation.SCREEN_OFF, store, rates, capable, changed
        ) {
            SwitchRow("Ignorar ao carregar", "Com o cabo ligado, a taxa normal continua valendo", aodIgn) {
                aodIgn = it
                store.aodIgnoreCharging = it
                changed()
            }
            SwitchRow(
                "Reiniciar o AOD ao desligar a tela",
                "Experimental: pode ajudar o sistema a usar a taxa nova no AOD",
                aod
            ) {
                aod = it
                store.aodRestart = it
            }
        }

        Panel {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Caption("Ao desligar a tela")
                SwitchRow(
                    "Economia de energia",
                    "Experimental · liga ao apagar a tela e desliga ao acender",
                    psm
                ) {
                    psm = it
                    store.psmOnScreenOff = it
                }
                if (psm) {
                    SwitchRow("Ignorar ao carregar", null, psmIgn) {
                        psmIgn = it
                        store.psmIgnoreCharging = it
                    }
                }
                Divider()
                SwitchRow(
                    "Desligar sincronização automática",
                    "Pausa a sincronização com a tela apagada e volta ao acender",
                    sync
                ) {
                    sync = it
                    store.syncOff = it
                }
                if (sync) {
                    SwitchRow("Ignorar ao carregar", null, syncIgn) {
                        syncIgn = it
                        store.syncIgnoreCharging = it
                    }
                }
            }
        }
    }
}
