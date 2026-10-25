package com.devthiago.ultrahertz

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

data class AppItem(val pkg: String, val label: String)

private fun loadApps(ctx: Context): List<AppItem> {
    val pm = ctx.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map { AppItem(it.activityInfo.packageName, it.loadLabel(pm).toString()) }
        .filter { it.pkg != ctx.packageName }
        .distinctBy { it.pkg }
}

private fun ruleText(r: AppRule?): String = when {
    r == null -> "Padrão"
    r.mode == Mode.ADAPTIVE -> "Adaptativo\n${rangeText(r.min, r.max)}"
    else -> "${modeLabel(r.mode)}\n${rangeText(r.min, r.max)}"
}

@Composable
private fun AppRuleDialog(
    label: String,
    initial: AppRule?,
    rates: List<Float>,
    capable: Boolean,
    onSave: (AppRule?) -> Unit,
    onDismiss: () -> Unit
) {
    // 0 = geral, 1 = padrão, 2 = adaptativo, 3 = alta
    var choice by remember {
        val c = if (initial == null) 0 else initial.mode.ordinal + 1
        mutableStateOf(if (!capable && c == 2) 3 else c)
    }
    val keys = if (capable) listOf(0, 1, 2, 3) else listOf(0, 1, 3)
    val names = if (capable) listOf("Geral", "Padrão", "Adaptativo", "Alta") else listOf("Geral", "Padrão", "Alta")
    var mn by remember { mutableStateOf(initial?.min ?: Config.DEFAULT_MIN_HZ) }
    var mx by remember { mutableStateOf(initial?.max ?: Config.DEFAULT_MAX_HZ) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Pal.surface2,
        title = { Text(label, color = Pal.textHi) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Segmented(names, null, keys.indexOf(choice)) { choice = keys[it] }
                if (choice == 2) {
                    HzChips("Mínimo", mn, rates) { mn = it.coerceAtMost(mx) }
                    HzChips("Máximo", mx, rates) { mx = it.coerceAtLeast(mn) }
                }
                if (choice == 3) {
                    HzChips("Taxa fixa", mx, rates) { mx = it; mn = it }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val rule: AppRule? = when (choice) {
                    1 -> AppRule(Mode.STANDARD, Config.STANDARD_HZ, Config.STANDARD_HZ)
                    2 -> AppRule(Mode.ADAPTIVE, mn, mx)
                    3 -> AppRule(Mode.HIGH, mx, mx)
                    else -> null
                }
                onSave(rule)
            }) { Text("Salvar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
private fun AppRow(item: AppItem, rule: AppRule?, onClick: () -> Unit) {
    val ctx = LocalContext.current
    val icon = remember(item.pkg) {
        try {
            ctx.packageManager.getApplicationIcon(item.pkg).toBitmap(96, 96).asImageBitmap()
        } catch (_: Throwable) { null }
    }
    Panel {
        Row(
            Modifier.fillMaxWidth().clickable { onClick() }.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(11.dp))
                )
            } else {
                Spacer(Modifier.size(42.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(item.label, color = Pal.textHi, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.pkg, color = Pal.textLo, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                ruleText(rule),
                color = if (rule == null) Pal.textLo else Pal.warm,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AppsScreen(mgr: RefreshRateManager, bump: () -> Unit) {
    val ctx = LocalContext.current
    val store = mgr.store
    val rates = remember { mgr.supportedRates() }
    val capable = remember { mgr.adaptiveCapable() }

    var apps by remember { mutableStateOf<List<AppItem>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var perApp by remember { mutableStateOf(store.perAppEnabled) }
    var a11y by remember { mutableStateOf(SystemTweaks.accessibilityEnabled(ctx)) }
    var editing by remember { mutableStateOf<AppItem?>(null) }
    var version by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.Default) { loadApps(ctx) }
        while (true) {
            delay(1500)
            a11y = SystemTweaks.accessibilityEnabled(ctx)
        }
    }

    val filtered = remember(apps, query, version) {
        apps
            .filter { it.label.contains(query, ignoreCase = true) || it.pkg.contains(query, ignoreCase = true) }
            .sortedWith(
                compareByDescending<AppItem> { store.appRule(it.pkg) != null }
                    .thenBy { it.label.lowercase() }
            )
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { ScreenTitle("Apps", "Uma taxa para cada app") }

        item {
            Panel {
                Column(
                    Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SwitchRow(
                        "Taxa por app",
                        "Regras salvas: ${store.ruleCount()}",
                        perApp
                    ) {
                        perApp = it
                        store.perAppEnabled = it
                        Engine.applyNow(ctx)
                        bump()
                    }
                    if (!a11y) {
                        Text(
                            "O serviço de acessibilidade está desligado. Ele só descobre qual app está aberto e não lê o conteúdo da tela.",
                            color = Pal.warm,
                            fontSize = 12.sp
                        )
                        TextButton(onClick = {
                            val err = SystemTweaks.enableAccessibility(ctx)
                            if (err != null) {
                                ctx.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                            }
                            a11y = SystemTweaks.accessibilityEnabled(ctx)
                        }) { Text("Ativar serviço") }
                    } else {
                        Text("Serviço de acessibilidade ligado", color = Pal.accent, fontSize = 12.sp)
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                label = { Text("Buscar app") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        items(filtered, key = { it.pkg }) { app ->
            AppRow(app, store.appRule(app.pkg)) { editing = app }
        }
    }

    editing?.let { item ->
        AppRuleDialog(
            label = item.label,
            initial = store.appRule(item.pkg),
            rates = rates,
            capable = capable,
            onSave = { rule ->
                store.setAppRule(item.pkg, rule)
                editing = null
                version++
                Engine.applyNow(ctx)
                bump()
            },
            onDismiss = { editing = null }
        )
    }
}
