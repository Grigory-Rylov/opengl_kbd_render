package com.github.grishberg.cad3d.keyboard.casebody.matrix

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.Offset
import eu.printingin3d.javascad.models.Abstract3dModel

class InnerCorners(
    private val borderThickness: Double = 1.5,
    private val borderHeight: Double = 4.0,
    private val verticalOffset: Double = 4.0,
    private val leftOffset: Double = -8.0,
    private val rightOffset: Double = 8.0,
    private val borderZOffset: Double = -2.0,
) : CornerWallBuilder {

    override fun backLeft(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        return Utils.hull(
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopLeft().move(0.0, verticalOffset, borderZOffset))),
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopLeft().move(leftOffset, 0.0, borderZOffset))),
            keyPlace(KeyPlaceholder.placeHolderTopLeft())
        )
    }

    override fun backRight(offset: Offset?, keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        val x = offset?.right ?: rightOffset
        val y = offset?.top ?:verticalOffset
        return Utils.hull(
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopRight().move(0.0, y, borderZOffset))),
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopRight().move(x, 0.0, borderZOffset))),
            keyPlace(KeyPlaceholder.placeHolderTopRight())
        )
    }

    override fun frontLeft(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        return Utils.hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomLeft().move(0.0, -verticalOffset, borderZOffset)
                )
            ), verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomLeft().move(leftOffset, 0.0, borderZOffset)
                )
            ), keyPlace(KeyPlaceholder.placeHolderBottomLeft())
        )
    }

    override fun frontRight(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        return Utils.hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomRight().move(0.0, -verticalOffset, borderZOffset)
                )
            ), verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomRight().move(rightOffset, 0.0, borderZOffset)
                )
            ), keyPlace(KeyPlaceholder.placeHolderBottomRight())
        )
    }

    private fun verticalCube(obj: Abstract3dModel): Abstract3dModel {
        return borderObject(borderThickness, borderHeight).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }
}
