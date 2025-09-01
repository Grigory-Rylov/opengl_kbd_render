package com.github.grishberg.cad3d.keyboard.matrix

import com.github.grishberg.cad3d.keyboard.Connections
import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ModelHolder
import com.github.grishberg.cad3d.keyboard.ThumbConnections
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.amoeba.Amoeba
import com.github.grishberg.cad3d.keyboard.casebody.Walls
import com.github.grishberg.cad3d.plugin.cfg.KeyPlaceholderType
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.keyboard.screws.ScrewBase
import com.github.grishberg.cad3d.keyboard.screws.ScrewKeyMatrixPlace
import com.github.grishberg.cad3d.plugin.VertexHolder
import com.github.grishberg.cad3d.util.fromModel
import eu.printingin3d.javascad.coords.Angles3d
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Cube
import eu.printingin3d.javascad.models.IModel
import eu.printingin3d.javascad.tranzitions.Union
import eu.printingin3d.javascad.utils.Color

class KeyMatrix(
    private val cfg: KeyboardConfig,
    private val keyPlace: KeyPlace,
    private val thumbKeyPlace: ThumbKeyPlace
) {

    fun createConnectionsModel(): ModelHolder {
        val connections = Connections(cfg, keyPlace).buildConnections()
        val thumbPlaceConnections = ThumbConnections(thumbKeyPlace).buildThumbPlaceConnections()

        return ModelHolder(
            connections.addModel(thumbPlaceConnections),
            createVertexHolder(connections, DEFAULT_COLOR),
            createVertexHolder(thumbPlaceConnections, DEFAULT_COLOR)
        )
    }

    fun createBordersModel(amoebaHoles: Abstract3dModel?): ModelHolder {
        val screwBase = ScrewBase(cfg)
        val screws = ScrewKeyMatrixPlace(cfg, keyPlace, thumbKeyPlace).place(screwBase.matrixScrewHole())

        val borderHeigth = 2.5
        val wallsSettings = WallsSettings(
            borderHeight = borderHeigth, bottomBorderHeight = 4.0

        )
        val borders = Walls(cfg, wallsSettings, keyPlace, thumbKeyPlace, topEdgeOffsetZ = 0.0).createBorders(
            1.5, borderHeigth
        ).subtractModel(screws).subtractModel(amoebaHoles)
            //.addModel(Cube(130.0, 200.0, 100.0).move(0.0, 0.0, 20.0).rotate(Angles3d.zOnly(-15.0)))

        return ModelHolder(
            borders,
            createVertexHolder(borders, Color.lightGray),
        )
    }

    fun createPlaceholders(): ModelHolder {
        val models = mutableListOf<Abstract3dModel>()

        val amoeba = Amoeba(cfg)
        val amoebaHole = amoeba.createHoles(height = 7.0, diameter = 0.7).addModel(amoeba.createSimple())

        val placeHolder = if (cfg.keyPlaceholderType == KeyPlaceholderType.AmoebaSu120) {
            KeyPlaceholder.placeHolder(cfg).subtractModel(amoebaHole)
        } else {
            KeyPlaceholder.placeHolder(cfg)
        }

        for (column in 0 until cfg.columnsCount) {
            for (row in 0 until cfg.rowsCount) {
                models.add(keyPlace.place(column, row, placeHolder))
            }
        }

        models.add(thumbKeyPlace.thumbPlace(placeHolder))

        val allPlaceholders = Union(models)

        //saveModel("placeHolder.stl", placeHolder)

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
