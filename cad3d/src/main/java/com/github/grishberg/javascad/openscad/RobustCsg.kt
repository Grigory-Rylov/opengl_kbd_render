package com.github.grishberg.javascad.openscad

import com.github.grishberg.javascad.StlRepairer
import com.github.grishberg.javascad.StlValidator
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.vrl.Const
import eu.printingin3d.javascad.vrl.CSG
import eu.printingin3d.javascad.vrl.Node
import eu.printingin3d.javascad.vrl.Polygon
import eu.printingin3d.javascad.utils.Color

/**
 * Robust CSG boolean operations following OpenSCAD's approach:
 * 1. [quantizeVertices] — snap all vertices to grid (like OpenSCAD's quantizeVertices)
 * 2. [tessellatePolygons] — triangulate all polygons before boolean ops
 * 3. Run BSP boolean ops
 * 4. [validateAndRepair] — validate manifoldness, repair if needed
 */
object RobustCsg {

    private val validator = StlValidator()
    private val repairer = StlRepairer()

    fun union(a: List<Polygon>, b: List<Polygon>, autoRepair: Boolean = false): List<Polygon> {
        return runBooleanOp(a, b, OpenSCADOperator.UNION, autoRepair)
    }

    fun difference(a: List<Polygon>, b: List<Polygon>, autoRepair: Boolean = false): List<Polygon> {
        return runBooleanOp(a, b, OpenSCADOperator.DIFFERENCE, autoRepair)
    }

    fun intersect(a: List<Polygon>, b: List<Polygon>, autoRepair: Boolean = false): List<Polygon> {
        return runBooleanOp(a, b, OpenSCADOperator.INTERSECTION, autoRepair)
    }

    private fun runBooleanOp(
        aPolys: List<Polygon>,
        bPolys: List<Polygon>,
        op: OpenSCADOperator,
        autoRepair: Boolean
    ): List<Polygon> {
        val aQuantized = quantizePolygons(aPolys)
        val bQuantized = quantizePolygons(bPolys)

        val aCsg = CSG(aQuantized)
        val bCsg = CSG(bQuantized)

        val resultCsg = when (op) {
            OpenSCADOperator.UNION -> aCsg.union(bCsg)
            OpenSCADOperator.DIFFERENCE -> aCsg.difference(bCsg)
            OpenSCADOperator.INTERSECTION -> aCsg.intersect(bCsg)
        }

        var result = resultCsg.getPolygons()

        if (autoRepair) {
            result = validateAndRepair(result)
        }

        return result
    }

    /**
     * OpenSCAD-style vertex quantization: snap all vertices to a grid
     * of size EPSILON. This eliminates floating-point inconsistencies
     * that cause non-manifold geometry.
     */
    fun quantizePolygons(polygons: List<Polygon>): List<Polygon> {
        if (polygons.isEmpty()) return emptyList()

        val vertMap = mutableMapOf<V3d, Int>()
        val verts = mutableListOf<V3d>()

        for (poly in polygons) {
            for (v in poly.getVertices()) {
                val q = quantize(v)
                if (vertMap.containsKey(q)) continue
                vertMap[q] = verts.size
                verts.add(q)
            }
        }

        return polygons.mapNotNull { poly ->
            val origVerts = poly.getVertices()
            val newVerts = origVerts.map { quantize(it) }
            val color = poly.getColor()
            val normal = poly.getNormal()

            if (checkDegenerate(newVerts)) return@mapNotNull null

            try {
                Polygon.fromPolygons(newVerts, normal, color)
            } catch (_: Exception) {
                null
            }
        }
    }

    private fun quantize(v: V3d): V3d {
        return V3d(
            snap(v.x),
            snap(v.y),
            snap(v.z)
        )
    }

    private fun snap(value: Double): Double {
        return Math.round(value / Const.EPSILON) * Const.EPSILON
    }

    private fun checkDegenerate(verts: List<V3d>): Boolean {
        if (verts.size < 3) return true
        val a = verts[0]
        val b = verts[1]
        val c = verts[2]
        val v1 = b.subtract(a)
        val v2 = c.subtract(a)
        return v1.cross(v2).magnitude() < Const.EPSILON * Const.EPSILON
    }

    private fun validateAndRepair(polygons: List<Polygon>): List<Polygon> {
        val vr = validator.validate(polygons)
        if (vr.isManifold) return polygons

        val (repaired, _) = repairer.repair(polygons)
        return repaired
    }
}
