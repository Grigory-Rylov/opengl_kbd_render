package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.thumb.ThumbPoints
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.plugin.cfg.ThumbClusterMode
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Abstract3dModel
import com.github.grishberg.javascad.utils.Color

class SingleRow3ButtonsFrontRightToMatrixWallBuilder(
    private val cfg: KeyboardConfig,
    private val bottomEdgePatcher: WallBottomEdgePatcher,
    private val topEdgeOffsetZ: Double,
): FrontRightToMatrixWallBuilder {

    private val wallsSettings = cfg.wallsSettings
    private val isSkeletonMode = cfg.isSkeletonMode

    override fun create(
        ThumbR: (Abstract3dModel) -> Abstract3dModel,
        matrixOuterPlace: (Abstract3dModel) -> Abstract3dModel,
        matrixInnerPlace: (Abstract3dModel) -> Abstract3dModel
    ): List<Abstract3dModel> {
        val thumbFrontRight = ThumbR(
            KeyPlaceholder.placeHolderFrontRight().move(2.0, 0.0, wallsSettings.borderZOffset)
        )
        val thumbBackRight = ThumbR(
            KeyPlaceholder.placeHolderBackRight().move(2.0, -3.0, wallsSettings.borderZOffset)
        )
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

        val outerLeft = matrixOuterPlace(
            KeyPlaceholder.placeHolderFrontLeft()
                .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.outerBorderZOffset)
        )

        val thumbRPoint = verticalCube(
            ThumbR(
                KeyPlaceholder.placeHolderFrontRight()
                    .move(0.0, -wallsSettings.outerVerticalOffset, wallsSettings.borderZOffset)
            ),
        )

        val wall1 = hull(
            verticalCube(thumbFrontRight),
            verticalCube(thumbBackRight),
            verticalCube(innerLeft),
            verticalCube(outerLeft),
        ).withColor(Color.PINK)

        val bottomBorder = hull(
            verticalCube(innerLeft),
            verticalCube(innerRight),
            verticalCube(outerLeft),
        ).withColor(Color.BROWN)

        val models = listOf(
            verticalCube(thumbFrontRight),
            thumbRPoint.takeIf { cfg.thumbClusterSettings.type == ThumbClusterMode.TwoRows5Buttons },
            //verticalCube(thumbVertexFront),
            topBorderObj(frontOuter),

            bottomEdgePatcher.projection(frontOuter),
            bottomEdgePatcher.projection(outerLeft),
            verticalCube(outerLeft),
        )
        val topBorder = hull(
            models.filterNotNull()
        ).withColor(Color.AQUA_MARINE)

        if (isSkeletonMode) {
            return listOf(
                topBorder, hull(
                    bottomEdgePatcher.projection(frontOuter),
                    bottomEdgePatcher.projection(outerLeft),
                )
            )
        }

        return listOf(
            wall1,
            bottomBorder,
            topBorder,
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
