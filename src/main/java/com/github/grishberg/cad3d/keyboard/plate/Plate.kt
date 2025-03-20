package com.github.grishberg.cad3d.keyboard.plate

import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.casebody.Walls
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.LinearExtrude
import eu.printingin3d.javascad.models.Projection
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.VertexHolder

class Plate(
    private val cfg: KeyboardConfig,
    private val walls: Walls,
) {

    fun create(): ModelHolder {
        val model = createPlateModel()
        return ModelHolder(
            model, VertexHolder.createVertexHolder(model.moveZ(-cfg.plateThickness / 2), Color.MAGENTA, 20)
        )
    }

    private fun createPlateModel(): Abstract3dModel {


        val wallsModel = walls.createWalls(
            bottomBorderHeight = cfg.plateThickness
        ).subtractModel(Cube(300.0, 300.0, 50.0).move(0.0, 0.0, -25.0))

        val projection = Projection(wallsModel)
        val extrude = LinearExtrude(projection,cfg.plateThickness )

        return extrude
    }
}
