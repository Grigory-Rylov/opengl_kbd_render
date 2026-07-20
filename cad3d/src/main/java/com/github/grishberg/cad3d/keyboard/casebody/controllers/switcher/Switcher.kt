package com.github.grishberg.cad3d.keyboard.casebody.controllers.switcher

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.javascad.models.Model

interface Switcher {

    fun createSwitcher(): ModelHolder
    fun createSwitcherHole(): Model
}
