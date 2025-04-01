package com.github.grishberg.cad3d.keyboard.amoeba

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import eu.printingin3d.javascad.basic.Radius
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.Cylinder

class Amoeba(private val cfg: KeyboardConfig) {

    private val width = 17.8
    private val height = 17.8
    private val thikness = 1.55
    private val holeDistance = 14.0
    private val holeDiameter = 1.4

    private val bottomOffset = if (cfg.isLowProfile) -thikness / 2 else -thikness / 2 - 1.0

    fun create(): Abstract3dModel {
        return Cube(width, height, thikness).moveZ(bottomOffset).subtractModel(createHoles())
    }

    fun createHoles(height: Double = 4.0, diameter: Double = holeDiameter): Abstract3dModel {
        val hole = Cylinder(height, Radius.fromDiameter(diameter))
        return hole.move(V3d(-holeDistance / 2, -holeDistance / 2, 0.0)).addModel(
            hole.move(V3d(-holeDistance / 2, holeDistance / 2, 0.0))
        ).addModel(
            hole.move(V3d(holeDistance / 2, holeDistance / 2, 0.0))
        ).addModel(
            hole.move(V3d(holeDistance / 2, -holeDistance / 2, 0.0))
        ).moveZ(-2)
    }

    fun createSimple(): Abstract3dModel {
        val heightDelta = 4.0
        return Cube(width + 1, height + 1, thikness + heightDelta).moveZ(bottomOffset - (heightDelta) / 2)
    }
}

