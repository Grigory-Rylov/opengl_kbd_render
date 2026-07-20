package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.tranzitions.Union

class OuterCornersWallBuilder(
    private val cfg: KeyboardConfig,
    private val isSkeletonMode: Boolean,
    private val topEdgeOffsetZ: Double,
    private val wallsSettings: WallsSettings,
    private val bottomEdgePatcher: WallBottomEdgePatcher,
    private val isThumb: Boolean = false,
) : CornerWallBuilder {

    override fun backLeft(
        keyPlace: (Model) -> Model
    ): Model {
        val back = keyPlace(
            KeyPlaceholder.placeHolderBackLeft()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val left = keyPlace(
            KeyPlaceholder.placeHolderBackLeft()
                .move(-wallsSettings.outerLeftOffset, 0.0, wallsSettings.outerBorderZOffset)
        )
        val border = hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackLeft()
                        .move(0.0, wallsSettings.verticalOffset, wallsSettings.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackLeft()
                        .move(-wallsSettings.leftOffset, 0.0, wallsSettings.borderZOffset)
                )
            ),
            topBorderObj(back),
            topBorderObj(left),
        )

        if (isSkeletonMode) {
            return Union(
                border, hull(
                    left,
                    left.moveY(-2.0),
                    bottomEdgePatcher.leftPoint(left),
                    bottomEdgePatcher.leftPoint(left.moveY(-2.0)),
                ), hull(
                    back,
                    bottomEdgePatcher.backPoint(back),
                ), hull(
                    bottomEdgePatcher.leftPoint(left),
                    bottomEdgePatcher.backPoint(back),
                )
            )
        }

        val wall = hull(
            topBorderObj(left), topBorderObj(back),
            bottomEdgePatcher.leftPoint(left),
            bottomEdgePatcher.backPoint(back),
        )
        return Union(border, wall)
    }

    override fun backRight(keyPlace: (Model) -> Model): List<Model> {
        if (isThumb) {
            return listOf(backRightThumb(keyPlace))
        }
        val count: Int = 20
        val back = keyPlace(
            KeyPlaceholder.placeHolderBackRight()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val right = keyPlace(
            KeyPlaceholder.placeHolderBackRight()
                .move(wallsSettings.outerRightOffset, 0.0, wallsSettings.outerBorderZOffset)
        )

        val start = right.move
        val end = back.move

        var lastTop = bottomEdgePatcher.backPoint(right)
        var lastBottom = bottomEdgePatcher.rightPoint(right)

        val models = mutableListOf<Model>()

        // Генерируем промежуточные точки
        for (i in 0..count) {
            val t: Double = i.toDouble() / count
            val intermediatePoint: V3d = start.lerp(end, t)
            val topObject = topBorderObj(intermediatePoint)
            val bottomObject = bottomEdgePatcher.backPoint(topObject)
            models.add(hull(topObject, bottomObject, lastTop, lastBottom))
            lastTop = topObject
            lastBottom = bottomObject
        }

        val border = hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackRight()
                        .move(0.0, wallsSettings.verticalOffset, wallsSettings.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackRight()
                        .move(wallsSettings.rightOffset, 0.0, wallsSettings.borderZOffset)
                )
            ),
            topBorderObj(back),
            topBorderObj(right),
        )
        models.add(border)

        models.add(
            hull(
                topBorderObj(right),
                bottomCylinder(right.move),
                bottomEdgePatcher.backPoint(right),
            )
        )

        if (isSkeletonMode) {
            return listOf(
                border,
                hull(
                    right,
                    right.moveY(-2.0),
                    bottomEdgePatcher.rightPoint(right),
                    bottomEdgePatcher.rightPoint(right.moveY(-2.0)),
                ),
                hull(back, bottomEdgePatcher.backPoint(back)),
                hull(bottomEdgePatcher.backPoint(back), bottomEdgePatcher.rightPoint(right))
            )
        }
        return models
    }

    private fun backRightThumb(keyPlace: (Model) -> Model): Model {
        val back = keyPlace(
            KeyPlaceholder.placeHolderBackRight()
                .move(0.0, wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val right = keyPlace(
            KeyPlaceholder.placeHolderBackRight()
                .move(wallsSettings.outerRightOffset, 0.0, wallsSettings.outerBorderZOffset)
        )
        val right2 = keyPlace(
            KeyPlaceholder.placeHolderBackRight()
                .move(wallsSettings.outerRightOffset, -2.0, wallsSettings.outerBorderZOffset)
        )
        val border = hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackRight()
                        .move(0.0, wallsSettings.verticalOffset, wallsSettings.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackRight()
                        .move(wallsSettings.rightOffset, 0.0, wallsSettings.borderZOffset)
                )
            ),
            topBorderObj(back),
            topBorderObj(right),
        )
        if (isSkeletonMode) {
            return Union(
                border,
                hull(
                    right,
                    right.moveY(-2.0),
                    bottomEdgePatcher.rightPoint(right),
                    bottomEdgePatcher.rightPoint(right.moveY(-2.0)),
                ),
                hull(back, bottomEdgePatcher.backPoint(back)),
                hull(bottomEdgePatcher.backPoint(back), bottomEdgePatcher.rightPoint(right))
            )
        }
        val wall = hull(
            topBorderObj(back),
            topBorderObj(right),
            bottomEdgePatcher.backPoint(back),
            bottomEdgePatcher.rightPoint(right)
        )
        return Union(border, wall)
    }

    override fun frontLeft(keyPlace: (Model) -> Model): Model {
        val left = keyPlace(
            KeyPlaceholder.placeHolderFrontLeft()
                .move(-wallsSettings.outerLeftOffset, 0.0, wallsSettings.outerBorderZOffset)
        )
        val front = keyPlace(
            KeyPlaceholder.placeHolderFrontLeft()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val border = hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontLeft()
                        .move(0.0, -wallsSettings.verticalOffset, wallsSettings.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontLeft()
                        .move(-wallsSettings.leftOffset, 0.0, wallsSettings.borderZOffset)
                )
            ),
            topBorderObj(front),
            topBorderObj(left),
        )

        if (isSkeletonMode) {
            return Union(
                border,
                hull(left, bottomEdgePatcher.leftPoint(left)),
                hull(front, bottomEdgePatcher.leftPoint(front)),
                hull(
                    bottomEdgePatcher.leftPoint(left),
                    bottomEdgePatcher.frontPoint(front),
                )
            )
        }

        val wall = hull(
            topBorderObj(left), topBorderObj(front),
            bottomEdgePatcher.leftPoint(left),
            bottomEdgePatcher.frontPoint(front),
        )
        return Union(border, wall)
    }

    override fun frontRight(keyPlace: (Model) -> Model): Model {
        val front = keyPlace(
            KeyPlaceholder.placeHolderFrontRight()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )
        val right = keyPlace(
            KeyPlaceholder.placeHolderFrontRight()
                .move(wallsSettings.outerRightOffset, 0.0, wallsSettings.outerBorderZOffset)
        )
        val border = hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontRight()
                        .move(0.0, -wallsSettings.verticalOffset, wallsSettings.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontRight()
                        .move(wallsSettings.rightOffset, 0.0, wallsSettings.borderZOffset)
                )
            ),
            topBorderObj(front),
            topBorderObj(right),
        )

        if (isSkeletonMode) {
            return Union(
                border,
                hull(front, bottomEdgePatcher.frontPoint(front)),
                hull(right, bottomEdgePatcher.rightPoint(right)),
                hull(
                    bottomEdgePatcher.frontPoint(front),
                    bottomEdgePatcher.rightPoint(right),
                )
            )
        }

        val wall = hull(
            topBorderObj(front), topBorderObj(right),
            bottomEdgePatcher.frontPoint(front),
            bottomEdgePatcher.rightPoint(right),
        )
        return Union(border, wall)
    }

    private fun verticalCube(obj: Model): Model {
        return borderObject(wallsSettings.borderThickness, wallsSettings.borderHeight).moveZ(topEdgeOffsetZ)
            .move(obj.move)
    }

    private fun bottomCylinder(point: V3d): Model {
        return Utils.cylinder(
            wallsSettings.borderThickness, wallsSettings.bottomBorderHeight
        ).move(point.projectionZ(wallsSettings.bottomBorderHeight / 2))
    }

    private fun borderObject(thickness: Double, height: Double): Model {
        return Utils.cylinder(thickness, height)
    }

    private fun topBorderObj(obj: Model): Model {
        return Utils.sphere(wallsSettings.borderThickness / 2.0).move(obj.move)
    }

    private fun topBorderObj(point: V3d): Model {
        return Utils.sphere(wallsSettings.borderThickness / 2.0).move(point)
    }
}
