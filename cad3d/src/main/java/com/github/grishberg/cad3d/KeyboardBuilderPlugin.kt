package com.github.grishberg.cad3d

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig.Companion.getKeyboardConfig
import com.github.grishberg.cad3d.plugin.Cad3dPlugin
import com.github.grishberg.cad3d.plugin.ResultListener
import com.github.grishberg.cad3d.plugin.cfg.KeyboardPart
import com.github.grishberg.cad3d.plugin.cfg.SettingsContainer
import kotlinx.coroutines.Dispatchers

class KeyboardBuilderPlugin : Cad3dPlugin {

    private val keyboardBuilder = KeyboardBuilder(
        mainThreadDispatcher = Dispatchers.Main,
    )

    override fun requestModels(
        config: SettingsContainer, modifiedKeyboardParts: Set<KeyboardPart>, listener: ResultListener
    ) {
        keyboardBuilder.rebuildModels(config.getKeyboardConfig(modifiedKeyboardParts), listener)
    }

    override val version: Long = 2

    override val name: String = "Keyboard builder $version"
}
