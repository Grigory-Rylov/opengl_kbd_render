package com.github.grishberg.cad3d.keyboard.casebody.wall

import com.github.grishberg.cad3d.keyboard.KeyPlace
import com.github.grishberg.cad3d.keyboard.KeyPlaceholder
import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig
import com.github.grishberg.cad3d.keyboard.cfg.WallsSettings
import com.github.grishberg.cad3d.keyboard.casebody.DefaultBottomEdgePatcher
import com.github.grishberg.javascad.basic.Radius
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.models.Model
import com.github.grishberg.javascad.models.EdgeType
import com.github.grishberg.javascad.models.Sphere
import com.github.grishberg.javascad.models.surfaces.S12x3
import com.github.grishberg.javascad.models.surfaces.SmoothSurface
import com.github.grishberg.javascad.models.surfaces.VoronoiSurface
import com.github.grishberg.javascad.tranzitions.Union
import java.util.Random

class CurveWallBuilder(
    private val cfg: WallsSettings,
) {
    private val bottomEdgePatcher: DefaultBottomEdgePatcher = DefaultBottomEdgePatcher(
        cfg.borderThickness, cfg.bottomBorderHeight
    )

    fun build(
        keyboardConfig: KeyboardConfig,
        keyPlace: KeyPlace,
    ): Model {
        val topEdgePoints = mutableListOf<V3d>()
        val topBorderPoints = mutableListOf<V3d>()
        val bottomEdgePoints = mutableListOf<V3d>()

        for (column in 0 until keyboardConfig.keyPlaceConfig.columnsCount) {
            val topKeyPlace = { obj: Model ->
                keyPlace.place(
                    column, 0, obj
                )
            }

            val leftTopEdge = topKeyPlace(
                KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.verticalOffset, cfg.borderZOffset)
            )

            val rightTopEdge = topKeyPlace(
                KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.verticalOffset, cfg.borderZOffset)
            )

            topEdgePoints.add(leftTopEdge.move)
            topEdgePoints.add(rightTopEdge.move)

            val leftBorderEdge = topKeyPlace(
                KeyPlaceholder.placeHolderBackLeft().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
            )
            val rightBorderEdge = topKeyPlace(
                KeyPlaceholder.placeHolderBackRight().move(0.0, cfg.outerVerticalOffset, cfg.outerBorderZOffset)
            )

            topBorderPoints.add(leftBorderEdge.move)
            topBorderPoints.add(rightBorderEdge.move)

            val bottomLeft = bottomEdgePatcher.backPoint(leftTopEdge)
            val bottomRight = bottomEdgePatcher.backPoint(rightTopEdge)

            bottomEdgePoints.add(bottomLeft.move)
            bottomEdgePoints.add(bottomRight.move)
        }

        val controlPoints = arrayOf(
            bottomEdgePoints.toTypedArray(),
            topBorderPoints.toTypedArray(),
            topEdgePoints.toTypedArray(),
        )

        // Создание случайных сайтов
        val sites: MutableList<V3d> = ArrayList()
        val rand: Random = Random()
        for (i in 0..9) {
            sites.add(
                V3d(
                    rand.nextDouble() * 100 - 50, rand.nextDouble() * 20, rand.nextDouble() * 50
                )
            )
        }

        val vs = VoronoiSurface(
            controlPoints, sites, 2.5 // Ширина ребер
        )
        val contours = vs.calculateVoronoiEdges(4)

        val holes = mutableListOf<Model>()
        // Визуализация контуров
        for (contour in contours) {
            val points = mutableListOf<Model>()
            for (point in contour) {
                points.add(Sphere(Radius.fromDiameter(1.0)).move(point))
            }
            //holes.add(Hull(points))
            holes.addAll(points)
        }/*val surfaceBuilder: Model = SmoothSurface(
            BicubicSurfaceSpline.bSplineSurface(controlPoints, 10),
            borderHeight,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Normal
        )
 */

        val surface = SmoothSurface(
            S12x3.create(controlPoints).buildSurfaceStrategy(5),
            cfg.borderThickness * 2,
            EdgeType.Vertical,
            EdgeType.Normal,
            EdgeType.HorizontalY,
            EdgeType.Normal
        );
        return Union(holes)
    }
}
