package com.github.grishberg.cad3d.keyboard.screws

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.openscad.coords.Angles3d
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.tranzitions.Union

class ScrewKeyMatrixPlace(
    private val cfg: KeyboardConfig,
    private val keyPlace: KeyPlace,
    private val thumbKeyPlace: ThumbKeyPlace,
    private val verticalOffset: Double = 4.0,
    private val leftOffset: Double = -8.0,
    private val rightOffset: Double = 8.0,
) {

    fun place(o: Abstract3dModel): Abstract3dModel {

        val screwHorizontalOffset = (cfg.screwHolderWallhickness * 2 + cfg.screwNutHoleDiameter) / 2
        val screwLeftOffset = leftOffset + screwHorizontalOffset - 1
        val screwRightOffset = rightOffset - screwHorizontalOffset + 1

        val models = mutableListOf<Abstract3dModel>()
        models.add(
            keyPlace.place(
                0, 0, KeyPlaceholder.placeHolderLeft(o)
                    .moveX(screwLeftOffset)
            )
        )

        models.add(
            keyPlace.place(
                0, cfg.lastRow, KeyPlaceholder.placeHolderLeft(o)
                    .moveX(screwLeftOffset)
            )
        )

        models.add(
            keyPlace.place(
                cfg.lastCol, 0, KeyPlaceholder.placeHolderRight(o.rotate(Angles3d.zOnly(180.0)))
                    .moveX(screwRightOffset)
            )
        )
        models.add(
            keyPlace.place(
                cfg.lastCol, cfg.lastRow, KeyPlaceholder.placeHolderRight(o.rotate(Angles3d.zOnly(180.0)))
                    .moveX(screwRightOffset)
            )
        )

        models.add(thumbKeyPlace.placeL(KeyPlaceholder.placeHolderLeft(o).moveX(screwLeftOffset)))

        return Union(models)
    }
}
