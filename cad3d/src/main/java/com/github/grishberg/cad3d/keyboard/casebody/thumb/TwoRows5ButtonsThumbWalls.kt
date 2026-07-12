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
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.utils.Color

class TwoRows5ButtonsThumbWalls(
    private val cfg: KeyboardConfig,
    private val keyPlace: KeyPlace,
    private val thumbKeyPlace: ThumbKeyPlace,
    private val thumbPoints: ThumbPoints,
    private val frontRightToMatrixWallBuilder: FrontRightToMatrixWallBuilder,
) : ThumbWalls {

    override fun createThumbWalls(
        wallsBuilder: WallsBuilder,
        cornerWallBuilder: CornerWallBuilder,
        verticalOffset: Double,
        leftOffset: Double,
        bottomEdgePatcher: WallBottomEdgePatcher,
        topEdgeOffsetZ: Double,
    ): List<Abstract3dModel> {
        val models = mutableListOf<Abstract3dModel>()
        val wallsSettings = cfg.wallsSettings
        val borderZOffset: Double = -2.0
        //corners
        //left back
        val thumbKeyPlaceL: (Abstract3dModel) -> Abstract3dModel = { o -> thumbKeyPlace.placeL(o) }
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
            border,
        )
        models.add(wall.withColor(Color.ALICE_BLUE))

        //left front
        models.add(cornerWallBuilder.frontLeft { obj -> thumbKeyPlace.placeL2(obj) })
        // right front
        models.addAll(
            frontRightToMatrixWallBuilder.create(
                keyPlace = { obj -> thumbKeyPlace.placeR(obj) },
                matrixOuterPlace = { o -> keyPlace.place(4, cfg.lastRow, o) },
                matrixInnerPlace = { o -> keyPlace.place(3, cfg.lastRow, o) },
            )
        )


        models.addAll(frontRight(topEdgeOffsetZ, bottomEdgePatcher))

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeR2(o) })

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeL2(o) })

        models.addAll(wallsBuilder.leftWall { o -> thumbKeyPlace.placeL(o) })
        models.addAll(wallsBuilder.leftWall { o -> thumbKeyPlace.placeL2(o) })

//        models.add(
//            wallsBuilder.midEdge(
//                midPlace = { o -> thumbKeyPlace.placeM(o) },
//                leftPlace = { o -> thumbKeyPlace.placeL(o) },
//                rightPlace = { o -> thumbKeyPlace.placeR(o) },
//            ).withColor(Color.AQUA_MARINE)
//        )

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
            ).withColor(Color.BROWN)
        )

        return models
    }

    private fun frontRight(topEdgeOffsetZ: Double, bottomEdgePatcher: WallBottomEdgePatcher): List<Abstract3dModel> {
        val rightOuter = thumbKeyPlace.placeR2(
            KeyPlaceholder.placeHolderFrontRight().move(5.0, 0.0, cfg.wallsSettings.outerBorderZOffset)
        )

        val border = hull(
            //inner
            verticalCube(
                thumbKeyPlace.placeR2(
                    KeyPlaceholder.placeHolderFrontRight()
                        .move(0.0, -cfg.wallsSettings.verticalOffset, cfg.wallsSettings.borderZOffset)
                ),
                topEdgeOffsetZ,
            ),
            //right
            verticalCube(
                thumbPoints.row1RightFRInner,
                topEdgeOffsetZ,
            ),

            // outer
            thumbPoints.row2RightFRRightOuter,
        )


        if (cfg.isSkeletonMode) {
            return listOf(
                border, hull(
                    thumbPoints.row2RightFRRightOuter, bottomEdgePatcher.frontPoint(thumbPoints.row2RightFRRightOuter)
                ), hull(rightOuter, bottomEdgePatcher.rightPoint(rightOuter)), hull(
                    bottomEdgePatcher.frontPoint(thumbPoints.row2RightFRRightOuter),
                    bottomEdgePatcher.rightPoint(rightOuter),
                )
            )
        }


        return listOf(
            border.withColor(Color.YELLOW),
        )
    }

    //TODO: refactor
    private fun verticalCube(obj: Abstract3dModel, topEdgeOffsetZ: Double): Abstract3dModel {
        return borderObject(cfg.wallsSettings.borderThickness, cfg.wallsSettings.borderHeight).moveZ(topEdgeOffsetZ)
            .move(obj.move)
    }

    private fun topBorderObj(obj: Abstract3dModel): Abstract3dModel {
        return Utils.sphere(cfg.wallsSettings.borderThickness / 2.0).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }

}
