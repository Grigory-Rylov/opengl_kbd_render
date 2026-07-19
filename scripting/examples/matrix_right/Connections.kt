package com.github.grishberg.scripting.matrix

import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.tranzitions.Hull
import com.github.grishberg.javascad.tranzitions.Union

fun buildConnections(): Abstract3dModel {
    val models = mutableListOf<Abstract3dModel>()
    val kfc = KeyCfg

    for (col in 0 until kfc.columnsCount - 1) {
        for (row in 0 until kfc.rowsCount - 1) {
            models.add(Hull(
                placeKey(placeHolderFrontRight(), col, row),
                placeKey(placeHolderFrontLeft(), col + 1, row),
                placeKey(placeHolderBackLeft(), col + 1, row + 1),
                placeKey(placeHolderBackRight(), col, row + 1)
            ))
        }
    }

    for (col in 0 until kfc.columnsCount - 1) {
        for (row in 0 until kfc.rowsCount) {
            models.add(Hull(
                placeKey(placeHolderRight(), col, row),
                placeKey(placeHolderLeft(), col + 1, row)
            ))
        }
    }

    for (col in 0 until kfc.columnsCount) {
        for (row in 0 until kfc.rowsCount - 1) {
            models.add(Hull(
                placeKey(placeHolderFront(), col, row),
                placeKey(placeHolderBack(), col, row + 1)
            ))
        }
    }

    models.add(Hull(
        placeThumbR(placeHolderLeft()),
        placeThumbM(placeHolderRight())
    ))
    models.add(Hull(
        placeThumbM(placeHolderLeft()),
        placeThumbL(placeHolderRight())
    ))

    return Union(models)
}
