package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.Utils.union
import com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.WallsBuilder
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.tranzitions.Union
import com.github.grishberg.javascad.utils.Color

class OuterWallsBuilder(
    private val isSkeletonMode: Boolean,
    private val topEdgeOffsetZ: Double,
    private val cfg: WallsSettings,

    private val bottomEdgePatcher: WallBottomEdgePatcher = DefaultBottomEdgePatcher(
        cfg.borderThickness, cfg.bottomBorderHeight
    ),
) : WallsBuilder {

    override fun backWall(
        onlyBorder: Boolean, keyPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val left =
            keyPlace(KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))
        val right =
            keyPlace(KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))

        val border = hull(
            topBorderObj(left), topBorderObj(right),

            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.verticalOffset, cfg.borderZOffset)
                )
            ),

            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.verticalOffset, cfg.borderZOffset)
                )
            )
        )
        if (onlyBorder) {
            return border
        }
        if (isSkeletonMode) {
            val right0 = keyPlace(
                KeyPlaceholder.placeHolderBackRight().move(-4.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
            )

            return Union(
                border,
                // connection
                hull(
                    right,
                    right0,
                    bottomEdgePatcher.backPoint(right),
                    bottomEdgePatcher.backPoint(right0),
                ),
                // bottom edge
                hull(
                    bottomEdgePatcher.backPoint(left),
                    bottomEdgePatcher.backPoint(right),
                )
            )
        }
        val wall = hull(
            left, right, bottomEdgePatcher.backPoint(left), bottomEdgePatcher.backPoint(right)
        )
        return Union(border, wall)
    }

    override fun backMidWall(
        onlyBorder: Boolean,
        leftOffset: Double,
        rightOffset: Double,
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val left = leftPlace.invoke(
            KeyPlaceholder.placeHolderBackRight().move(leftOffset, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val right = rightPlace.invoke(
            KeyPlaceholder.placeHolderBackLeft().move(rightOffset, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val border = hull(
            topBorderObj(left), topBorderObj(right), verticalCube(
                leftPlace.invoke(
                    KeyPlaceholder.placeHolderBackRight().move(leftOffset, cfg.verticalOffset, cfg.borderZOffset)
                )
            ), verticalCube(
                rightPlace.invoke(
                    KeyPlaceholder.placeHolderBackLeft().move(rightOffset, cfg.verticalOffset, cfg.borderZOffset)
                )
            )
        )

        if (onlyBorder) {
            return border
        }

        if (isSkeletonMode) {
            return union(
                border, hull(
                    bottomEdgePatcher.backPoint(left),
                    bottomEdgePatcher.backPoint(right),
                )
            )
        }
        val wall = hull(
            left, right,
            bottomEdgePatcher.backPoint(left),
            bottomEdgePatcher.backPoint(right),
        )
        return Union(border, wall)
    }

    override fun leftWall(
        topOffset: Double, bottomOffset: Double, keyPlace: (Abstract3dModel) -> Abstract3dModel
    ): List<Abstract3dModel> {
        val back = keyPlace(
            KeyPlaceholder.placeHolderBackLeft().move(-cfg.outerHorizontalOffset, topOffset, cfg.outerBorderZOffset)
        )
        val front = keyPlace(
            KeyPlaceholder.placeHolderFrontLeft().move(-cfg.outerHorizontalOffset, bottomOffset, cfg.outerBorderZOffset)
        )

        val border = hull(
            topBorderObj(back), topBorderObj(front),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBackLeft().move(-cfg.leftOffset, 0.0, cfg.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(-cfg.leftOffset, 0.0, cfg.borderZOffset)
                )
            ).withColor(Color.CORN_SILK),
        )

        if (isSkeletonMode) {
            return listOf(
                border, hull(
                    bottomEdgePatcher.leftPoint(back), bottomEdgePatcher.leftPoint(front)
                )
            )
        }

        val wall = hull(
            topBorderObj(back), topBorderObj(front),
            bottomEdgePatcher.leftPoint(back),
            bottomEdgePatcher.leftPoint(front),
        )
        return listOf(border, wall)
    }

    override fun leftMidWall(
        leftPlace: (Abstract3dModel) -> Abstract3dModel, rightPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val top = leftPlace(
            KeyPlaceholder.placeHolderFrontLeft().move(-cfg.outerHorizontalOffset, 0.0, cfg.outerBorderZOffset)
        )
        val bottom = rightPlace(
            KeyPlaceholder.placeHolderBackLeft().move(-cfg.outerHorizontalOffset, 0.0, cfg.outerBorderZOffset)
        )

        val border = hull(
            topBorderObj(top),
            topBorderObj(bottom),
            verticalCube(
                leftPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(-cfg.leftOffset, 0.0, cfg.borderZOffset)
                )
            ),
            verticalCube(
                rightPlace(
                    KeyPlaceholder.placeHolderBackLeft().move(-cfg.leftOffset, 0.0, cfg.borderZOffset)
                )
            ),
        )

        if (isSkeletonMode) {
            return union(
                border, hull(
                    bottomEdgePatcher.leftPoint(top), bottomEdgePatcher.leftPoint(bottom)
                )
            )
        }

        val wall = hull(
            topBorderObj(top),
            topBorderObj(bottom),
            bottomEdgePatcher.leftPoint(top),
            bottomEdgePatcher.leftPoint(bottom)
        )

        return Union(border, wall)
    }

    override fun frontWall(
        leftOffset: Double, rightOffset: Double,
        onlyBorder: Boolean,
        keyPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel {
        val left = keyPlace(
            KeyPlaceholder.placeHolderFrontLeft().move(leftOffset, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val right = keyPlace(
            KeyPlaceholder.placeHolderFrontRight().move(rightOffset, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val border = hull(
            topBorderObj(left), topBorderObj(right),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(leftOffset, -cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(rightOffset, -cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
        )
        if (onlyBorder) {
            return border
        }

        if (isSkeletonMode) {
            return union(
                border, hull(left, bottomEdgePatcher.frontPoint(left)), hull(
                    bottomEdgePatcher.frontPoint(left),
                    bottomEdgePatcher.frontPoint(right),
                )
            )
        }

        val wall = hull(
            topBorderObj(left),
            topBorderObj(right),
            bottomEdgePatcher.frontPoint(left),
            bottomEdgePatcher.frontPoint(right)
        )

        return Union(border, wall).withColor(Color.PINK)
    }

    override fun frontMidWall(
        leftOffset: Double,
        rightOffset: Double,
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val left = leftPlace(
            KeyPlaceholder.placeHolderFrontRight().move(leftOffset, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val right = rightPlace(
            KeyPlaceholder.placeHolderFrontLeft().move(rightOffset, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val border = hull(
            topBorderObj(left),
            topBorderObj(right),
            verticalCube(
                leftPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(leftOffset, -cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
            verticalCube(
                rightPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(rightOffset, -cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
        )

        if (isSkeletonMode) {
            return union(
                border, hull(
                    bottomEdgePatcher.frontPoint(left),
                    bottomEdgePatcher.frontPoint(right),
                )
            )
        }

        val wall = hull(
            topBorderObj(left), topBorderObj(right),
            bottomEdgePatcher.frontPoint(left),
            bottomEdgePatcher.frontPoint(right),
        )

        return Union(border, wall).withColor(Color.RED)
    }

    override fun rightWall(
        topOffset: Double, bottomOffset: Double, keyPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val top = keyPlace(
            KeyPlaceholder.placeHolderBackRight().move(cfg.outerHorizontalOffset, topOffset, cfg.outerBorderZOffset)
        )
        val bottom = keyPlace(
            KeyPlaceholder.placeHolderFrontRight().move(cfg.outerHorizontalOffset, bottomOffset, cfg.outerBorderZOffset)
        )

        val border = hull(
            topBorderObj(top), topBorderObj(bottom),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(cfg.rightOffset, 0.0, cfg.borderZOffset)
                )
            ),
            verticalCube(keyPlace(KeyPlaceholder.placeHolderBackRight().move(cfg.rightOffset, 0.0, cfg.borderZOffset))),
        )

        if (isSkeletonMode) {
            return union(
                border, hull(
                    bottomEdgePatcher.rightPoint(top),
                    bottomEdgePatcher.rightPoint(bottom),
                )
            )
        }

        val wall = hull(
            topBorderObj(top), topBorderObj(bottom),
            bottomEdgePatcher.rightPoint(top),
            bottomEdgePatcher.rightPoint(bottom),
        )

        return Union(border, wall)
    }

    override fun rightMidWall(
        backPlace: (Abstract3dModel) -> Abstract3dModel, frontPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val top = frontPlace(
            KeyPlaceholder.placeHolderBackRight().move(cfg.outerHorizontalOffset, 0.0, cfg.outerBorderZOffset)
        )
        val bottom = backPlace(
            KeyPlaceholder.placeHolderFrontRight().move(cfg.outerHorizontalOffset, 0.0, cfg.outerBorderZOffset)
        )

        val border = hull(
            topBorderObj(bottom), topBorderObj(top), verticalCube(
                backPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(cfg.rightOffset, 0.0, cfg.borderZOffset)
                )
            ), verticalCube(
                frontPlace(
                    KeyPlaceholder.placeHolderBackRight().move(cfg.rightOffset, 0.0, cfg.borderZOffset)
                )
            )
        )


        if (isSkeletonMode) {
            return union(
                border, hull(
                    bottomEdgePatcher.rightPoint(top),
                    bottomEdgePatcher.rightPoint(bottom),
                )
            )
        }

        val wall = hull(
            topBorderObj(top), topBorderObj(bottom),
            bottomEdgePatcher.rightPoint(top),
            bottomEdgePatcher.rightPoint(bottom),
        )
        return Union(border, wall)
    }

    override fun midEdge(
        midPlace: (Abstract3dModel) -> Abstract3dModel,
        leftPlace: (Abstract3dModel) -> Abstract3dModel,
        rightPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val left = leftPlace(
            KeyPlaceholder.placeHolderFrontRight().move(0.0, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val right = rightPlace(
            KeyPlaceholder.placeHolderFrontLeft().move(0.0, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val border = hull(
            topBorderObj(left), topBorderObj(right),
            verticalCube(
                leftPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(0.0, -cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
            verticalCube(
                rightPlace(
                    KeyPlaceholder.placeHolderFrontLeft().move(0.0, -cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
        )

        if (isSkeletonMode) {
            return union(
                border, hull(
                    bottomEdgePatcher.projection(left),
                    bottomEdgePatcher.projection(right),
                )
            )
        }

        val wall = hull(
            topBorderObj(left), topBorderObj(right),
            bottomEdgePatcher.projection(left),
            bottomEdgePatcher.projection(right),
        )
        return Union(border, wall)
    }

    override fun rightDiagonal(
        backKeyPlace: (Abstract3dModel) -> Abstract3dModel,
        frontKeyPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val topOffset = 0.0
        val bottomOffset = 0.0
        val offset = 1.0
        val back = backKeyPlace(
            KeyPlaceholder.placeHolderFrontRight().move(offset, 0.0, cfg.borderZOffset)
        )
        val front = frontKeyPlace(
            KeyPlaceholder.placeHolderFrontRight().move(offset, bottomOffset, cfg.borderZOffset)
        )

        val border = hull(
            topBorderObj(back), topBorderObj(front),
            /*
            verticalCube(
                frontKeyPlace(
                    KeyPlaceholder.placeHolderFrontRight().move(cfg.rightOffset, 0.0, cfg.borderZOffset)
                )
            ),
            verticalCube(backKeyPlace(KeyPlaceholder.placeHolderFrontRight().move(cfg.rightOffset, 0.0, cfg.borderZOffset))),

             */
        )

        if (isSkeletonMode) {
            return union(
                border, hull(
                    bottomEdgePatcher.rightPoint(back),
                    bottomEdgePatcher.rightPoint(front),
                )
            )
        }

        val wall = hull(
            topBorderObj(back), topBorderObj(front),
            bottomEdgePatcher.rightPoint(back),
            bottomEdgePatcher.rightPoint(front),
        )

        return Union(border, wall)
    }

    private fun verticalCube(obj: Abstract3dModel): Abstract3dModel {
        return borderObject(cfg.borderThickness, cfg.borderHeight).moveZ(topEdgeOffsetZ).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }

    private fun topBorderObj(obj: Abstract3dModel): Abstract3dModel {
        return Utils.sphere(cfg.borderThickness / 2.0).move(obj.move)
    }

    private fun topBorderObj(point: V3d): Abstract3dModel {
        return Utils.sphere(cfg.borderThickness / 2.0).move(point)
    }
}
