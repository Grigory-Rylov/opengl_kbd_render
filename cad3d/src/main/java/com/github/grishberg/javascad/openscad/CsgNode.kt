package com.github.grishberg.javascad.openscad

import eu.printingin3d.javascad.vrl.Polygon

sealed class CsgNode {

    object Empty : CsgNode()

    data class Leaf(val polygons: List<Polygon>) : CsgNode()

    data class Transform(
        val child: CsgNode,
        val matrix: GeometryTransform
    ) : CsgNode()

    data class Union(val children: List<CsgNode>) : CsgNode() {
        constructor(vararg children: CsgNode) : this(children.toList())
    }

    data class Difference(val left: CsgNode, val right: CsgNode) : CsgNode()

    data class Intersection(val left: CsgNode, val right: CsgNode) : CsgNode()

    companion object {
        fun fromPolygons(polygons: List<Polygon>): CsgNode {
            return if (polygons.isEmpty()) Empty else Leaf(polygons)
        }
    }
}
