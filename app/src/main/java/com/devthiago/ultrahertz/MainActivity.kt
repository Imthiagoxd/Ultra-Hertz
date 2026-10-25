package com.devthiago.ultrahertz

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { AppShell() }
    }
}

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AppShell() {
    val ctx = LocalContext.current
    val mgr = remember { RefreshRateManager(ctx) }
    var themeMode by remember { mutableStateOf(mgr.store.themeMode) }

    val sysDark = isSystemInDarkTheme()
    val dark = when (themeMode) {
        1 -> false
        2 -> true
        else -> sysDark
    }
    Pal.dark = dark

    val scheme = if (dark) {
        darkColorScheme(
            primary = Pal.accent, background = Pal.bg, surface = Pal.surface1,
            onSurface = Pal.textHi, onBackground = Pal.textHi, onPrimary = Pal.onAccent
        )
    } else {
        lightColorScheme(
            primary = Pal.accent, background = Pal.bg, surface = Pal.surface1,
            onSurface = Pal.textHi, onBackground = Pal.textHi, onPrimary = Pal.onAccent
        )
    }

    // cores das barras do sistema acompanham o tema
    SideEffect {
        val w = ctx.findActivity()?.window ?: return@SideEffect
        w.statusBarColor = Pal.bg.toArgb()
        w.navigationBarColor = Pal.surface1.toArgb()
        w.decorView.systemUiVisibility =
            if (dark) 0 else (View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR)
    }

    MaterialTheme(colorScheme = scheme) {
        Surface(Modifier.fillMaxSize(), color = Pal.bg) {
            key(dark) {
                Content(mgr, themeMode) {
                    themeMode = it
                    mgr.store.themeMode = it
                }
            }
        }
    }
}

@Composable
private fun Content(mgr: RefreshRateManager, themeMode: Int, onTheme: (Int) -> Unit) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(0) }
    var tick by remember { mutableStateOf(0) }
    val bump: () -> Unit = { tick++ }
    val goTab: (Int) -> Unit = { tab = it }

    // Garante que o serviço que mantém a taxa esteja rodando sempre que o app é aberto
    LaunchedEffect(Unit) {
        if (mgr.store.enabled && mgr.hasPermission()) {
            try { EnforcerService.start(ctx) } catch (_: Throwable) {}
            MonitorService.refresh(ctx)
        }
    }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                0 -> RateScreen(mgr, tick, bump, goTab)
                1 -> SituationsScreen(mgr, bump)
                2 -> AppsScreen(mgr, bump)
                3 -> ExtrasScreen(mgr, bump)
                else -> SettingsScreen(mgr, themeMode, onTheme, bump)
            }
        }
        BottomBar(tab) { tab = it }
    }
}
