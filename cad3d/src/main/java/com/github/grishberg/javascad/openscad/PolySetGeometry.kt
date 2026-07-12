package com.github.grishberg.javascad.openscad

import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.vrl.Const
import eu.printingin3d.javascad.vrl.Facet
import eu.printingin3d.javascad.vrl.Polygon
import eu.printingin3d.javascad.utils.Color

data class IndexedTriangle(val v0: Int, val v1: Int, val v2: Int)

class PolySetGeometry(
    val vertices: MutableList<V3d>,
    val indices: MutableList<IndexedTriangle>,
    val color: Color = Color.BLACK
) : Geometry {

    override val dimension: Int get() = 3

    var isManifold: Boolean = false

    override fun isEmpty(): Boolean = indices.isEmpty()

    override fun boundingBox(): Pair<V3d, V3d>? {
        if (vertices.isEmpty()) return null
        var minX = Double.MAX_VALUE
        var minY = Double.MAX_VALUE
        var minZ = Double.MAX_VALUE
        var maxX = -Double.MAX_VALUE
        var maxY = -Double.MAX_VALUE
        var maxZ = -Double.MAX_VALUE
        for (v in vertices) {
            if (v.x < minX) minX = v.x
            if (v.y < minY) minY = v.y
            if (v.z < minZ) minZ = v.z
            if (v.x > maxX) maxX = v.x
            if (v.y > maxY) maxY = v.y
            if (v.z > maxZ) maxZ = v.z
        }
        return Pair(V3d(minX, minY, minZ), V3d(maxX, maxY, maxZ))
    }

    override fun transform(matrix: GeometryTransform): PolySetGeometry {
        val newVerts = vertices.map { matrix.transform(it) }.toMutableList()
        val hasMirror = matrix.isMirror()
        val newIndices = if (hasMirror) {
            indices.map { IndexedTriangle(it.v2, it.v1, it.v0) }.toMutableList()
        } else {
            indices.toMutableList()
        }
        return PolySetGeometry(newVerts, newIndices, color).also {
            it.isManifold = this.isManifold
        }
    }

    override fun copy(): PolySetGeometry {
        return PolySetGeometry(
            ArrayList(vertices),
            ArrayList(indices),
            color
        ).also {
            it.isManifold = this.isManifold
        }
    }

    fun toPolygons(): List<Polygon> {
        val result = mutableListOf<Polygon>()
        for (tri in indices) {
            val pts = listOf(vertices[tri.v0], vertices[tri.v1], vertices[tri.v2])
            val a = pts[0]
            val b = pts[1]
            val c = pts[2]
            val normal = b.subtract(a).cross(c.subtract(a)).unit()
            result.add(Polygon.fromPolygons(pts, normal, color))
        }
        return result
    }

    fun toFacets(): List<Facet> {
        val result = mutableListOf<Facet>()
        for (tri in indices) {
            val triangle = Triangle3d(vertices[tri.v0], vertices[tri.v1], vertices[tri.v2])
            val a = vertices[tri.v0]
            val b = vertices[tri.v1]
            val c = vertices[tri.v2]
            val normal = b.subtract(a).cross(c.subtract(a)).unit()
            result.add(Facet(triangle, normal, color))
        }
        return result
    }

    fun triangleCount(): Int = indices.size

    fun vertexCount(): Int = vertices.size

    companion object {
        fun empty(): PolySetGeometry = PolySetGeometry(mutableListOf(), mutableListOf())

        fun fromPolygons(polygons: List<Polygon>): PolySetGeometry {
            val vertMap = mutableMapOf<V3d, Int>()
            val verts = mutableListOf<V3d>()
            val tris = mutableListOf<IndexedTriangle>()

            for (poly in polygons) {
                val vlist = poly.getVertices()
                val normal = poly.getNormal()
                val triVerts = Triangulator3d.triangulatePolygon(vlist, normal)
                for ((v0, v1, v2) in triVerts) {
                    val idx0 = getOrCreateVertex(vertMap, verts, v0)
                    val idx1 = getOrCreateVertex(vertMap, verts, v1)
                    val idx2 = getOrCreateVertex(vertMap, verts, v2)
                    tris.add(IndexedTriangle(idx0, idx1, idx2))
                }
            }

            return PolySetGeometry(verts, tris, polygons.firstOrNull()?.getColor() ?: Color.BLACK)
        }

        private fun getOrCreateVertex(
            map: MutableMap<V3d, Int>,
            list: MutableList<V3d>,
            v: V3d
        ): Int {
            val rounded = v.roundedToEpsilon()
            val existing = map[rounded]
            if (existing != null) return existing
            val idx = list.size
            list.add(v)
            map[rounded] = idx
            return idx
        }
    }
}

internal object Triangulator3d {
    fun triangulatePolygon(vertices: List<V3d>, normal: V3d): List<Triple<V3d, V3d, V3d>> {
        if (vertices.size < 3) return emptyList()
        if (vertices.size == 3) {
            return listOf(Triple(vertices[0], vertices[1], vertices[2]))
        }
        return earClipTriangulate(vertices, normal)
    }

    private fun earClipTriangulate(vertices: List<V3d>, normal: V3d): List<Triple<V3d, V3d, V3d>> {
        val result = mutableListOf<Triple<V3d, V3d, V3d>>()
        val work = vertices.toMutableList()
        var attempts = 0
        val maxAttempts = work.size * work.size

        while (work.size >= 3 && attempts < maxAttempts) {
            val size = work.size
            var earFound = false
            for (i in 0 until size) {
                val prev = work[(i - 1 + size) % size]
                val curr = work[i]
                val next = work[(i + 1) % size]
                if (isEar(prev, curr, next, work, normal)) {
                    result.add(Triple(prev, curr, next))
                    work.removeAt(i)
                    earFound = true
                    break
                }
            }
            if (!earFound) {
                if (work.size >= 3) {
                    result.add(Triple(work[0], work[1], work[2]))
                }
                break
            }
            attempts++
        }
        return result
    }

    private fun isEar(prev: V3d, curr: V3d, next: V3d, all: List<V3d>, normal: V3d): Boolean {
        val a = curr.subtract(prev)
        val b = next.subtract(curr)
        val cross = a.cross(b)
        if (cross.magnitude() < Const.EPSILON) return false

        if (cross.dot(normal) <= 0) return false

        for (p in all) {
            if (p == prev || p == curr || p == next) continue
            if (isPointInTriangle(p, prev, curr, next)) return false
        }
        return true
    }

    private fun isPointInTriangle(p: V3d, a: V3d, b: V3d, c: V3d): Boolean {
        val v0 = c.subtract(a)
        val v1 = b.subtract(a)
        val v2 = p.subtract(a)
        val dot00 = v0.dot(v0)
        val dot01 = v0.dot(v1)
        val dot02 = v0.dot(v2)
        val dot11 = v1.dot(v1)
        val dot12 = v1.dot(v2)
        val invDenom = 1.0 / (dot00 * dot11 - dot01 * dot01)
        val u = (dot11 * dot02 - dot01 * dot12) * invDenom
        val v = (dot00 * dot12 - dot01 * dot02) * invDenom
        return u >= 0 && v >= 0 && (u + v) <= 1
    }
}
