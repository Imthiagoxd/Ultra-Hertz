package com.devthiago.ultrahertz

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

private fun scaleText(v: Float): String =
    if (v == v.toInt().toFloat()) "${v.toInt()}x" else "${v}x"

@Composable
fun ExtrasScreen(mgr: RefreshRateManager, bump: () -> Unit) {
    val ctx = LocalContext.current
    val store = mgr.store

    var monHz by remember { mutableStateOf(store.monitorHz) }
    var monNet by remember { mutableStateOf(store.netSpeed) }
    var monBottom by remember { mutableStateOf(store.monitorBottom) }
    var monSize by remember { mutableStateOf(store.monitorSize) }
    var netBits by remember { mutableStateOf(store.netBits) }
    var netCombined by remember { mutableStateOf(store.netCombined) }
    var anim by remember { mutableStateOf(SystemTweaks.animationScale(ctx)) }
    var animErr by remember { mutableStateOf<String?>(null) }
    var resizable by remember { mutableStateOf(SystemTweaks.forceResizable(ctx)) }
    var protect by remember { mutableStateOf(SystemTweaks.protectBattery(ctx)) }
    var err by remember { mutableStateOf<String?>(null) }
    var showBattery by remember { mutableStateOf(false) }

    fun applyMonitor() {
        if ((monHz || monNet) && !Settings.canDrawOverlays(ctx)) {
            ctx.startActivity(
                Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${ctx.packageName}"))
            )
            monHz = false
            monNet = false
        }
        store.monitorHz = monHz
        store.netSpeed = monNet
        MonitorService.refresh(ctx)
    }

    ScreenColumn {
        ScreenTitle("Extras", "Bateria, animações e monitor")

        // ---- Bateria ----
        Panel {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Caption("Bateria")
                SwitchRow("Proteger bateria", "Limita a carga a 85% (recurso da própria Samsung)", protect) {
                    err = SystemTweaks.setProtectBattery(ctx, it)
                    protect = SystemTweaks.protectBattery(ctx)
                }
                Divider()
                ActionRow("Informações da bateria", "Nível, saúde, temperatura e ciclos") { showBattery = true }
                if (err != null) Text(err ?: "", color = Pal.danger, fontSize = 12.sp)
            }
        }

        // ---- Animações ----
        Panel {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Caption("Animações do sistema")
                val steps = Config.ANIMATION_STEPS
                ChipRow(
                    "Velocidade: ${scaleText(anim)}",
                    steps.map { scaleText(it) },
                    steps.indexOfFirst { abs(it - anim) < 0.01f }
                ) {
                    animErr = SystemTweaks.setAnimationScale(ctx, steps[it])
                    anim = SystemTweaks.animationScale(ctx)
                }
                Text(
                    "Menor número = animações mais rápidas. 0x desliga as animações.",
                    color = Pal.textLo,
                    fontSize = 12.sp
                )
                if (animErr != null) Text(animErr ?: "", color = Pal.danger, fontSize = 12.sp)
            }
        }

        // ---- Janelas ----
        Panel {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                SwitchRow(
                    "Forçar atividades redimensionáveis",
                    "Deixa apps antigos abrirem em janela e tela dividida",
                    resizable
                ) {
                    err = SystemTweaks.setForceResizable(ctx, it)
                    resizable = SystemTweaks.forceResizable(ctx)
                }
            }
        }

        // ---- Monitor ----
        Panel {
            Column(
                Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Caption("Monitor na tela")
                SwitchRow("Taxa de atualização real", "Mostra os Hz na tela", monHz) {
                    monHz = it
                    applyMonitor()
                }
                SwitchRow("Indicador de velocidade da rede", "Mostra download e upload na tela", monNet) {
                    monNet = it
                    applyMonitor()
                }
                ChipRow("Posição", listOf("Topo", "Base"), if (monBottom) 1 else 0) {
                    monBottom = it == 1
                    store.monitorBottom = monBottom
                    MonitorService.refresh(ctx)
                }
                ChipRow("Tamanho", Config.MONITOR_SIZE_NAMES, monSize) {
                    monSize = it
                    store.monitorSize = it
                    MonitorService.refresh(ctx)
                }
                ChipRow("Unidade da rede", Config.NET_UNIT_NAMES, if (netBits) 1 else 0) {
                    netBits = it == 1
                    store.netBits = netBits
                }
                ChipRow("Exibição da rede", Config.NET_MODE_NAMES, if (netCombined) 1 else 0) {
                    netCombined = it == 1
                    store.netCombined = netCombined
                }
            }
        }
    }

    if (showBattery) {
        AlertDialog(
            onDismissRequest = { showBattery = false },
            containerColor = Pal.surface2,
            title = { Text("Informações da bateria", color = Pal.textHi) },
            text = { Text(SystemTweaks.batteryInfo(ctx), color = Pal.textHi, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { showBattery = false }) { Text("OK") } }
        )
    }
}
