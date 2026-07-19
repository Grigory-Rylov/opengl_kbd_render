package com.github.grishberg.cad3d.keyboard.casebody.controllers.switcher

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.util.fromModel
import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.coords.Angles3d
import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.utils.Color

class RoundSwitcher(private val cfg: KeyboardConfig) : Switcher {

    private val holeDiameter = 6.0
    private val outerHoleDiameter = 12.0

    override fun createSwitcher(): ModelHolder {
        val cylinder = Cylinder(5.0, Radius.fromDiameter(holeDiameter)).rotate(Angles3d.xOnly(90.0))
        return ModelHolder(cylinder, fromModel(cylinder, Color.PINK, 20))
    }

    override fun createSwitcherHole(): Abstract3dModel {
        return Cylinder(10.0, Radius.fromDiameter(holeDiameter)).addModel(
            Cylinder(2.0, Radius.fromDiameter(outerHoleDiameter)).moveZ(-cfg.wallsSettings.borderThickness - 0.5)
        ).rotate(Angles3d.xOnly(90.0))
    }
}
