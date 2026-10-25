package com.devthiago.ultrahertz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Reaplica a taxa depois de reiniciar o celular. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val mgr = RefreshRateManager(context)
        if (mgr.store.enabled && mgr.hasPermission()) {
            EnforcerService.start(context)
            MonitorService.refresh(context)
        }
    }
}
