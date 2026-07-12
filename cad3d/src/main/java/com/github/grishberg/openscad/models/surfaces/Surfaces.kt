package com.github.grishberg.openscad.models.surfaces

import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.models.EdgeType
import com.github.grishberg.openscad.models.Abstract3dModel
import com.github.grishberg.openscad.models.IModel
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.model.Model as CsgModel

class S12x3(private val controlPoints: List<List<V3d>>) {
    companion object {
        fun create(points: Array<Array<V3d>>): S12x3 = S12x3(points.toList().map { it.toList() })
        fun create(points: List<List<V3d>>): S12x3 = S12x3(points)
    }
    fun buildSurfaceStrategy(steps: Int): Any = this
}

class SmoothSurface(
    val strategy: Any, val borderHeight: Double,
    val edge1: EdgeType, val edge2: EdgeType,
    val edge3: EdgeType, val edge4: EdgeType
) : Abstract3dModel(CsgModel(PolySet3.EMPTY)) {
    override fun cloneModel(): IModel = SmoothSurface(strategy, borderHeight, edge1, edge2, edge3, edge4)
}

class VoronoiSurface(
    val controlPoints: Array<Array<V3d>>,
    val sites: List<V3d>,
    val edgeWidth: Double
) {
    fun calculateVoronoiEdges(tolerance: Int): List<List<V3d>> {
        return controlPoints.map { it.toList() }
    }
}

object BicubicSurfaceSpline {
    @JvmStatic
    fun bSplineSurface(controlPoints: Array<Array<V3d>>, resolution: Int): Any = Unit
}

class BicubicInterpolator(val points: Array<Array<V3d>>) {
    companion object {
        @JvmStatic
        fun interpolate(points: Array<Array<V3d>>): BicubicInterpolator = BicubicInterpolator(points)
    }

    fun generateSurface(steps: Int): Array<Array<V3d>> = points
}
