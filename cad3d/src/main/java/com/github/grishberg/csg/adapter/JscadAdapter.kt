package com.github.grishberg.csg.adapter

import com.github.grishberg.csg.bsp.meshFromPolygons
import com.github.grishberg.csg.bsp.Polygon as BspPolygon
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.vrl.Polygon as JscadPolygon
import eu.printingin3d.javascad.vrl.CSG as JscadCSG

/**
 * Адаптер: конвертирует результат JSCAD BSP в наш PolySet3.
 * JSCAD BSP создаёт N-gons с дублированными вершинами.
 * Наш движок делает из этого чистый треугольный меш с общими вершинами.
 */
object JscadAdapter {

    /**
     * Конвертирует JSCAD CSG полигоны в PolySet3 с общими вершинами.
     * N-gons триангулируются ear-clipping (через fan от первой вершины).
     */
    fun csgToPolySet3(jscadCsg: JscadCSG): PolySet3 {
        return polygonsToPolySet3(jscadCsg.getPolygons())
    }

    /**
     * Конвертирует JSCAD полигоны в PolySet3 с общими вершинами.
     */
    fun polygonsToPolySet3(polygons: List<JscadPolygon>): PolySet3 {
        val bspPolys = mutableListOf<BspPolygon>()

        for (poly in polygons) {
            val verts = poly.getVertices()
            if (verts.size < 3) continue

            // Fan-триангуляция N-gon -> треугольники
            val v0 = toVec3(verts[0])
            for (i in 1 until verts.size - 1) {
                val v1 = toVec3(verts[i])
                val v2 = toVec3(verts[i + 1])
                if (!degenerate(v0, v1, v2)) {
                    bspPolys.add(BspPolygon(v0, v1, v2))
                }
            }
        }

        return meshFromPolygons(bspPolys)
    }

    /**
     * Прямая конвертация JSCAD Facet -> PolySet3 (для уже триангулированных данных).
     */
    fun facetsToPolySet3(facets: List<eu.printingin3d.javascad.vrl.Facet>): PolySet3 {
        data class VKey(val x: Double, val y: Double, val z: Double)

        val vertexMap = mutableMapOf<VKey, Int>()
        val vertices = mutableListOf<Vec3>()
        val tris = mutableListOf<PolySet3.Triplet>()

        fun getIdx(v: V3d): Int {
            val key = VKey(v.x, v.y, v.z)
            if (key !in vertexMap) {
                vertexMap[key] = vertices.size
                vertices.add(toVec3(v))
            }
            return vertexMap[key]!!
        }

        for (facet in facets) {
            val tri = facet.getTriangle()
            val pts = tri.getPoints()
            val i0 = getIdx(pts[0])
            val i1 = getIdx(pts[1])
            val i2 = getIdx(pts[2])
            if (i0 != i1 && i1 != i2 && i0 != i2) {
                tris.add(PolySet3.Triplet(i0, i1, i2))
            }
        }

        return PolySet3(vertices, tris)
    }

    private fun toVec3(v: V3d): Vec3 = Vec3(v.x, v.y, v.z)

    private fun degenerate(a: Vec3, b: Vec3, c: Vec3): Boolean {
        return (b - a).cross(c - a).squaredLength() < 1e-20
    }
}
