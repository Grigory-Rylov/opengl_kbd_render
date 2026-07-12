package com.github.grishberg.openscad.vrl

import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.utils.Color

open class Facet(
    val v0: V3d, val v1: V3d, val v2: V3d,
    @get:JvmName("getNormal") val normal: V3d,
    @get:JvmName("getColor") val color: Color = Color.GRAY
) {
    constructor(tri: Triangle3d, n: V3d, c: Color) : this(tri.p0, tri.p1, tri.p2, n, c)
    fun getTriangle(): Triangle3d = Triangle3d(v0, v1, v2)
}

open class Triangle3d {
    lateinit var p0: V3d
    lateinit var p1: V3d
    lateinit var p2: V3d

    constructor(a0: V3d, a1: V3d, a2: V3d) {
        p0 = a0; p1 = a1; p2 = a2
    }

    constructor(a0: V3d, a1: V3d, a2: V3d, normal: V3d) {
        val e1 = a1 - a0
        val e2 = a2 - a0
        val computed = e1.cross(e2).normalized()
        if (computed.dot(normal) < 0) {
            p0 = a0; p1 = a2; p2 = a1
        } else {
            p0 = a0; p1 = a1; p2 = a2
        }
    }

    fun getPoints(): List<V3d> = listOf(p0, p1, p2)
}

open class Polygon @JvmOverloads constructor(
    @get:JvmName("getVertices") val vertices: List<V3d>,
    @get:JvmName("getNormal") val normal: V3d,
    @get:JvmName("getColor") val color: Color = Color.GRAY,
    private val dist: Double = 0.0, private val valid: Boolean = true
) {
    fun getDist(): Double = dist
    fun isValid(): Boolean = valid
    fun toJson(): String {
        val sb = StringBuilder()
        sb.append("{\"vertices\":[")
        for (i in vertices.indices) {
            if (i > 0) sb.append(",")
            sb.append("{\"x\":").append(vertices[i].x).append(",\"y\":")
                .append(vertices[i].y).append(",\"z\":").append(vertices[i].z).append("}")
        }
        sb.append("]}")
        return sb.toString()
    }

    companion object {
        @JvmStatic
        fun fromPolygons(vertices: List<V3d>, color: Color): Polygon {
            val n = if (vertices.size >= 3) {
                val v0 = vertices[0]
                val v1 = vertices[1]
                val v2 = vertices[2]
                val a = v1 - v0
                val b = v2 - v0
                val cr = a.cross(b)
                val mag = cr.magnitude()
                if (mag > 1e-12) cr / mag else V3d(0.0, 0.0, 1.0)
            } else V3d(0.0, 0.0, 1.0)
            return Polygon(vertices, n, color)
        }
        @JvmStatic
        fun fromPolygons(vertices: List<V3d>, normal: V3d, color: Color): Polygon = Polygon(vertices, normal, color)
        @JvmStatic
        fun fromPolygons(vertices: List<V3d>, normal: V3d, color: Color, dist: Double): Polygon = Polygon(vertices, normal, color, dist)
        @JvmStatic
        fun isValid(vertices: List<V3d>, normal: V3d, dist: Double): Boolean {
            return vertices.size >= 3
        }
    }
}

open class CSG {
    @get:JvmName("getPolygons") val polygons: MutableList<Polygon> = mutableListOf()
    fun toFacets(): List<Facet> {
        val facets = mutableListOf<Facet>()
        for (p in polygons) {
            val verts = p.vertices
            if (verts.size == 3) {
                facets.add(Facet(Triangle3d(verts[0], verts[1], verts[2]), p.normal, p.color))
            } else {
                for (i in 1 until verts.size - 1) {
                    facets.add(Facet(Triangle3d(verts[0], verts[i], verts[i + 1]), p.normal, p.color))
                }
            }
        }
        return facets
    }
}

open class FacetGenerationContext {
    companion object { @JvmField val DEFAULT = FacetGenerationContext() }
    private var fn: Int = 10
    private var tag: Int = 0
    fun setFn(fn: Int): FacetGenerationContext { this.fn = fn; return this }
    fun getFn(): Int = fn
    open fun applyTag(tag: Int): FacetGenerationContext { this.tag = tag; return this }
    fun getTag(): Int = tag
}

class ColorFacetGenerationContext(@JvmField val defaultColor: Color = Color.GRAY) : FacetGenerationContext()

data class VertexPosition(val x: Double, val y: Double, val z: Double)
