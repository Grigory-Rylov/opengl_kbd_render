package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.Utils
import com.github.grishberg.cad3d.keyboard.Utils.hull
import com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.casebody.WallBottomEdgePatcher
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.tranzitions.Union

class OuterBackRightWallsBuilder(
    private val topEdgeOffsetZ: Double,
    private val cfg: WallsSettings,

    private val bottomEdgePatcher: WallBottomEdgePatcher = DefaultBottomEdgePatcher(
        cfg.borderThickness, cfg.bottomBorderHeight
    ),
) {

    fun backWall(
        keyPlace: (Abstract3dModel) -> Abstract3dModel,
    ): Abstract3dModel {
        val leftTop =
            keyPlace(KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))
        val rightTop =
            keyPlace(KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))

        val border = hull(
            topBorderObj(leftTop), topBorderObj(rightTop),

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

        val wall = hull(
            topBorderObj(leftTop), topBorderObj(rightTop),
            bottomEdgePatcher.backPoint(leftTop), bottomEdgePatcher.backPoint(rightTop)
        )
        return Union(
            border,
            wall)
    }

    fun backMidWall(
        count: Int = 20, keyPlace: (Abstract3dModel) -> Abstract3dModel, leftPlace: (Abstract3dModel) -> Abstract3dModel
    ): Abstract3dModel {
        val leftTop =
            keyPlace(KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))
        val rightTop =
            keyPlace(KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))

        val rightPrevTop =
            leftPlace(KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset))

        val start = rightPrevTop.move
        val end = rightTop.move

        var lastTop = topBorderObj(rightPrevTop)
        var lastBottom = bottomEdgePatcher.backPoint(rightPrevTop)

        val models = mutableListOf<Abstract3dModel>()

        // Генерируем промежуточные точки
        for (i in 0..count) {
            val t: Double = i.toDouble() / count
            val intermediatePoint: V3d = start.lerp(end, t)
            val topObject = topBorderObj().move(intermediatePoint)
            val bottomObject = bottomEdgePatcher.backPoint(topObject)
            models.add(hull(topObject, bottomObject, lastTop, lastBottom))
            lastTop = topObject
            lastBottom = bottomObject
        }

        // last hull
        models.add(
            hull(
                topBorderObj(rightTop),
                bottomEdgePatcher.backPoint(rightTop),
                topBorderObj(lastTop),
                lastBottom
            )
        )

        val border = hull(
            topBorderObj(rightPrevTop),
            topBorderObj(leftTop),
            topBorderObj(rightTop),
        )

        models.add(border)

        return Union(models)
    }

    private fun verticalCube(obj: Abstract3dModel): Abstract3dModel {
        return borderObject(cfg.borderThickness, cfg.borderHeight).moveZ(topEdgeOffsetZ).move(obj.move)
    }

    private fun verticalCube(point: V3d): Abstract3dModel {
        return borderObject(cfg.borderThickness, cfg.borderHeight).moveZ(topEdgeOffsetZ).move(point)
    }

    private fun borderObject(thickness: Double, height: Double): Abstract3dModel {
        return Utils.cylinder(thickness, height)
    }

    private fun topBorderObj(obj: Abstract3dModel): Abstract3dModel {
        return Utils.sphere(cfg.borderThickness / 2.0).move(obj.move)
    }

    private fun topBorderObj(): Abstract3dModel {
        return Utils.sphere(cfg.borderThickness / 2.0)
    }
}
