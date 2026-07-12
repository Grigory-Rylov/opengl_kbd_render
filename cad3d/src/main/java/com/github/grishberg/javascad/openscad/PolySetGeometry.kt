package com.github.grishberg.javascad.openscad

import com.github.grishberg.javascad.Triangulator
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords.V3d
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
                val triangles = Triangulator.triangulate(vlist, normal)
                for (t in triangles) {
                    val pts = t.getPoints()
                    val r0 = pts[0].roundedToEpsilon()
                    val r1 = pts[1].roundedToEpsilon()
                    val r2 = pts[2].roundedToEpsilon()
                    val idx0 = getOrCreateVertex(vertMap, verts, r0)
                    val idx1 = getOrCreateVertex(vertMap, verts, r1)
                    val idx2 = getOrCreateVertex(vertMap, verts, r2)
                    tris.add(IndexedTriangle(idx0, idx1, idx2))
                }
            }

            return PolySetGeometry(verts, tris, polygons.firstOrNull()?.getColor() ?: Color.BLACK)
        }

        fun fromFacets(facets: List<Facet>): PolySetGeometry {
            val vertMap = mutableMapOf<V3d, Int>()
            val verts = mutableListOf<V3d>()
            val tris = mutableListOf<IndexedTriangle>()
            val color = facets.firstOrNull()?.getColor() ?: Color.BLACK

            for (facet in facets) {
                val pts = facet.getTriangle().getPoints()
                val idx0 = getOrCreateVertex(vertMap, verts, pts[0].roundedToEpsilon())
                val idx1 = getOrCreateVertex(vertMap, verts, pts[1].roundedToEpsilon())
                val idx2 = getOrCreateVertex(vertMap, verts, pts[2].roundedToEpsilon())
                tris.add(IndexedTriangle(idx0, idx1, idx2))
            }

            return PolySetGeometry(verts, tris, color)
        }

        private fun getOrCreateVertex(
            map: MutableMap<V3d, Int>,
            list: MutableList<V3d>,
            v: V3d
        ): Int {
            val existing = map[v]
            if (existing != null) return existing
            val idx = list.size
            list.add(v)
            map[v] = idx
            return idx
        }
    }
}
