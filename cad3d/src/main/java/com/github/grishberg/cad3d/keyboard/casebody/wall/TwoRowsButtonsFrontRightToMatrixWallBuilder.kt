package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbPoints
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.utils.Color

class TwoRowsButtonsFrontRightToMatrixWallBuilder(
    private val cfg: KeyboardConfig,
    private val bottomEdgePatcher: WallBottomEdgePatcher,
    private val topEdgeOffsetZ: Double,
    private val thumbPoints: ThumbPoints,
) : FrontRightToMatrixWallBuilder {

    private val wallsSettings = cfg.wallsSettings
    private val isSkeletonMode = cfg.isSkeletonMode

    override fun create(
        ThumbR: (Abstract3dModel) -> Abstract3dModel,
        matrixOuterPlace: (Abstract3dModel) -> Abstract3dModel,
        matrixInnerPlace: (Abstract3dModel) -> Abstract3dModel
    ): List<Abstract3dModel> {

        val thumbVertexFront = ThumbR(
            KeyPlaceholder.placeHolderFrontRight().move(0.0, -wallsSettings.verticalOffset, wallsSettings.borderZOffset)
        )
        val frontOuter = ThumbR(
            KeyPlaceholder.placeHolderFrontRight()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        val innerLeft = matrixInnerPlace(
            KeyPlaceholder.placeHolderFrontLeft().move(0.0, -wallsSettings.verticalOffset, wallsSettings.borderZOffset)
        )

        val innerRight = matrixInnerPlace(
            KeyPlaceholder.placeHolderFrontRight().move(0.0, -wallsSettings.verticalOffset, wallsSettings.borderZOffset)
        )

        val thumbRPoint = verticalCube(
            ThumbR(
                KeyPlaceholder.placeHolderFrontRight()
                    .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.borderZOffset)
            ),
        )

        val wall1 = hull(
            verticalCube(thumbPoints.row1RightFRInner),
            verticalCube(thumbPoints.row1RightBRInner),
            verticalCube(innerLeft),
            verticalCube(thumbPoints.col4LFOut),
        ).withColor(Color.PINK)

        val bottomBorder = hull(
            verticalCube(innerLeft),
            verticalCube(innerRight),
            verticalCube(thumbPoints.col4LFOut),
        ).withColor(Color.BROWN)

        val topBorder = hull(
            verticalCube(thumbPoints.row1RightFRInner),
            thumbRPoint,
            //verticalCube(thumbVertexFront),

            topBorderObj(frontOuter),

            bottomEdgePatcher.projection(frontOuter),
            bottomEdgePatcher.projection(thumbPoints.col4LFOut),
            verticalCube(thumbPoints.col4LFOut),
        ).withColor(Color.AQUA_MARINE)

        val diagonalWall = hull(
            thumbPoints.row2RightFRRightOuter,
            verticalCube(thumbPoints.row1RightFRInner),
            verticalCube(thumbPoints.col4LFOut),
        ).withColor(Color.BISQUE)

        val diagonalBottomWall = hull(
            thumbPoints.row2RightFRRightOuter,
            verticalCube(thumbPoints.col4LFOut),
            bottomEdgePatcher.projection(thumbPoints.col4LFOut),
            bottomEdgePatcher.projection(thumbPoints.row2RightFRRightOuter),
        ).withColor(Color.ORANGE)

        if (isSkeletonMode) {
            return listOf(
                topBorder, hull(
                    bottomEdgePatcher.projection(frontOuter),
                    bottomEdgePatcher.projection(thumbPoints.col4LFOut),
                )
            )
        }

        return listOf(
            wall1,
            bottomBorder,
            diagonalWall,
            diagonalBottomWall,
        )
    }

    private fun verticalCube(obj: Abstract3dModel): Abstract3dModel {
        return borderObject(wallsSettings.borderThickness, wallsSettings.borderHeight).moveZ(topEdgeOffsetZ)
            .move(obj.move)
    }

    private fun bottomCylinder(point: V3d): Abstract3dModel {
        return Utils.cylinder(
            wallsSettings.borderThickness, wallsSettings.bottomBorderHeight
        ).move(point.projectionZ(wallsSettings.bottomBorderHeight / 2))
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }

    private fun topBorderObj(obj: Abstract3dModel): Abstract3dModel {
        return Utils.sphere(wallsSettings.borderThickness / 2.0).move(obj.move)
    }

    private fun topBorderObj(point: V3d): Abstract3dModel {
        return Utils.sphere(wallsSettings.borderThickness / 2.0).move(point)
    }
}
