package com.devthiago.ultrahertz

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

/**
 * Só descobre qual app está aberto (não lê o conteúdo da tela) para aplicar a taxa escolhida para ele.
 */
class AppWatcherService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())
    private val applyTask = Runnable { Engine.applyNow(this) }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg in Config.IGNORED_PACKAGES) return
        // só vale janela de Activity de verdade (ignora diálogos, pop-ups, menus)
        val cls = event.className?.toString() ?: return
        try {
            packageManager.getActivityInfo(ComponentName(pkg, cls), 0)
        } catch (_: Exception) {
            return
        }
        // a tela inicial (launcher) conta: ao voltar pra ela, sai a regra do app anterior
        if (pkg == ForegroundApp.pkg) return

        ForegroundApp.pkg = pkg
        handler.removeCallbacks(applyTask)
        handler.postDelayed(applyTask, Config.APP_SWITCH_DELAY_MS)
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: Intent?): Boolean {
        ForegroundApp.pkg = null
        handler.removeCallbacksAndMessages(null)
        return super.onUnbind(intent)
    }
}
