package com.github.grishberg.javascad.openscad

import eu.printingin3d.javascad.coords.V3d

interface Geometry {
    val dimension: Int
    fun isEmpty(): Boolean
    fun boundingBox(): Pair<V3d, V3d>?
    fun transform(matrix: GeometryTransform): Geometry
    fun copy(): Geometry
}
