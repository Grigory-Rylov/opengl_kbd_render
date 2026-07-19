package com.github.grishberg.javascad.optimizator

import com.github.grishberg.javascad.vrl.Polygon

/**
 * Find common edges between all polygons, groups polygon by edges.
 */
interface EdgesFinder {

    suspend fun groupPolygonsByEdges(polygons: List<Polygon>): Map<LineKey, List<PolygonEdge>>
}
