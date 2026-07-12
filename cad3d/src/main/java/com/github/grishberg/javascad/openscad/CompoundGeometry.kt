package com.github.grishberg.javascad.openscad

import eu.printingin3d.javascad.coords.V3d

class CompoundGeometry(
    val children: List<Geometry>
) : Geometry {

    override val dimension: Int get() = children.firstOrNull()?.dimension ?: 3

    override fun isEmpty(): Boolean = children.all { it.isEmpty() }

    override fun boundingBox(): Pair<V3d, V3d>? {
        var result: Pair<V3d, V3d>? = null
        for (child in children) {
            val bb = child.boundingBox() ?: continue
            result = if (result == null) {
                bb
            } else {
                Pair(
                    V3d(
                        minOf(result.first.x, bb.first.x),
                        minOf(result.first.y, bb.first.y),
                        minOf(result.first.z, bb.first.z)
                    ),
                    V3d(
                        maxOf(result.second.x, bb.second.x),
                        maxOf(result.second.y, bb.second.y),
                        maxOf(result.second.z, bb.second.z)
                    )
                )
            }
        }
        return result
    }

    override fun transform(matrix: GeometryTransform): CompoundGeometry {
        return CompoundGeometry(children.map { it.transform(matrix) })
    }

    override fun copy(): CompoundGeometry {
        return CompoundGeometry(children.map { it.copy() })
    }
}
