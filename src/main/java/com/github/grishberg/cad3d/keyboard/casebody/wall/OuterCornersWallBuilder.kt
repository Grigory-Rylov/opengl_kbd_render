package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.Utils.union
import com.github.grishberg.cad3d.keyboard.casebody.CornerWallBuilder
import com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.Offset
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import eu.printingin3d.javascad.basic.Radius
import eu.printingin3d.javascad.models.Abstract3dModel
import eu.printingin3d.javascad.models.Sphere
import eu.printingin3d.javascad.tranzitions.Union

class OuterCornersWallBuilder(
    private val isSkeletonMode: Boolean,
    private val topEdgeOffsetZ: Double,
    private val cfg: WallsSettings,
    private val bottomEdgePatcher: WallBottomEdgePatcher = DefaultBottomEdgePatcher(
        cfg.borderThickness, cfg.bottomBorderHeight
    ),
) : CornerWallBuilder {

    override fun backLeft(
        keyPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val back = keyPlace(KeyPlaceholder.placeHolderTopLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))
        val left = keyPlace(KeyPlaceholder.placeHolderTopLeft().move(-cfg.outerLeftOffset, 0.0, cfg.outerBorderZOffset))
        val border = hull(
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopLeft().move(0.0, cfg.verticalOffset, cfg.borderZOffset))),
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopLeft().move(-cfg.leftOffset, 0.0, cfg.borderZOffset))),
            back,
            left,
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
            left, back,
            bottomEdgePatcher.leftPoint(left),
            bottomEdgePatcher.backPoint(back),
        )
        return Union(border, wall)
    }

    override fun backRight(offset: Offset?, keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        val back = keyPlace(KeyPlaceholder.placeHolderTopRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))
        val right = keyPlace(KeyPlaceholder.placeHolderTopRight().move(cfg.outerRightOffset, 0.0, cfg.outerBorderZOffset))
        val border = hull(
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopRight().move(0.0, cfg.verticalOffset, cfg.borderZOffset))),
            verticalCube(keyPlace(KeyPlaceholder.placeHolderTopRight().move(cfg.rightOffset, 0.0, cfg.borderZOffset))),
            back,
            right,
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
            back, right, bottomEdgePatcher.backPoint(back), bottomEdgePatcher.rightPoint(right)
        )
        return Union(border, wall)
    }

    override fun frontLeft(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        val left = keyPlace(
            KeyPlaceholder.placeHolderBottomLeft().move(-cfg.outerLeftOffset, 0.0, cfg.outerBorderZOffset)
        )
        val front = keyPlace(
            KeyPlaceholder.placeHolderBottomLeft().move(0.0, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val border = hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomLeft().move(0.0, -cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomLeft().move(-cfg.leftOffset, 0.0, cfg.borderZOffset)
                )
            ),
            front,
            left,
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
            left, front,
            bottomEdgePatcher.leftPoint(left),
            bottomEdgePatcher.frontPoint(front),
        )
        return Union(border, wall)
    }

    override fun frontRight(keyPlace: (Abstract3dModel) -> Abstract3dModel): Abstract3dModel {
        val front = keyPlace(
            KeyPlaceholder.placeHolderBottomRight().move(0.0, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )
        val right = keyPlace(
            KeyPlaceholder.placeHolderBottomRight().move(cfg.outerRightOffset, 0.0, cfg.outerBorderZOffset)
        )
        val border = hull(
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomRight().move(0.0, -cfg.verticalOffset, cfg.borderZOffset)
                )
            ),
            verticalCube(
                keyPlace(
                    KeyPlaceholder.placeHolderBottomRight().move(cfg.rightOffset, 0.0, cfg.borderZOffset)
                )
            ),
            front,
            right,
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
            front, right,
            bottomEdgePatcher.frontPoint(front),
            bottomEdgePatcher.rightPoint(right),
        )
        return Union(border, wall)
    }

    override fun frontRightToMatrix(
        keyPlace: (Abstract3dModel) -> Abstract3dModel,
        matrixOuterPlace: (Abstract3dModel) -> Abstract3dModel,
        matrixInnerPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel {
        val thumbVertex = keyPlace(
            KeyPlaceholder.placeHolderBottomRight().move(cfg.rightOffset, 0.0, cfg.outerBorderZOffset)
        )
        val thumbVertexFront = keyPlace(
            KeyPlaceholder.placeHolderBottomRight().move(0.0, -cfg.verticalOffset, cfg.borderZOffset)
        )
        val frontOuter = keyPlace(
            KeyPlaceholder.placeHolderBottomRight().move(0.0, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val innerLeft = matrixInnerPlace(
            KeyPlaceholder.placeHolderBottomLeft().move(0.0, -cfg.verticalOffset, cfg.borderZOffset)
        )

        val innerRight = matrixInnerPlace(
            KeyPlaceholder.placeHolderBottomRight().move(0.0, -cfg.verticalOffset, cfg.borderZOffset)
        )

        val outerLeft = matrixOuterPlace(
            KeyPlaceholder.placeHolderBottomLeft().move(0.0, -cfg.outerVerticalOffset, cfg.outerBorderZOffset)
        )

        val wall1 = hull(
            verticalCube(thumbVertex),
            verticalCube(innerLeft),
            verticalCube(outerLeft),
        )

        val bottomBorder = hull(
            verticalCube(innerLeft),
            verticalCube(innerRight),
            verticalCube(outerLeft),
        )

        val topBorder = hull(
            verticalCube(thumbVertex),
            verticalCube(thumbVertexFront),
            frontOuter,
            bottomEdgePatcher.projection(frontOuter),
            bottomEdgePatcher.projection(outerLeft),
            verticalCube(outerLeft),
        )


        if (isSkeletonMode) {
            return union(
                topBorder, hull(
                    bottomEdgePatcher.projection(frontOuter),
                    bottomEdgePatcher.projection(outerLeft),
                )
            )
        }

        return Union(
            wall1,
            bottomBorder,
            topBorder,
        )
    }

    private fun verticalCube(obj: Abstract3dModel): Abstract3dModel {
        return borderObject(cfg.borderThickness, cfg.borderHeight).moveZ(topEdgeOffsetZ).move(obj.move)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }
}
