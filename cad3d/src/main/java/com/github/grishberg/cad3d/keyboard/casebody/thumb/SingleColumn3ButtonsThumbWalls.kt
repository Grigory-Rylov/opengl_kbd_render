package com.github.grishberg.cad3d.keyboard.casebody.thumb

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.WallsBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.FrontRightToMatrixWallBuilder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.utils.Color

class SingleColumn3ButtonsThumbWalls(
    private val cfg: KeyboardConfig,
    private val keyPlace: KeyPlace,
    private val thumbKeyPlace: ThumbKeyPlace,
    private val frontRightToMatrixWallBuilder: FrontRightToMatrixWallBuilder,
) : ThumbWalls {

    override fun createThumbWalls(
        wallsBuilder: WallsBuilder,
        cornerWallBuilder: CornerWallBuilder,
        verticalOffset: Double,
        leftOffset: Double,
        bottomEdgePatcher: WallBottomEdgePatcher,
        topEdgeOffsetZ: Double,
    ): List<Model> {
        val models = mutableListOf<Model>()
        val wallsSettings = cfg.wallsSettings
        val borderZOffset: Double = -2.0
        //corners
        //left back
        val thumbKeyPlaceL: (Model) -> Model = { o -> thumbKeyPlace.placeL(o) }
        val thumbBackLPoint = verticalCube(
            thumbKeyPlaceL(
                KeyPlaceholder.placeHolderBackLeft()
                    .move(0.0, wallsSettings.verticalOffset, wallsSettings.borderZOffset),
            ), topEdgeOffsetZ
        )

        val backLeftL = thumbKeyPlaceL(
            KeyPlaceholder.placeHolderBackLeft()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val left = thumbKeyPlaceL(
            KeyPlaceholder.placeHolderBackLeft()
                .move(-wallsSettings.outerLeftOffset, 0.0, wallsSettings.outerBorderZOffset)
        )
        val border = hull(
            verticalCube(
                thumbKeyPlaceL(
                    KeyPlaceholder.placeHolderBackLeft()
                        .move(0.0, wallsSettings.verticalOffset, wallsSettings.borderZOffset)
                ), topEdgeOffsetZ
            ),
            verticalCube(
                thumbKeyPlaceL(
                    KeyPlaceholder.placeHolderBackLeft()
                        .move(-wallsSettings.leftOffset, 0.0, wallsSettings.borderZOffset)
                ), topEdgeOffsetZ
            ),
            topBorderObj(left),
        )
        val wall = hull(
            topBorderObj(left),
            thumbBackLPoint,
            bottomEdgePatcher.leftPoint(left),
            bottomEdgePatcher.projection(thumbBackLPoint),
        )
        models.add(wall)
        models.add(border)

        //left front
        models.add(cornerWallBuilder.frontLeft { obj -> thumbKeyPlace.placeL(obj) })
        // right front
        models.addAll(
            frontRightToMatrixWallBuilder.create(
                keyPlace = { obj -> thumbKeyPlace.placeR(obj) },
                matrixOuterPlace = { o -> keyPlace.place(4, cfg.lastRow, o) },
                matrixInnerPlace = { o -> keyPlace.place(3, cfg.lastRow, o) },
            )
        )

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeR(o) })
        //models.add(wallsBuilder.frontWall { o -> ThumbKeyPlace.placeM(o) })
        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeL(o) })

        models.addAll(wallsBuilder.leftWall { o -> thumbKeyPlace.placeL(o) })


        models.add(
            wallsBuilder.midEdge(
                midPlace = { o -> thumbKeyPlace.placeM(o) },
                leftPlace = { o -> thumbKeyPlace.placeL(o) },
                rightPlace = { o -> thumbKeyPlace.placeR(o) },
            )
        )

        val topInnerPoint = keyPlace.place(
            0, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft().move(-leftOffset, 0.0, wallsSettings.borderZOffset)
        )

        val topOuterPoint = keyPlace.place(
            0,
            cfg.lastRow,
            KeyPlaceholder.placeHolderFrontLeft()
                .move(-wallsSettings.outerHorizontalOffset, 0.0, wallsSettings.outerBorderZOffset)
        )

        models.add(
            hull(
                verticalCube(
                    topInnerPoint, topEdgeOffsetZ
                ),

                topBorderObj(
                    topOuterPoint,
                ),

                thumbBackLPoint,

                verticalCube(
                    thumbKeyPlace.placeL(
                        KeyPlaceholder.placeHolderBackRight().move(0.0, verticalOffset, borderZOffset)
                    ), topEdgeOffsetZ
                ),
            ).withColor(Color.YELLOW_GREEN)
        )

        models.add(
            hull(
                topBorderObj(
                    topOuterPoint,
                ),

                thumbBackLPoint,
                bottomEdgePatcher.projection(thumbBackLPoint),

                bottomEdgePatcher.projection(topOuterPoint),
            ).withColor(Color.DARK_RED)
        )

        return models
    }

    //TODO: refactor
    private fun verticalCube(obj: Model, topEdgeOffsetZ: Double): Model {
        return borderObject(cfg.wallsSettings.borderThickness, cfg.wallsSettings.borderHeight).moveZ(topEdgeOffsetZ)
            .move(obj.move)
    }

    private fun topBorderObj(obj: Model): Model {
        return Utils.sphere(cfg.wallsSettings.borderThickness / 2.0).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Model {
        return Utils.cylinder(thickness, height)
    }

}
