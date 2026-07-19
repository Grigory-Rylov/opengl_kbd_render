package com.github.grishberg.javascad.manifold

import com.github.grishberg.javascad.coords.V3d

/**
 * Spatial hash grid for edge-triangle intersection queries and ray casting.
 * Triangles are inserted into all cells their bounding box covers.
 */
class SpatialGrid(tris: List<Tri>) {
    private val cells = mutableMapOf<Long, MutableList<Tri>>()
    val cellSize: Double
    private val globalMinX: Double
    private val globalMaxX: Double
    private val globalMinY: Double
    private val globalMaxY: Double
    private val globalMinZ: Double
    private val globalMaxZ: Double

    init {
        var minX = Double.POSITIVE_INFINITY
        var maxX = Double.NEGATIVE_INFINITY
        var minY = Double.POSITIVE_INFINITY
        var maxY = Double.NEGATIVE_INFINITY
        var minZ = Double.POSITIVE_INFINITY
        var maxZ = Double.NEGATIVE_INFINITY

        for (tri in tris) {
            val bb = tri.bbox()
            if (bb.minX < minX) minX = bb.minX
            if (bb.maxX > maxX) maxX = bb.maxX
            if (bb.minY < minY) minY = bb.minY
            if (bb.maxY > maxY) maxY = bb.maxY
            if (bb.minZ < minZ) minZ = bb.minZ
            if (bb.maxZ > maxZ) maxZ = bb.maxZ
        }

        globalMinX = minX; globalMaxX = maxX
        globalMinY = minY; globalMaxY = maxY
        globalMinZ = minZ; globalMaxZ = maxZ

        val diag = Math.sqrt(
            (maxX - minX) * (maxX - minX) +
            (maxY - minY) * (maxY - minY) +
            (maxZ - minZ) * (maxZ - minZ)
        )
        cellSize = if (diag > 0) diag / 100.0 else 1.0

        for (tri in tris) {
            val bb = tri.bbox()
            val cx0 = Math.floor(bb.minX / cellSize).toLong()
            val cx1 = Math.floor(bb.maxX / cellSize).toLong()
            val cy0 = Math.floor(bb.minY / cellSize).toLong()
            val cy1 = Math.floor(bb.maxY / cellSize).toLong()
            val cz0 = Math.floor(bb.minZ / cellSize).toLong()
            val cz1 = Math.floor(bb.maxZ / cellSize).toLong()

            for (cx in cx0..cx1) {
                for (cy in cy0..cy1) {
                    for (cz in cz0..cz1) {
                        cells.computeIfAbsent(hashKey(cx, cy, cz)) { mutableListOf() }.add(tri)
                    }
                }
            }
        }
    }

    fun querySegment(p1: V3d, p2: V3d): List<Tri> {
        val result = mutableSetOf<Tri>()
        val dist = Math.sqrt(
            (p2.x - p1.x) * (p2.x - p1.x) +
            (p2.y - p1.y) * (p2.y - p1.y) +
            (p2.z - p1.z) * (p2.z - p1.z)
        )
        val steps = maxOf(2, Math.ceil(dist / cellSize).toInt())

        for (i in 0..steps) {
            val t = i.toDouble() / steps
            val x = p1.x + t * (p2.x - p1.x)
            val y = p1.y + t * (p2.y - p1.y)
            val z = p1.z + t * (p2.z - p1.z)
            val key = hashKey(
                Math.floor(x / cellSize).toLong(),
                Math.floor(y / cellSize).toLong(),
                Math.floor(z / cellSize).toLong()
            )
            cells[key]?.let { result.addAll(it) }
        }
        return result.toList()
    }

    fun rayCastInsideAdaptive(pt: V3d, sourceTri: Tri): Boolean {
        val ex = sourceTri.v1.x - sourceTri.v0.x
        val ey = sourceTri.v1.y - sourceTri.v0.y
        val ez = sourceTri.v1.z - sourceTri.v0.z
        val fx = sourceTri.v2.x - sourceTri.v0.x
        val fy = sourceTri.v2.y - sourceTri.v0.y
        val fz = sourceTri.v2.z - sourceTri.v0.z
        val nx = ey * fz - ez * fy
        val ny = ez * fx - ex * fz
        val nz = ex * fy - ey * fx

        val absNx = Math.abs(nx)
        val absNy = Math.abs(ny)
        val absNz = Math.abs(nz)

        return if (absNx >= absNy && absNx >= absNz) {
            rayCastAxis(pt, sourceTri, 0)
        } else if (absNy >= absNz) {
            rayCastAxis(pt, sourceTri, 1)
        } else {
            rayCastAxis(pt, sourceTri, 2)
        }
    }

