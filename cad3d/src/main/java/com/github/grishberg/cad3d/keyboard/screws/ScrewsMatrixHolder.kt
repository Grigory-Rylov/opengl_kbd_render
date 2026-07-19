package com.github.grishberg.cad3d.keyboard.screws

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.models.Cube
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.models.Hull

class ScrewsMatrixHolder(
    private val cfg: KeyboardConfig,
    private val screwBase: ScrewBase,
) {
    fun create(): Abstract3dModel {
        val verticalOffset = -5.8
        val height = 5.0
        val outerDiameter = cfg.screwNutHoleDiameter + cfg.screwHolderWallhickness * 2.0
        val cylinder = Cylinder(height, Radius.fromDiameter(outerDiameter))
        val border = Cube(4.0, 10.0, height).moveX(-5.0)
        return Hull(border, cylinder)
            //.subtractModel(screwBase.screwNutHole().moveZ(-2.0))
            .moveZ(verticalOffset)

    }

    fun createNutHole(): Abstract3dModel {
        val verticalOffset = -5.0
        return screwBase.screwNutHole(5.0).moveZ(-2.0).moveZ(verticalOffset)

    }

}
