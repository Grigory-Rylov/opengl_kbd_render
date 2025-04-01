package com.github.grishberg.cad3d.keyboard.casebody.matrix

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.casebody.WallsBuilder
import eu.printingin3d.javascad.models.Abstract3dModel

class InnerBordersBuilder(
    private val thumbKeyPlace: ThumbKeyPlace,
    private val borderThickness: Double = 1.5,
    private val borderHeight: Double = 4.0,
    private val verticalOffset: Double = 4.0,
    private val leftOffset: Double = -8.0,
    private val rightOffset: Double = 8.0,
    private val borderZOffset: Double = -2.0,

    ) : WallsBuilder {

    override fun backWall(
        onlyBorder: Boolean,
        leftVerticalOffset: Double?,
        rightVerticalOffset: Double?,
        keyPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {

        val leftOffsetY = leftVerticalOffset?: verticalOffset
        val rightOffsetY = rightVerticalOffset?: verticalOffset

        return Utils.hull(
            keyPlace(KeyPlaceholder.placeHolderTop()),

            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderTopLeft().move(0.0, leftOffsetY, borderZOffset)
                )
            ),

            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderTopRight().move(0.0, rightOffsetY, borderZOffset)
                )
            )
        )
    }

    override fun backMidWall(
        onlyBorder: Boolean,
        leftVerticalOffset: Double?,
        rightVerticalOffset: Double?,
        leftPlace: (Abstract3dModel) -> Abstract3dModel, rightPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {

        val leftOffsetY = leftVerticalOffset?: verticalOffset
        val rightOffsetY = rightVerticalOffset?: verticalOffset

        return Utils.hull(
            leftPlace.invoke(KeyPlaceholder.placeHolderTopRight()),
            rightPlace.invoke(KeyPlaceholder.placeHolderTopLeft()),

            verticalCube(
                leftPlace.invoke(
                    KeyPlaceholder.placeHolderTopRight().move(0.0, leftOffsetY, borderZOffset)
                )
            ),
            verticalCube(
                rightPlace.invoke(
                    KeyPlaceholder.placeHolderTopLeft().move(0.0, rightOffsetY, borderZOffset)
                )
            )
        )
    }

    override fun leftWall(
        topOffset: Double, bottomOffset: Double, keyPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        return Utils.hull(
            keyPlace(KeyPlaceholder.placeHolderLeft()),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderTopLeft().move(leftOffset, 0.0, borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomLeft().move(leftOffset, 0.0, borderZOffset)
                )
            ),
        )
    }

    override fun leftMidWall(
        leftPlace: (Abstract3dModel) -> Abstract3dModel, rightPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        return Utils.hull(
            leftPlace(KeyPlaceholder.placeHolderBottomLeft()),
            rightPlace(KeyPlaceholder.placeHolderTopLeft()),
            verticalCube(
                leftPlace(
                    KeyPlaceholder.placeHolderBottomLeft().move(leftOffset, 0.0, borderZOffset)
                )
            ),
            verticalCube(
                rightPlace(
                    KeyPlaceholder.placeHolderTopLeft().move(leftOffset, 0.0, borderZOffset)
                )
            ),
        )
    }

    override fun frontWall(
        leftOffset: Double, rightOffset: Double, onlyBottomEdge: Boolean, keyPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        return Utils.hull(
            keyPlace(KeyPlaceholder.placeHolderBottom()),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomLeft().move(0.0, -verticalOffset, borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomRight().move(0.0, -verticalOffset, borderZOffset)
                )
            ),
        )
    }

    override fun frontMidWall(
        leftOffset: Double,
        rightOffset: Double,
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        return Utils.hull(
            leftPlace(KeyPlaceholder.placeHolderBottomRight()),
            rightPlace(KeyPlaceholder.placeHolderBottomLeft()),
            verticalCube(
                leftPlace(
                    KeyPlaceholder.placeHolderBottomRight().move(0.0, -verticalOffset, borderZOffset)
                )
            ),
            verticalCube(
                rightPlace(
                    KeyPlaceholder.placeHolderBottomLeft().move(0.0, -verticalOffset, borderZOffset)
                )
            ),
        )
    }

    override fun rightWall(
        topOffset: Double, bottomOffset: Double, keyPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        return Utils.hull(
            keyPlace(KeyPlaceholder.placeHolderRight()),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomRight().move(rightOffset, 0.0, borderZOffset)
                )
            ),
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopRight().move(rightOffset, 0.0, borderZOffset))),
        )
    }

    override fun rightMidWall(
        backPlace: (Abstract3dModel) -> Abstract3dModel, frontPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        return Utils.hull(
            backPlace(KeyPlaceholder.placeHolderBottomRight()),
            frontPlace(KeyPlaceholder.placeHolderTopRight()),
            verticalCube(
                backPlace(
                    KeyPlaceholder.placeHolderBottomRight().move(rightOffset, 0.0, borderZOffset)
                )
            ),
            verticalCube(
                frontPlace(
                    KeyPlaceholder.placeHolderTopRight().move(rightOffset, 0.0, borderZOffset)
                )
            )
        )
    }

    override fun midEdge(
        midPlace: (Abstract3dModel) -> Abstract3dModel,
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel
    ) = Utils.hull(
        verticalCube(
            thumbKeyPlace.placeM(
                KeyPlaceholder.placeHolderBottomLeft().move(0.0, -verticalOffset, borderZOffset)
            )
        ),
        verticalCube(
            thumbKeyPlace.placeM(
                KeyPlaceholder.placeHolderBottomRight().move(0.0, -verticalOffset, borderZOffset)
            )
        ),
        verticalCube(
            thumbKeyPlace.placeL(
                KeyPlaceholder.placeHolderBottomRight().move(0.0, -verticalOffset, borderZOffset)
            )
        ),
        verticalCube(
            thumbKeyPlace.placeR(
                KeyPlaceholder.placeHolderBottomLeft().move(0.0, -verticalOffset, borderZOffset)
            )
        ),
    )

    private fun verticalCube(obj: Abstract3dModel): Abstract3dModel {
        return borderObject(borderThickness, borderHeight).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }
}
