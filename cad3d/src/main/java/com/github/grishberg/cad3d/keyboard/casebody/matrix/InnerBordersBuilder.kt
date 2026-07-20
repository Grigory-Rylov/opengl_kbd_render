package com.github.grishberg.cad3d.keyboard.casebody.matrix

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.casebody.WallsBuilder
import com.github.grishberg.javascad.models.Model

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
        onlyBorder: Boolean, keyPlace: (Model) -> Model
    ): Model {
        return Utils.hull(
            keyPlace(KeyPlaceholder.placeHolderBack()),

            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackLeft().move(0.0, verticalOffset, borderZOffset)
                )
            ),

            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackRight().move(0.0, verticalOffset, borderZOffset)
                )
            )
        )
    }

    override fun backMidWall(
        onlyBorder: Boolean,
        leftOffset: Double,
        rightOffset: Double,
        leftPlace: (Model) -> Model,
        rightPlace: (Model) -> Model
    ): Model {
        return Utils.hull(
            leftPlace.invoke(KeyPlaceholder.placeHolderBackRight()),
            rightPlace.invoke(KeyPlaceholder.placeHolderBackLeft()),

            verticalCube(
                leftPlace.invoke(
                    KeyPlaceholder.placeHolderBackRight().move(leftOffset, verticalOffset, borderZOffset)
                )
            ),
            verticalCube(
                rightPlace.invoke(
                    KeyPlaceholder.placeHolderBackLeft().move(rightOffset, verticalOffset, borderZOffset)
                )
            )
        )
    }

    override fun leftWall(
        topOffset: Double, bottomOffset: Double, keyPlace: (Model) -> Model
    ): List<Model> {
        return listOf(
            Utils.hull(
                keyPlace(KeyPlaceholder.placeHolderLeft()),
                verticalCube(
                    keyPlace(
                        KeyPlaceholder.placeHolderBackLeft().move(leftOffset, 0.0, borderZOffset)
                    )
                ),
                verticalCube(
                    keyPlace(
                        KeyPlaceholder.placeHolderFrontLeft().move(leftOffset, 0.0, borderZOffset)
                    )
                ),
            )
        )
    }

    override fun leftMidWall(
        leftPlace: (Model) -> Model, rightPlace: (Model) -> Model
    ): Model {
        return Utils.hull(
            leftPlace(KeyPlaceholder.placeHolderFrontLeft()),
            rightPlace(KeyPlaceholder.placeHolderBackLeft()),
            verticalCube(
                leftPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(leftOffset, 0.0, borderZOffset)
                )
            ),
            verticalCube(
                rightPlace(
                    KeyPlaceholder.placeHolderBackLeft().move(leftOffset, 0.0, borderZOffset)
                )
            ),
        )
    }

    override fun frontWall(
        leftOffset: Double, rightOffset: Double, onlyBottomEdge: Boolean, keyPlace: (Model) -> Model
    ): Model {
        return Utils.hull(
            keyPlace(KeyPlaceholder.placeHolderFront()),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(0.0, -verticalOffset, borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(0.0, -verticalOffset, borderZOffset)
                )
            ),
        )
    }

    override fun frontMidWall(
        leftOffset: Double,
        rightOffset: Double,
        leftPlace: (Model) -> Model,
        rightPlace: (Model) -> Model
    ): Model {
        return Utils.hull(
            leftPlace(KeyPlaceholder.placeHolderFrontRight()),
            rightPlace(KeyPlaceholder.placeHolderFrontLeft()),
            verticalCube(
                leftPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(0.0, -verticalOffset, borderZOffset)
                )
            ),
            verticalCube(
                rightPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(0.0, -verticalOffset, borderZOffset)
                )
            ),
        )
    }

    override fun rightWall(
        topOffset: Double, bottomOffset: Double, keyPlace: (Model) -> Model
    ): Model {
        return Utils.hull(
            keyPlace(KeyPlaceholder.placeHolderRight()),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(rightOffset, 0.0, borderZOffset)
                )
            ),
            verticalCube(keyPlace(KeyPlaceholder.placeHolderBackRight().move(rightOffset, 0.0, borderZOffset))),
        )
    }

    override fun rightMidWall(
        backPlace: (Model) -> Model, frontPlace: (Model) -> Model
    ): Model {
        return Utils.hull(
            backPlace(KeyPlaceholder.placeHolderFrontRight()),
            frontPlace(KeyPlaceholder.placeHolderBackRight()),
            verticalCube(
                backPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(rightOffset, 0.0, borderZOffset)
                )
            ),
            verticalCube(
                frontPlace(
                    KeyPlaceholder.placeHolderBackRight().move(rightOffset, 0.0, borderZOffset)
                )
            )
        )
    }

    override fun midEdge(
        midPlace: (Model) -> Model,
        leftPlace: (Model) -> Model,
        rightPlace: (Model) -> Model
    ) = Utils.hull(
        verticalCube(
            thumbKeyPlace.placeM(
                KeyPlaceholder.placeHolderFrontLeft().move(0.0, -verticalOffset, borderZOffset)
            )
        ),
        verticalCube(
            thumbKeyPlace.placeM(
                KeyPlaceholder.placeHolderFrontRight().move(0.0, -verticalOffset, borderZOffset)
            )
        ),
        verticalCube(
            thumbKeyPlace.placeL(
                KeyPlaceholder.placeHolderFrontRight().move(0.0, -verticalOffset, borderZOffset)
            )
        ),
        verticalCube(
            thumbKeyPlace.placeR(
                KeyPlaceholder.placeHolderFrontLeft().move(0.0, -verticalOffset, borderZOffset)
            )
        ),
    )

    override fun rightDiagonal(
        backKeyPlace: (Model) -> Model,
        frontKeyPlace: (Model) -> Model,
    ): Model {
        return Utils.hull(
            backKeyPlace(KeyPlaceholder.placeHolderFrontRight()),
            frontKeyPlace(KeyPlaceholder.placeHolderFrontRight()),
            verticalCube(
                backKeyPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(rightOffset, 0.0, borderZOffset)
                )
            ),
            verticalCube(frontKeyPlace(KeyPlaceholder.placeHolderFrontRight().move(rightOffset, 0.0, borderZOffset))),
        )
    }

    private fun verticalCube(obj: Model): Model {
        return borderObject(borderThickness, borderHeight).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Model {
        return Utils.cylinder(thickness, height)
    }

    private fun topBorderObject(): Model {
        return Utils.cylinder(borderThickness, borderHeight)
    }
}
