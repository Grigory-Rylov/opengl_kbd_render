package com.github.grishberg.cad3d.keyboard.casebody

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.ThumbKeyPlace
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.casebody.matrix.InnerBordersBuilder
import com.github.grishberg.cad3d.keyboard.casebody.matrix.InnerCorners
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbBorders
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbWalls
import com.github.grishberg.cad3d.keyboard.casebody.wall.ControllerHolderWall
import com.github.grishberg.cad3d.keyboard.casebody.wall.ControllerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.OuterBackRightWallsBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.OuterCornersWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.wall.OuterWallsBuilder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.utils.Color

class Walls(
    private val cfg: KeyboardConfig,
    private val wallsSettings: WallsSettings,
    private val keyPlace: KeyPlace,
    private val thumbKeyPlace: ThumbKeyPlace,
    private val topEdgeOffsetZ: Double,
    private val wallVerticalOffset: Double = 5.0,
    private val wallHorizontalOffset: Double = 10.0,
    private val thumbBorders: ThumbBorders,
    private val thumbWalls: ThumbWalls,
) {

    private val models = ArrayList<Model>()
    val thumbRightOffset = 4.0
    val thumbOuterRightOffset = 7.0

    fun createBorders(borderThickness: Double = 1.5, borderHeight: Double): List<Model> {
        models.clear()

        val borderOffset = 2.0
        //columns
        models.addAll(
            thumbBorders.create(
                InnerBordersBuilder(
                    thumbKeyPlace = thumbKeyPlace,
                    rightOffset = borderOffset,
                    verticalOffset = borderOffset,
                    borderThickness = borderThickness,
                    borderHeight = borderHeight
                ),
                InnerCorners(
                    rightOffset = borderOffset,
                    verticalOffset = borderOffset,
                    borderThickness = borderThickness,
                    borderHeight = borderHeight
                ),
                isWallMode = false,
            )
        )

        matrixBorders(
            InnerBordersBuilder(
                thumbKeyPlace = thumbKeyPlace, borderThickness = borderThickness, borderHeight = borderHeight
            ), InnerCorners(borderThickness = borderThickness, borderHeight = borderHeight)
        )

        betweenThumbAndMatrixBorders(borderThickness, borderHeight)

        return models
    }

    fun createWalls(
        bottomBorderHeight: Double,
    ): List<Model> {
        models.clear()

        val bottomEdgePatcher = CircleBottomEdgePatcher(
            thickness = 1.5,
            objectHeight = bottomBorderHeight,
            radiusX = 115.0,
            radiusY = 82.2,
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

        models.addAll(controllerWallBuilder.createWall())

        val wallsBuilder = OuterWallsBuilder(
            isSkeletonMode = cfg.isSkeletonMode,
            topEdgeOffsetZ = topEdgeOffsetZ,
            cfg = wallsSettings,
            bottomEdgePatcher = bottomEdgePatcher
        )
        matrixBorders(
            wallsBuilder, OuterCornersWallBuilder(
                cfg = cfg,
                isSkeletonMode = cfg.isSkeletonMode,
                topEdgeOffsetZ = topEdgeOffsetZ,
                wallsSettings = wallsSettings,
                bottomEdgePatcher = bottomEdgePatcher
            ), isWallMode = true
        )

        models.add(
            wallsBuilder.backMidWall(
                onlyBorder = true,
                leftOffset = -2.0,
                rightOffset = -1.0,
                leftPlace = { obj -> keyPlace.place(1, 0, obj) },
                rightPlace = { obj -> keyPlace.place(2, 0, obj) },
            ).withColor(Color.ALICE_BLUE)
        )

        backWallsFrom2To6(
            OuterBackRightWallsBuilder(
                topEdgeOffsetZ = topEdgeOffsetZ,
                cfg = wallsSettings,
                bottomEdgePatcher = bottomEdgePatcher,
            )
        )

        models.addAll(
            thumbWalls.createThumbWalls(
                OuterWallsBuilder(
                    isSkeletonMode = cfg.isSkeletonMode,
                    topEdgeOffsetZ = topEdgeOffsetZ,
                    cfg = wallsSettings,
                ),
                OuterCornersWallBuilder(
                    cfg = cfg,
                    isSkeletonMode = cfg.isSkeletonMode,
                    topEdgeOffsetZ = topEdgeOffsetZ,
                    wallsSettings = wallsSettings,
                    isThumb = true,
                    bottomEdgePatcher = DefaultBottomEdgePatcher(
                        wallsSettings.borderThickness, wallsSettings.bottomBorderHeight
                    ),
                ),
                verticalOffset = wallVerticalOffset,
                leftOffset = wallHorizontalOffset,
                bottomEdgePatcher = bottomEdgePatcher,
                topEdgeOffsetZ = topEdgeOffsetZ,
            )
        )

        return models
    }

    private fun backWallsFrom2To6(wallsBuilder: OuterBackRightWallsBuilder) {
        models.add(
            wallsBuilder.backWall(
                keyPlace = { obj -> keyPlace.place(2, 0, obj) },
            )
        )
        for (column in 3 until cfg.keyPlaceConfig.columnsCount) {
            // back columns
            models.add(
                wallsBuilder.backMidWall(
                    keyPlace = { obj -> keyPlace.place(column, 0, obj) },
                    leftPlace = { obj -> keyPlace.place(column - 1, 0, obj) },
                )
            )
        }
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

        // right back
        models.addAll(cornerWallBuilder.backRight { obj ->
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

        for (column in 0 until cfg.keyPlaceConfig.columnsCount) {
            // back columns
            models.add(wallsBuilder.backWall(onlyBorder = true) { obj ->
                keyPlace.place(
                    column, 0, obj
                )
            })
            if (column < 3) {
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
        for (column in 0 until cfg.keyPlaceConfig.columnsCount - 1) {
            // back diagonals
            if (!isWallMode || column != 1) {
                models.add(
                    wallsBuilder.backMidWall(
                        onlyBorder = true,
                        leftPlace = { obj -> keyPlace.place(column, 0, obj) },
                        rightPlace = { obj -> keyPlace.place(column + 1, 0, obj) },
                    )
                )
            }
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

        for (row in 0 until cfg.keyPlaceConfig.rowsCount) {
            //left
            models.addAll(wallsBuilder.leftWall { obj ->
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
        for (row in 0 until cfg.keyPlaceConfig.rowsCount - 1) {
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
                    leftOffset = -2.5,
                    rightOffset = -2.5,
                    leftPlace = { obj -> keyPlace.place(4, cfg.lastRow, obj) },
                    rightPlace = { obj -> keyPlace.place(5, cfg.lastRow, obj) },
                )
            )

            models.add(
                wallsBuilder.frontWall(
                    rightOffset = -4.0
                ) { obj -> keyPlace.place(4, cfg.lastRow, obj) })

            models.add(
                wallsBuilder.frontWall(
                    leftOffset = -2.0,
                ) { obj -> keyPlace.place(5, cfg.lastRow, obj) })

            if (cfg.isSkeletonMode) {
                val topOffset = 0.0
                val bottomOffset = 0.0

                //left
                models.addAll(wallsBuilder.leftWall(topOffset = topOffset, bottomOffset = bottomOffset) { obj ->
                    keyPlace.place(0, 0, obj)
                })

                models.addAll(wallsBuilder.leftWall(topOffset = topOffset, bottomOffset = bottomOffset) { obj ->
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

    private fun betweenThumbAndMatrixBorders(borderThickness: Double, borderHeight: Double) {
        val verticalOffset = 2.0
        val defaultVerticalOffset = 4.0
        val leftOffset = -8.0
        val rightOffset = 2.0
        val borderZOffset = -2.0

        val a = keyPlace.place(
            0, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft().move(leftOffset, 0.0, borderZOffset)
        ).move
        val b = keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft()).move
        val c = keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderFrontRight()).move

        val d = keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft()).move
        val e = keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderFrontRight()).move

        val f = keyPlace.place(2, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft()).move
        val g = keyPlace.place(2, cfg.lastRow, KeyPlaceholder.placeHolderFrontRight()).move

        val h = keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft().moveY(-verticalOffset)).move
        val i = keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderFrontRight().moveY(-verticalOffset)).move

        val j = keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft().moveY(-defaultVerticalOffset)).move
        val k =
            keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderFrontRight().moveY(-defaultVerticalOffset)).move

        val l = keyPlace.place(2, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft().moveY(-verticalOffset)).move
        val m = keyPlace.place(2, cfg.lastRow, KeyPlaceholder.placeHolderFrontRight().moveY(-verticalOffset)).move

        val n = keyPlace.place(3, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft()).move
        val o = keyPlace.place(3, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft().moveY(-defaultVerticalOffset)).move

        val p = thumbKeyPlace.placeM(
            KeyPlaceholder.placeHolderBackLeft().move(0.0, verticalOffset, borderZOffset)
        ).move

        val q = thumbKeyPlace.placeM(
            KeyPlaceholder.placeHolderBackRight().move(0.0, verticalOffset, borderZOffset)
        ).move

        val r = thumbKeyPlace.placeR(
            KeyPlaceholder.placeHolderBackRight().move(0.0, verticalOffset, borderZOffset)
        ).move

        val s = thumbKeyPlace.placeR(
            KeyPlaceholder.placeHolderBackRight().move(rightOffset, 0, borderZOffset)
        ).move

        val t = thumbKeyPlace.placeR(
            KeyPlaceholder.placeHolderBackLeft().move(0.0, verticalOffset, borderZOffset)
        ).move

        val u = thumbKeyPlace.placeL(
            KeyPlaceholder.placeHolderBackRight().move(0.0, verticalOffset, borderZOffset)
        ).move

        // col 0 front edge
        models.add(
            hull(
                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderFront()),
                border(h, borderThickness, borderHeight),
                border(i, borderThickness, borderHeight),
            )
        )

        // col 1 front edge
        models.add(
            hull(
                keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderFront()),
//                keyPlace.place(1, cfg.lastRow, KeyPlaceholder.placeHolderBack().moveY(-defaultVerticalOffset)),
                border(j, borderThickness, borderHeight),
                border(k, borderThickness, borderHeight),
            )
        )

        // col 2 front edge
        models.add(
            hull(
                keyPlace.place(2, cfg.lastRow, KeyPlaceholder.placeHolderFront()),
                keyPlace.place(2, cfg.lastRow, KeyPlaceholder.placeHolderFront().moveY(-verticalOffset)),
            )
        )

        addHull(a, p, h, borderThickness, borderHeight)
        //addHull(a, b, h, borderThickness, borderHeight)
        models.add(
            hull(
                border(a, borderThickness, borderHeight),
                keyPlace.place(0, cfg.lastRow, KeyPlaceholder.placeHolderFrontLeft()),
                border(h, borderThickness, borderHeight),
            )
        )

        addHull(h, i, p, borderThickness, borderHeight)
        addHull(p, i, j, borderThickness, borderHeight)
        addHull(k, q, t, borderThickness, borderHeight)
        addHull(k, t, r, borderThickness, borderHeight)
        addHull(m, l, k, borderThickness, borderHeight)
        addHull(o, k, r, borderThickness, borderHeight)
        addHull(k, o, m, borderThickness, borderHeight)
        addHull(o, r, s, borderThickness, borderHeight)
        addHull(a, p, u, borderThickness, borderHeight)
    }

    private fun addHull(
        p0: V3d,
        p1: V3d,
        p2: V3d,
        borderThickness: Double = 1.5,
        borderHeight: Double,
        color: Color = Color.GRAY
    ) {
        models.add(
            hull(
                border(p0, borderThickness, borderHeight),
                border(p1, borderThickness, borderHeight),
                border(p2, borderThickness, borderHeight),
            ).withColor(color)
        )
    }

    private fun border(p: V3d, borderThickness: Double, borderHeight: Double): Model {
        return borderObject(borderThickness, borderHeight).move(p)
    }

    private fun borderObject(thickness: Double, height: Double): Model {
        return Utils.cylinder(thickness, height)
    }
}
