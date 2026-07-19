package com.github.grishberg.cad3d.keyboard.matrix

import com.github.grishberg.cad3d.keyboard.Connections
import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.ThumbConnections
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.amoeba.Amoeba
import com.github.grishberg.cad3d.keyboard.casebody.Walls
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbBorders
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbWalls
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.keyboard.screws.ScrewBase
import com.github.grishberg.cad3d.keyboard.screws.ScrewKeyMatrixPlace
import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType
import com.github.grishberg.cad3d.util.fromModel
import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.models.IModel
import com.github.grishberg.javascad.tranzitions.Union
import com.github.grishberg.javascad.utils.Color

class KeyMatrix(
    private val cfg: KeyboardConfig, private val keyPlace: KeyPlace, private val thumbKeyPlace: ThumbKeyPlace
) {

    fun createConnectionsModel(): ModelHolder {
        val connections = Connections(cfg, keyPlace).buildConnections()
        val thumbPlaceConnections = ThumbConnections(cfg, thumbKeyPlace).buildThumbPlaceConnections()

        return ModelHolder(
            connections.addModel(thumbPlaceConnections),
            createVertexHolder(connections, DEFAULT_COLOR),
            createVertexHolder(thumbPlaceConnections, DEFAULT_COLOR)
        )
    }

    fun createBordersModel(
        amoebaHoles: Abstract3dModel?,
        thumbBorders: ThumbBorders,
        thumbWalls: ThumbWalls,
    ): ModelHolder {
        val screwBase = ScrewBase(cfg)
        val screws = ScrewKeyMatrixPlace(cfg, keyPlace, thumbKeyPlace).place(screwBase.matrixScrewHole())

        val borderHeigth = 2.5
        val wallsSettings = WallsSettings(
            borderHeight = borderHeigth, bottomBorderHeight = 4.0

        )
        val bordersModels = Walls(
            cfg,
            wallsSettings,
            keyPlace,
            thumbKeyPlace,
            topEdgeOffsetZ = 0.0,
            thumbBorders = thumbBorders,
            thumbWalls = thumbWalls
        ).createBorders(
            1.5, borderHeigth
        )
        val borders = Union(bordersModels).subtractModel(screws).subtractModel(amoebaHoles)

        return ModelHolder(
            borders,
            createVertexHolder(borders, Color.lightGray),
        )
    }

    fun createPlaceHolder(): Abstract3dModel {
        val amoeba = Amoeba(cfg)
        val amoebaHole = amoeba.createHoles(height = 8.0, diameter = 0.7).addModel(amoeba.createSimple())
        return if (cfg.keyPlaceConfig.keyPlaceholderType == KeyPlaceholderType.AmoebaSu120) {
            KeyPlaceholder.placeHolder(cfg).subtractModel(amoebaHole)
        } else {
            KeyPlaceholder.placeHolder(cfg)
        }
    }

    fun createPlaceholders(): ModelHolder {
        val models = mutableListOf<Abstract3dModel>()

        val placeHolder = createPlaceHolder()

        for (column in 0 until cfg.keyPlaceConfig.columnsCount) {
            for (row in 0 until cfg.keyPlaceConfig.rowsCount) {
                models.add(keyPlace.place(column, row, placeHolder))
            }
        }

        models.add(thumbKeyPlace.thumbPlace(placeHolder))

        val allPlaceholders = Union(models)

        return ModelHolder(
            allPlaceholders,
            createVertexHolder(allPlaceholders, Color(30, 127, 40)),
        )
    }

    private fun createVertexHolder(model: IModel, color: Color): VertexHolder {
        return fromModel(model, color, cfg.fn)
    }

    companion object {

        private val DEFAULT_COLOR = Color.GRAY
    }
}
