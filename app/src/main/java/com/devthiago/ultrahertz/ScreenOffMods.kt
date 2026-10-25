package com.devthiago.ultrahertz

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings

/** Ações ao apagar a tela: ligar a economia de energia e pausar a sincronização (desfeitas ao acender). */
object ScreenOffMods {

    fun onScreenOff(ctx: Context, store: Store) {
        val charging = SystemTweaks.isCharging(ctx)

        if (store.psmOnScreenOff && !(store.psmIgnoreCharging && charging)) {
            try {
                val r = ctx.contentResolver
                if (Settings.Global.getInt(r, "low_power", 0) == 0) {
                    try { Settings.System.putInt(r, "psm_switch", 1) } catch (_: Throwable) {}
                    Settings.Global.putInt(r, "low_power", 1)
                    store.psmByUs = true
                }
            } catch (_: Throwable) {}
        }

        if (store.syncOff && !(store.syncIgnoreCharging && charging)) {
            try {
                if (ContentResolver.getMasterSyncAutomatically()) {
                    ContentResolver.setMasterSyncAutomatically(false)
                    store.syncByUs = true
                }
            } catch (_: Throwable) {}
        }
    }

    /** Desfaz só o que nós fizemos. */
    fun onScreenOn(ctx: Context, store: Store) {
        if (store.psmByUs) {
            try {
                val r = ctx.contentResolver
                Settings.Global.putInt(r, "low_power", 0)
                try { Settings.System.putInt(r, "psm_switch", 0) } catch (_: Throwable) {}
            } catch (_: Throwable) {}
            store.psmByUs = false
        }
        if (store.syncByUs) {
            try { ContentResolver.setMasterSyncAutomatically(true) } catch (_: Throwable) {}
            store.syncByUs = false
        }
    }
}
