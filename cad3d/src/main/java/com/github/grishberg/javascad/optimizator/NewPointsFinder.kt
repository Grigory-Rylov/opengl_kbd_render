package com.github.grishberg.javascad.optimizator

import com.github.grishberg.javascad.vrl.Polygon

interface NewPointsFinder {

    suspend fun findNewPoints(edges: Map<LineKey, List<PolygonEdge>>): Map<Polygon, MutableSet<PointInsert>>
}
