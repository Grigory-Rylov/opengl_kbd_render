package com.github.grishberg.cad3d

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig.Companion.getKeyboardConfig
import com.github.grishberg.cad3d.plugin.Cad3dPlugin
import com.github.grishberg.cad3d.plugin.StlExportListener
import com.github.grishberg.cad3d.plugin.ResultListener
import com.github.grishberg.cad3d.plugin.cfg.KeyboardPart
import com.github.grishberg.cad3d.plugin.cfg.SettingsContainer
import com.github.grishberg.javascad.manifold.Manifold3dEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class KeyboardBuilderPlugin : Cad3dPlugin {

    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default)

    private val keyboardBuilder = KeyboardBuilder(
        coroutineScope = coroutineScope,
        mainThreadDispatcher = Dispatchers.Main,
    )

    override fun requestModels(
        config: SettingsContainer, modifiedKeyboardParts: Set<KeyboardPart>, listener: ResultListener
    ) {
        keyboardBuilder.rebuildModels(config.getKeyboardConfig(modifiedKeyboardParts), listener)
    }

    override fun exportStl(config: SettingsContainer, listener: StlExportListener) {
        keyboardBuilder.exportStl(config.getKeyboardConfig(emptySet()), listener)
    }

    override fun onUnload() {
        println("onUnload $this")
        coroutineScope.cancel()
        Manifold3dEngine.clearAll()
    }

    override val version: Long = 1

    override val name: String = "Ergonomic keyboard builder by Grishberg"

    override fun toString(): String = "KeyboardBuilderPlugin : $name, $version"
}
