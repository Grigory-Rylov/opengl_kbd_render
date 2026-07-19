package com.github.grishberg.javascad.manifold

import com.github.grishberg.javascad.coords.V3d

const val EPS = 1e-8
const val EPS2 = EPS * EPS

data class Tri(
    val v0: V3d, val v1: V3d, val v2: V3d,
    var inside: Boolean = false
) {
    fun centroid(): V3d = V3d(
        (v0.x + v1.x + v2.x) / 3.0,
        (v0.y + v1.y + v2.y) / 3.0,
        (v0.z + v1.z + v2.z) / 3.0
    )

    fun bbox(): TriBBox {
        val minX = minOf(v0.x, v1.x, v2.x)
        val minY = minOf(v0.y, v1.y, v2.y)
        val minZ = minOf(v0.z, v1.z, v2.z)
        val maxX = maxOf(v0.x, v1.x, v2.x)
        val maxY = maxOf(v0.y, v1.y, v2.y)
        val maxZ = maxOf(v0.z, v1.z, v2.z)
        return TriBBox(minX, minY, minZ, maxX, maxY, maxZ)
    }
}

data class TriBBox(
    val minX: Double, val minY: Double, val minZ: Double,
    val maxX: Double, val maxY: Double, val maxZ: Double
)
