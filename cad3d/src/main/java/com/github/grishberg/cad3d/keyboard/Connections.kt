package com.github.grishberg.cad3d.keyboard

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderFront
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderFrontLeft
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderFrontRight
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderLeft
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderRight
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBack
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBackLeft
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBackRight
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.javascad.models.Abstract3dModel

class Connections(private val cfg: KeyboardConfig, private val keyPlace: KeyPlace) {

    private val models = ArrayList<Abstract3dModel>()
    fun buildConnections(): Abstract3dModel {
        models.clear()
        // diagonals
        for (column in 0 until cfg.keyPlaceConfig.columnsCount - 1) {
            for (row in 0 until cfg.keyPlaceConfig.rowsCount - 1) {
                addHull(
                    keyPlace.place(column, row, placeHolderFrontRight()),
                    keyPlace.place(column + 1, row, placeHolderFrontLeft()),
                    keyPlace.place(column + 1, row + 1, placeHolderBackLeft()),
                    keyPlace.place(column, row + 1, placeHolderBackRight())
                )
            }
        }

        //columns
        for (column in 0 until cfg.keyPlaceConfig.columnsCount - 1) {
            for (row in 0 until cfg.keyPlaceConfig.rowsCount) {
                addHull(
                    keyPlace.place(column, row, placeHolderRight()), keyPlace.place(column + 1, row, placeHolderLeft())
                )
            }
        }

        // rows
        for (column in 0 until cfg.keyPlaceConfig.columnsCount) {
            for (row in 0 until cfg.keyPlaceConfig.rowsCount - 1) {
                addHull(
                    keyPlace.place(column, row, placeHolderFront()),
                    keyPlace.place(column, row + 1, placeHolderBack())
                )
            }
        }
        //side
//        for (int row = 0; row < cfg.getRowsCount(); row++) {
//            addHull(
//                keyPlace.place(0, row, placeHolderTopLeft()),
//                keyPlace.place(0, row, placeHolderBottomLeft())
//            );
//
//            addHull(
//                keyPlace.place(cfg.getColumnsCount() - 1, row, placeHolderTopRight()),
//                keyPlace.place(cfg.getColumnsCount() - 1, row, placeHolderBottomRight())
//            );
//        }
//        for (int row = 0; row < cfg.getRowsCount(); row++) {
//            addHull(
//                keyPlace.place(0, row, placeHolderBottomLeft()),
//                keyPlace.place(0, row + 1, placeHolderTopLeft())
//            );
//
//            addHull(
//                keyPlace.place(cfg.getColumnsCount() - 1, row, placeHolderBottomRight()),
//                keyPlace.place(cfg.getColumnsCount() - 1, row + 1, placeHolderTopRight())
//            );
//
//        }
        return Utils.union(models)
    }

    private fun addHull(vararg children: Abstract3dModel) {
        models.add(Utils.hull(*children))
    }
}
