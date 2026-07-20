package com.github.grishberg.cad3d.keyboard.screws

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.models.Cylinder
import com.github.grishberg.javascad.tranzitions.Union

class ScrewBase(private val cfg: KeyboardConfig) {

    private val holeHeight = 4.0
    private val headDiameter = 5.0

    fun screwHolder(height: Double = 5.0): Model {
        val outerDiameter = cfg.screwNutHoleDiameter + cfg.screwHolderWallhickness * 2.0
        return Cylinder(height, Radius.fromDiameter(outerDiameter)).moveZ(height / 2.0).subtractModel(screwNutHole())
    }

    fun screwNutHole(holeHeight: Double = 4.0): Model {
        return Cylinder(holeHeight, Radius.fromDiameter(cfg.screwNutHoleDiameter)).moveZ(holeHeight / 2.0)
    }

    /**
     * Болт для отверстий в нижней крышке.
     */
    fun plateScrewHolder(): Model {
        return Union(
            Cylinder(cfg.keyPlaceConfig.plateThickness + 1, Radius.fromDiameter(cfg.screwBoltDiameter)),

            Cylinder(cfg.screwHeadHeight,
                Radius.fromDiameter(cfg.screwHeadDiameter + 0.2),
                Radius.fromDiameter(cfg.screwBoltDiameter),
            ).moveZ(-holeHeight/2 + cfg.screwHeadHeight/2),
        )
    }

    fun matrixScrewHole(): Model {
        val headerHoleHeight = 2.0
        val holeHeight = 10.0
        val boltDiameter = 3.1
        return Cylinder(holeHeight, Radius.fromDiameter(boltDiameter)).moveZ(-holeHeight/2 + headerHoleHeight / 2)
            .addModel(Cylinder(headerHoleHeight, Radius.fromDiameter(headDiameter)).moveZ(headerHoleHeight / 2))
    }

}
