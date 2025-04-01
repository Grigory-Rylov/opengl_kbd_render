package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.casebody.matrix.InnerBordersBuilder
import com.github.grishberg.cad3d.keyboard.casebody.matrix.InnerCorners
import com.github.grishberg.cad3d.keyboard.casebody.wall.ControllerHolderWall
import com.github.grishberg.cad3d.keyboard.casebody.wall.ControllerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.OuterCornersWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.OuterWallsBuilder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import eu.printingin3d.javascad.models.Abstract3dModel

class Walls(
    private val cfg: KeyboardConfig,
    private val wallsSettings: WallsSettings,
    private val keyPlace: KeyPlace,
    private val thumbKeyPlace: ThumbKeyPlace,
    private val topEdgeOffsetZ: Double,
    private val wallVerticalOffset: Double = 5.0,
    private val wallHorizontalOffset: Double = 10.0,
    private val thumbTopRightCornerTopOffset: Double = 2.0,
) {

    private val models = ArrayList<Abstract3dModel>()
    val thumbRightOffset = 4.0
    val thumbOuterRightOffset = 7.0

    fun createBorders(borderThickness: Double = 1.5, borderHeight: Double): Abstract3dModel {
        models.clear()

        //columns
        val wallsBuilder = InnerBordersBuilder(
            thumbKeyPlace = thumbKeyPlace,
            rightOffset = 4.0,
            borderThickness = borderThickness,
            borderHeight = borderHeight
        )
        thumbBorders(
            wallsBuilder,
            InnerCorners(rightOffset = 4.0, borderThickness = borderThickness, borderHeight = borderHeight)
        )

        matrixBorders(
            InnerBordersBuilder(
                thumbKeyPlace = thumbKeyPlace, borderThickness = borderThickness, borderHeight = borderHeight
            ), InnerCorners(borderThickness = borderThickness, borderHeight = borderHeight)
        )

        betweenThumbAndMatrixBorders(wallsBuilder, borderThickness, borderHeight)

        return Utils.union(models)
    }

    fun createWalls(
        bottomBorderHeight: Double,
    ): Abstract3dModel {
        models.clear()

        val bottomEdgePatcher = CircleBottomEdgePatcher(
            thickness = 1.5,
            objectHeight = bottomBorderHeight,
            radiusX = 100.0,
            radiusY = 80.0,
            centerY = -35.0,
        )
        val controllerHolderWall = ControllerHolderWall(cfg.wallsSettings, keyPlace)
        val controllerWallBuilder = ControllerWallBuilder(
            controllerHolderWall = controllerHolderWall,
            keyPlace = keyPlace,
            isSkeletonMode = cfg.isSkeletonMode,
            topEdgeOffsetZ = topEdgeOffsetZ,
            cfg = cfg.wallsSettings,
            bottomEdgePatcher = bottomEdgePatcher,
        )

        models.add(
            controllerWallBuilder.createWall()
        )

        matrixBorders(
            OuterWallsBuilder(
                isSkeletonMode = cfg.isSkeletonMode,
                topEdgeOffsetZ = topEdgeOffsetZ,
                cfg = wallsSettings,
                bottomEdgePatcher = bottomEdgePatcher
            ), OuterCornersWallBuilder(
                isSkeletonMode = cfg.isSkeletonMode,
                topEdgeOffsetZ = topEdgeOffsetZ,
                cfg = wallsSettings,
                bottomEdgePatcher = bottomEdgePatcher
            ), isWallMode = true
        )

        thumbWalls(
            OuterWallsBuilder(
                isSkeletonMode = cfg.isSkeletonMode,
                topEdgeOffsetZ = topEdgeOffsetZ,
                cfg = wallsSettings,
            ),
            OuterCornersWallBuilder(
                isSkeletonMode = cfg.isSkeletonMode,
                topEdgeOffsetZ = topEdgeOffsetZ,
                cfg = wallsSettings,
            ),
            verticalOffset = wallVerticalOffset,
            leftOffset = wallHorizontalOffset,
            rightOffset = thumbRightOffset,
            outerRightOffset = thumbOuterRightOffset,
            bottomEdgePatcher = bottomEdgePatcher,
        )

        return Utils.union(models)
    }

    private fun matrixBorders(
        wallsBuilder: WallsBuilder, cornerWallBuilder: CornerWallBuilder, isWallMode: Boolean = false
    ) {
        // corners
        //left back
        if (!isWallMode) {
            models.add(cornerWallBuilder.backLeft { obj ->
                keyPlace.place(
                    0, 0, obj
                )
            })
        }

        //left front
        //models.add(cornerWallBuilder.frontLeft { obj -> keyPlace.place(0, cfg.lastRow, obj) })
        // right back
        models.add(cornerWallBuilder.backRight { obj ->
            keyPlace.place(
                cfg.lastCol, 0, obj
            )
        })
        // right front
        models.add(cornerWallBuilder.frontRight { obj ->
            keyPlace.place(
                cfg.lastCol, cfg.lastRow, obj
            )
        })

        for (column in 0 until cfg.columnsCount) {

            // back columns
            val onlyBorder = isWallMode && column < 2
            models.add(wallsBuilder.backWall(onlyBorder) { obj ->
                keyPlace.place(
                    column, 0, obj
                )
            })
            if (column < 2) {
                continue
            }

            if (isWallMode) {
                continue
            }
            //front columns
            models.add(wallsBuilder.frontWall { obj ->
                keyPlace.place(
                    column, cfg.lastRow, obj
                )
            })

        }
        for (column in 0 until cfg.columnsCount - 1) {
            // back diagonals
            val onlyBorder = isWallMode && column < 2
            models.add(
                wallsBuilder.backMidWall(
                    onlyBorder = onlyBorder,
                    leftPlace = { obj -> keyPlace.place(column, 0, obj) },
                    rightPlace = { obj -> keyPlace.place(column + 1, 0, obj) },
                )
            )
            if (column < 2) {
                continue
            }
            if (isWallMode) {
                continue
            }
            // front diagonals
            models.add(
                wallsBuilder.frontMidWall(
                    leftPlace = { obj -> keyPlace.place(column, cfg.lastRow, obj) },
                    rightPlace = { obj -> keyPlace.place(column + 1, cfg.lastRow, obj) },
                )
            )

        }

        for (row in 0 until cfg.rowsCount) {
            //left
            models.add(wallsBuilder.leftWall { obj ->
                keyPlace.place(
                    0, row, obj
                )
            })
            //right
            models.add(wallsBuilder.rightWall { obj ->
                keyPlace.place(
                    cfg.lastCol, row, obj
                )
            })

        }
        for (row in 0 until cfg.rowsCount - 1) {
            models.add(
                wallsBuilder.leftMidWall(
                    leftPlace = { obj -> keyPlace.place(0, row, obj) },
                    rightPlace = { obj -> keyPlace.place(0, row + 1, obj) },
                )
            )

            models.add(
                wallsBuilder.rightMidWall(
                    backPlace = { obj -> keyPlace.place(cfg.lastCol, row, obj) },
                    frontPlace = { obj -> keyPlace.place(cfg.lastCol, row + 1, obj) },
                )
            )
        }

        if (isWallMode) {
            // front diagonals
            models.add(
                wallsBuilder.frontMidWall(
                    leftOffset = -4.0,
                    rightOffset = -4.0,
                    leftPlace = { obj -> keyPlace.place(4, cfg.lastRow, obj) },
                    rightPlace = { obj -> keyPlace.place(5, cfg.lastRow, obj) },
                )
            )

            models.add(wallsBuilder.frontWall(
                rightOffset = -4.0
            ) { obj -> keyPlace.place(4, cfg.lastRow, obj) })

            models.add(wallsBuilder.frontWall(
                leftOffset = -4.0,
            ) { obj -> keyPlace.place(5, cfg.lastRow, obj) })

            if (cfg.isSkeletonMode) {
                val topOffset = 0.0
                val bottomOffset = 0.0

                //left
                models.add(wallsBuilder.leftWall(topOffset = topOffset, bottomOffset = bottomOffset) { obj ->
                    keyPlace.place(0, 0, obj)
                })

                models.add(wallsBuilder.leftWall(topOffset = topOffset, bottomOffset = bottomOffset) { obj ->
                    keyPlace.place(0, cfg.lastRow, obj)
                })

                //right
                models.add(wallsBuilder.rightWall(topOffset = topOffset, bottomOffset = bottomOffset) { obj ->
                    keyPlace.place(
                        cfg.lastCol, 0, obj
                    )
                })

                models.add(wallsBuilder.rightWall(topOffset = topOffset, bottomOffset = bottomOffset) { obj ->
                    keyPlace.place(
                        cfg.lastCol, cfg.lastRow, obj
                    )
                })
            }
        }
    }

    private fun thumbBorders(
        wallsBuilder: WallsBuilder, cornerWallBuilder: CornerWallBuilder, isWallMode: Boolean = false
    ) {
        //corners
        //left back
        models.add(cornerWallBuilder.backLeft { obj -> thumbKeyPlace.placeL(obj) })
        //left front
        models.add(cornerWallBuilder.frontLeft { obj -> thumbKeyPlace.placeL(obj) })
        // right back
        models.add(cornerWallBuilder.backRight(offset = Offset(top = 2.0)) { obj -> thumbKeyPlace.placeR(obj) })
        // right front
        models.add(cornerWallBuilder.frontRight { obj -> thumbKeyPlace.placeR(obj) })

        //models.add(wallsBuilder.backWall { o -> ThumbKeyPlace.placeR(o) })
        //models.add(wallsBuilder.backWall { o -> ThumbKeyPlace.placeM(o) })
        models.add(wallsBuilder.backWall { o -> thumbKeyPlace.placeL(o) })

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeR(o) })
        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeM(o) })
        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeL(o) })

        models.add(wallsBuilder.leftWall { o -> thumbKeyPlace.placeL(o) })
        models.add(wallsBuilder.rightWall { o -> thumbKeyPlace.placeR(o) })

        if (!isWallMode) {
            models.add(
                wallsBuilder.frontMidWall(
                    leftPlace = { o -> thumbKeyPlace.placeM(o) },
                    rightPlace = { o -> thumbKeyPlace.placeR(o) },
                )
            )
            models.add(
                wallsBuilder.frontMidWall(
                    leftPlace = { o -> thumbKeyPlace.placeL(o) },
                    rightPlace = { o -> thumbKeyPlace.placeM(o) },
                )
            )

            models.add(
                wallsBuilder.backMidWall(
                    leftPlace = { o -> thumbKeyPlace.placeL(o) },
                    rightPlace = { o -> thumbKeyPlace.placeM(o) },
                )
            )
        }

        models.add(
            wallsBuilder.midEdge(
                midPlace = { o -> thumbKeyPlace.placeM(o) },
                leftPlace = { o -> thumbKeyPlace.placeL(o) },
                rightPlace = { o -> thumbKeyPlace.placeR(o) },
            )
        )
    }

    private fun thumbWalls(
        wallsBuilder: WallsBuilder,
        cornerWallBuilder: CornerWallBuilder,
        borderThickness: Double = 1.5,
        borderHeight: Double = 4.0,
        verticalOffset: Double = 4.0,
        leftOffset: Double = 8.0,
        rightOffset: Double = 8.0,
        borderZOffset: Double = -2.0,

        outerVerticalOffset: Double = 10.0,
        outerLeftOffset: Double = 15.0,
        outerRightOffset: Double = 15.0,
        outerBorderZOffset: Double = -6.0,
        bottomEdgePatcher: WallBottomEdgePatcher,
    ) {

        //corners
        //left back
        models.add(cornerWallBuilder.backLeft { obj -> thumbKeyPlace.placeL(obj) })
        //left front
        models.add(cornerWallBuilder.frontLeft { obj -> thumbKeyPlace.placeL(obj) })
        // right front
        models.add(
            cornerWallBuilder.frontRightToMatrix(

                keyPlace = { obj -> thumbKeyPlace.placeR(obj) },
                matrixOuterPlace = { o -> keyPlace.place(4, cfg.lastRow, o) },
                matrixInnerPlace = { o -> keyPlace.place(3, cfg.lastRow, o) },
            )
        )

        models.add(wallsBuilder.backWall(onlyBorder = true) { o -> thumbKeyPlace.placeL(o) })

        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeR(o) })
        //models.add(wallsBuilder.frontWall { o -> ThumbKeyPlace.placeM(o) })
        models.add(wallsBuilder.frontWall { o -> thumbKeyPlace.placeL(o) })

        models.add(wallsBuilder.leftWall { o -> thumbKeyPlace.placeL(o) })


        models.add(
            wallsBuilder.midEdge(
                midPlace = { o -> thumbKeyPlace.placeM(o) },
                leftPlace = { o -> thumbKeyPlace.placeL(o) },
                rightPlace = { o -> thumbKeyPlace.placeR(o) },
            )
        )

        val topInnerPoint = keyPlace.place(
            0, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft().move(-leftOffset, 0.0, borderZOffset)
        )

        val topOuterPoint = keyPlace.place(
            0, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft().move(-outerLeftOffset, 0.0, outerBorderZOffset)
        )

        models.add(
            hull(
                verticalCube(
                    thumbKeyPlace.placeL(
                        KeyPlaceholder.placeHolderTopRight().move(0.0, outerVerticalOffset, outerBorderZOffset)
                    ), borderThickness, borderHeight
                ),
                verticalCube(
                    thumbKeyPlace.placeL(
                        KeyPlaceholder.placeHolderTopLeft().move(0.0, outerVerticalOffset, outerBorderZOffset)
                    ), borderThickness, borderHeight
                ),
                thumbKeyPlace.placeL(
                    KeyPlaceholder.placeHolderTopLeft().move(0.0, outerVerticalOffset, outerBorderZOffset)
                ),

                verticalCube(
                    topInnerPoint, borderThickness, borderHeight
                ),

                topOuterPoint,
            )
        )

        models.add(
            hull(
                topInnerPoint,

                thumbKeyPlace.placeL(
                    KeyPlaceholder.placeHolderTopRight().move(0.0, outerVerticalOffset, outerBorderZOffset)
                ),

                verticalCube(
                    thumbKeyPlace.placeL(
                        KeyPlaceholder.placeHolderTopRight().move(0.0, verticalOffset + 2, borderZOffset)
                    ), borderThickness, borderHeight
                ),
            )
        )

        models.add(
            hull(
                topOuterPoint,
                bottomEdgePatcher.projection(topOuterPoint),
                bottomEdgePatcher.projection(
                    thumbKeyPlace.placeL(
                        KeyPlaceholder.placeHolderTopLeft().move(0.0, outerVerticalOffset, outerBorderZOffset)
                    ),
                ),
            )
        )
    }

    private fun betweenThumbAndMatrixBorders(
        wallsBuilder: WallsBuilder, borderThickness: Double, borderHeight: Double
    ) {
        val verticalOffset = 4.0
        val leftOffset = -8.0
        val rightOffset = 4.0
        val borderZOffset = -2.0

        // edge
        models.add(
            hull(
                verticalCube(
                    thumbKeyPlace.placeM(
                        KeyPlaceholder.placeHolderTopLeft().move(0.0, verticalOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),

                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft()),

                verticalCube(
                    keyPlace.place(
                        0, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft().move(leftOffset, 0.0, borderZOffset)
                    ), borderThickness, borderHeight
                )
            )
        )

        // thumb mid

        models.add(
            hull(
                verticalCube(
                    thumbKeyPlace.placeM(
                        KeyPlaceholder.placeHolderTopLeft().move(0.0, verticalOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),

                verticalCube(
                    thumbKeyPlace.placeM(
                        KeyPlaceholder.placeHolderTopRight().move(0.0, thumbTopRightCornerTopOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),

                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft()),
                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight()),
            )
        )

//        models.add(
//            hull(
//                verticalCube(
//                    thumbKeyPlace.placeR(
//                        KeyPlaceholder.placeHolderTopLeft().move(0.0, verticalOffset, borderZOffset)
//                    ), borderThickness, borderHeight
//                ),
//
//                verticalCube(
//                    thumbKeyPlace.placeR(
//                        KeyPlaceholder.placeHolderTopRight().move(0.0, thumbTopRightCornerTopOffset, borderZOffset)
//                    ), borderThickness, borderHeight
//                ),
//
//                keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft()),
//                keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight()),
//            )
//        )

//        models.add(
//            hull(
//                thumbKeyPlace.placeM(KeyPlaceholder.placeHolderTop()),
//                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderBottom()),
//            )
//        )

        // mid
        models.add(wallsBuilder.backWall(
            onlyBorder = true, rightVerticalOffset = thumbTopRightCornerTopOffset
        ) { o -> thumbKeyPlace.placeM(o) })


        models.add(wallsBuilder.backWall(
            onlyBorder = true,
            leftVerticalOffset = thumbTopRightCornerTopOffset,
            rightVerticalOffset = thumbTopRightCornerTopOffset,
        ) { o -> thumbKeyPlace.placeR(o) })

        models.add(
            wallsBuilder.backMidWall(
                onlyBorder = true,
                leftVerticalOffset = thumbTopRightCornerTopOffset,
                rightVerticalOffset = thumbTopRightCornerTopOffset,
                leftPlace = { o -> thumbKeyPlace.placeM(o) },
                rightPlace = { o -> thumbKeyPlace.placeR(o) },
            )
        )

//        models.add(
//            hull(
//                thumbKeyPlace.placeM(KeyPlaceholder.placeHolderTopLeft()),
//                thumbKeyPlace.placeM(
//                    KeyPlaceholder.placeHolderTopLeft()
//                        .move(0.0, thumbTopRightCornerTopOffset, wallsSettings.borderZOffset)
//                ),
//            )
//        )

//        models.add(
//            hull(
//                thumbKeyPlace.placeM(KeyPlaceholder.placeHolderTopRight().moveY(thumbTopRightCornerTopOffset)),
//                thumbKeyPlace.placeR(KeyPlaceholder.placeHolderTopLeft().moveY(thumbTopRightCornerTopOffset)),
//
//                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight()),
//            )
//        )

        // thumb R

//        models.add(
//            hull(
//                thumbKeyPlace.placeR(KeyPlaceholder.placeHolderTopLeft()),
//
//                verticalCube(
//                    thumbKeyPlace.placeR(
//                        KeyPlaceholder.placeHolderTopRight().move(0.0, thumbTopRightCornerTopOffset, borderZOffset)
//                    ), borderThickness, borderHeight
//                ),
//                //keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight()),
//            )
//        )

//        models.add(
//            hull(
//                keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight()),
//                keyPlace.place(
//                    1, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight().moveY(-thumbTopRightCornerTopOffset)
//                ),
//                keyPlace.place(2, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft()),
//            )
//        )
        models.add(
            hull(
                keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight()),
                keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft()),
                keyPlace.place(
                    1,
                    cfg.lastRow,
                    KeyPlaceholder.placeHolderBottomLeft()
                        .move(0, -thumbTopRightCornerTopOffset, wallsSettings.borderZOffset)
                ),
                keyPlace.place(
                    1,
                    cfg.lastRow,
                    KeyPlaceholder.placeHolderBottomRight()
                        .move(0, -thumbTopRightCornerTopOffset, wallsSettings.borderZOffset)
                ),
            )
        )
        models.add(
            hull(
                keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft()),
                keyPlace.place(
                    1,
                    cfg.lastRow,
                    KeyPlaceholder.placeHolderBottomLeft()
                        .move(0, -thumbTopRightCornerTopOffset, wallsSettings.borderZOffset)
                ),
                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight()),
            )
        )

        models.add(
            hull(
                keyPlace.place(
                    1,
                    cfg.lastRow,
                    KeyPlaceholder.placeHolderBottom().move(0.0, -thumbTopRightCornerTopOffset, borderZOffset)
                ),
                verticalCube(
                    thumbKeyPlace.placeR(
                        KeyPlaceholder.placeHolderTopRight().move(0.0, thumbTopRightCornerTopOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),
            )
        )

//        models.add(
//            hull(
//                verticalCube(
//                    thumbKeyPlace.placeR(
//                        KeyPlaceholder.placeHolderTopRight().move(0.0, thumbTopRightCornerTopOffset, borderZOffset)
//                    ), borderThickness, borderHeight
//                ),
//                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight()),
//                keyPlace.place(
//                    1, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft().moveY(-thumbTopRightCornerTopOffset)
//                ),
//                keyPlace.place(
//                    1, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight().moveY(-thumbTopRightCornerTopOffset)
//                ),
//            )
//        )
        val firstPointMatrix = keyPlace.place(
            3, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft().move(0.0, -verticalOffset, borderZOffset)
        )

        models.add(
            hull(
                verticalCube(
                    thumbKeyPlace.placeR(
                        KeyPlaceholder.placeHolderTopRight().move(0.0, thumbTopRightCornerTopOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),

                keyPlace.place(
                    1, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight().moveY(-thumbTopRightCornerTopOffset)
                ),

                verticalCube(
                    firstPointMatrix, borderThickness, borderHeight
                ),
            )
        )

        //row 3 bottom
        models.add(
            hull(
                keyPlace.place(
                    1, cfg.lastRow, KeyPlaceholder.placeHolderBottomRight().moveY(-thumbTopRightCornerTopOffset)
                ),
                verticalCube(
                    firstPointMatrix, borderThickness, borderHeight
                ),

                verticalCube(
                    keyPlace.place(
                        2, cfg.lastRow, KeyPlaceholder.placeHolderBottomLeft().move(0.0, -verticalOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),
                verticalCube(
                    keyPlace.place(
                        2,
                        cfg.lastRow,
                        KeyPlaceholder.placeHolderBottomRight().move(0.0, -verticalOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),

                )
        )

        models.add(
            hull(
                thumbKeyPlace.placeR(KeyPlaceholder.placeHolderTop()),
                verticalCube(
                    thumbKeyPlace.placeR(
                        KeyPlaceholder.placeHolderTopRight().move(0.0, thumbTopRightCornerTopOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),
            )
        )

        models.add(
            hull(
                verticalCube(
                    thumbKeyPlace.placeR(
                        KeyPlaceholder.placeHolderTopRight().move(rightOffset, 0.0, borderZOffset)
                    ), borderThickness, borderHeight
                ),
                verticalCube(
                    thumbKeyPlace.placeR(
                        KeyPlaceholder.placeHolderTopRight().move(0.0, thumbTopRightCornerTopOffset, borderZOffset)
                    ), borderThickness, borderHeight
                ),
                verticalCube(
                    firstPointMatrix, borderThickness, borderHeight
                ),
            )
        )

        models.add(
            hull(
                verticalCube(
                    thumbKeyPlace.placeR(
                        KeyPlaceholder.placeHolderTopRight().move(rightOffset, 0.0, borderZOffset)
                    ), borderThickness, borderHeight
                ),
                verticalCube(
                    thumbKeyPlace.placeR(
                        KeyPlaceholder.placeHolderBottomRight().move(rightOffset, 0.0, borderZOffset)
                    ), borderThickness, borderHeight
                ),
                verticalCube(
                    firstPointMatrix, borderThickness, borderHeight
                ),
            )
        )
    }

    private fun verticalCube(obj: Abstract3dModel, borderThickness: Double, borderHeight: Double): Abstract3dModel {
        return borderObject(borderThickness, borderHeight).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }
}