    private fun rayCastAxis(pt: V3d, sourceTri: Tri, axis: Int): Boolean {
        var crossings = 0
        val allTris = cells.values.flatten().toSet()

        for (tri in allTris) {
            if (tri === sourceTri) continue

            val e1x = tri.v1.x - tri.v0.x
            val e1y = tri.v1.y - tri.v0.y
            val e1z = tri.v1.z - tri.v0.z
            val e2x = tri.v2.x - tri.v0.x
            val e2y = tri.v2.y - tri.v0.y
            val e2z = tri.v2.z - tri.v0.z

            var proj: Double
            var triMin: Double
            var triMax: Double
            var ptVal: Double

            when (axis) {
                0 -> {
                    proj = e1y * e2z - e1z * e2y
                    triMin = minOf(tri.v0.x, tri.v1.x, tri.v2.x)
                    triMax = maxOf(tri.v0.x, tri.v1.x, tri.v2.x)
                    ptVal = pt.x
                }
                1 -> {
                    proj = e1z * e2x - e1x * e2z
                    triMin = minOf(tri.v0.y, tri.v1.y, tri.v2.y)
                    triMax = maxOf(tri.v0.y, tri.v1.y, tri.v2.y)
                    ptVal = pt.y
                }
                else -> {
                    proj = e1x * e2y - e1y * e2x
                    triMin = minOf(tri.v0.z, tri.v1.z, tri.v2.z)
                    triMax = maxOf(tri.v0.z, tri.v1.z, tri.v2.z)
                    ptVal = pt.z
                }
            }

            if (Math.abs(proj) < 1e-10) continue

            // Только треугольники ПЕРЕД точкой в направлении луча
            if (proj > 0 && triMin <= ptVal) continue
            if (proj < 0 && triMax >= ptVal) continue

            if (triRayCrossAxis(pt, tri, axis, proj > 0)) crossings++
        }
        return crossings % 2 == 1
    }

    private fun triRayCrossAxis(pt: V3d, tri: Tri, axis: Int, flip: Boolean): Boolean {
        val a0 = if (flip) tri.v1 else tri.v2
        val a1 = if (flip) tri.v2 else tri.v1
        val c = tri.v0

        var ac0 = 0.0
        var ac1 = 0.0
        var bc0 = 0.0
        var bc1 = 0.0
        var cc0 = 0.0
        var cc1 = 0.0

        when (axis) {
            0 -> {
                ac0 = pt.y - c.y
                ac1 = pt.z - c.z
                bc0 = a0.y - c.y
                bc1 = a0.z - c.z
                cc0 = a1.y - c.y
                cc1 = a1.z - c.z
            }
            1 -> {
                ac0 = pt.x - c.x
                ac1 = pt.z - c.z
                bc0 = a0.x - c.x
                bc1 = a0.z - c.z
                cc0 = a1.x - c.x
                cc1 = a1.z - c.z
            }
            else -> {
                ac0 = pt.x - c.x
                ac1 = pt.y - c.y
                bc0 = a0.x - c.x
                bc1 = a0.y - c.y
                cc0 = a1.x - c.x
                cc1 = a1.y - c.y
            }
        }

        val denom = bc0 * cc1 - bc1 * cc0
        if (Math.abs(denom) < 1e-10) return false

        val alpha = (bc1 * ac0 - bc0 * ac1) / denom
        val beta = (cc0 * ac1 - cc1 * ac0) / denom

        return alpha >= -EPS && beta >= -EPS && alpha + beta <= 1.0 + EPS
    }

    private fun hashKey(x: Long, y: Long, z: Long): Long {
        return x * 73856093L xor y * 19349663L xor z * 83492791L
    }
}
