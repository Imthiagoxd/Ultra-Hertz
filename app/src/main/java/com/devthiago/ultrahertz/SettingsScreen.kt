package com.devthiago.ultrahertz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(mgr: RefreshRateManager, themeMode: Int, onTheme: (Int) -> Unit, bump: () -> Unit) {
    val ctx = LocalContext.current
    val store = mgr.store
    val clipboard = LocalClipboardManager.current

    var diag by remember { mutableStateOf(mgr.status()) }
    var msg by remember { mutableStateOf<String?>(null) }

    ScreenColumn {
        ScreenTitle("Ajustes", "Tema, perfis e diagnóstico")

        Panel {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Caption("Aparência")
                ChipRow("Tema", Config.THEME_NAMES, themeMode) { onTheme(it) }
                Divider()
                ChipRow("Modo Adaptativo (reabra o app depois)", Config.ADAPTIVE_MODE_NAMES, store.adaptiveOverride) {
                    store.adaptiveOverride = it
                    msg = "Salvo. Feche e abra o app para atualizar as taxas e os modos."
                }
                Text(
                    "Automático esconde o Adaptativo em telas que não variam a taxa. Se o seu aparelho tem o modo e ele não apareceu, use Sempre ligado.",
                    color = Pal.textLo,
                    fontSize = 12.sp
                )
            }
        }

        Panel {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ActionRow("Recarregar perfis", "Aplica de novo a taxa de agora") {
                    Engine.applyNow(ctx)
                    EnforcerService.start(ctx)
                    msg = "Perfis recarregados"
                    diag = mgr.status()
                    bump()
                }
                Divider()
                ActionRow("Reiniciar mods", "Desfaz animações, janelas, bateria, tela desligada e monitor") {
                    msg = SystemTweaks.resetMods(ctx, store)
                    bump()
                }
                if (msg != null) Text(msg ?: "", color = Pal.accent, fontSize = 12.sp)
            }
        }

        Panel {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Caption("Diagnóstico")
                Spacer(Modifier.height(8.dp))
                SelectionContainer { Text(diag, fontSize = 12.sp, color = Pal.textLo) }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = { diag = mgr.status() }) { Text("Atualizar") }
                TextButton(onClick = { diag = mgr.probeWritableValues() + "\n\n" + mgr.status() }) {
                    Text("Testar valores aceitos pelo sistema")
                }
                TextButton(onClick = { diag = mgr.resetSystem() + "\n" + mgr.status(); bump() }) {
                    Text("Restaurar padrão do sistema")
                }
                TextButton(onClick = { mgr.resetLearned(); Engine.applyNow(ctx); diag = mgr.status(); bump() }) {
                    Text("Restaurar valores de modo")
                }
                TextButton(onClick = { clipboard.setText(AnnotatedString(diag)) }) { Text("Copiar diagnóstico") }
                Text(
                    "Ensinar valores: escolha o modo em Configurações do Android → Tela → Suavidade de movimento e toque no botão do mesmo modo.",
                    fontSize = 11.sp,
                    color = Pal.textLo
                )
                Row {
                    Mode.values().forEach { m ->
                        TextButton(onClick = { mgr.learn(m); diag = mgr.status() }) { Text(modeLabel(m)) }
                    }
                }
            }
        }

        Text("Ultra Hertz 1.0", color = Pal.textLo, fontSize = 12.sp)
    }
}
