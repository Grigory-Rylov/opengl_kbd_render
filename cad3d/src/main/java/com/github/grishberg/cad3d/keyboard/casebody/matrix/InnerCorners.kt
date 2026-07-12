package com.github.grishberg.cad3d.keyboard.casebody.matrix

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.openscad.models.Abstract3dModel

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
            verticalCube(keyPlace(KeyPlaceholder.placeHolderBackLeft().move(0.0, verticalOffset, borderZOffset))),
            verticalCube(keyPlace(KeyPlaceholder.placeHolderBackLeft().move(leftOffset, 0.0, borderZOffset))),
            keyPlace(KeyPlaceholder.placeHolderBackLeft())
        )
    }

    override fun backRight(keyPlace: (Abstract3dModel) -> Abstract3dModel): List<Abstract3dModel> {
        return listOf(
            Utils.hull(
                verticalCube(keyPlace(KeyPlaceholder.placeHolderBackRight().move(0.0, verticalOffset, borderZOffset))),
                verticalCube(keyPlace(KeyPlaceholder.placeHolderBackRight().move(rightOffset, 0.0, borderZOffset))),
                keyPlace(KeyPlaceholder.placeHolderBackRight())
            )
        )
    }

    override fun frontLeft(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        return Utils.hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(0.0, -verticalOffset, borderZOffset)
                )
            ), verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(leftOffset, 0.0, borderZOffset)
                )
            ), keyPlace(KeyPlaceholder.placeHolderFrontLeft())
        )
    }

    override fun frontRight(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        return Utils.hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(0.0, -verticalOffset, borderZOffset)
                )
            ), verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(rightOffset, 0.0, borderZOffset)
                )
            ), keyPlace(KeyPlaceholder.placeHolderFrontRight())
        )
    }

    private fun verticalCube(obj: Abstract3dModel): Abstract3dModel {
        return borderObject(borderThickness, borderHeight).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }
}
