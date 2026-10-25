package com.devthiago.ultrahertz

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Atalho nas configurações rápidas: cada toque troca Padrão → Adaptativo → Alta. */
class ModeTileService : TileService() {

    override fun onStartListening() = refresh()

    override fun onClick() {
        val store = Store(this)
        val capable = RefreshRateManager(this).adaptiveCapable()
        store.mode = when (store.mode) {
            Mode.STANDARD -> if (capable) Mode.ADAPTIVE else Mode.HIGH
            Mode.ADAPTIVE -> Mode.HIGH
            Mode.HIGH -> Mode.STANDARD
        }
        Engine.applyNow(this)
        refresh()
    }

    private fun refresh() {
        val store = Store(this)
        val tile = qsTile ?: return
        tile.label = "Ultra Hertz"
        tile.subtitle = modeLabel(store.mode)
        tile.state = if (store.enabled) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.updateTile()
    }
}
