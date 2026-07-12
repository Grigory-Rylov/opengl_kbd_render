package com.github.grishberg.javascad.openscad

import com.github.grishberg.javascad.StlRepairer
import com.github.grishberg.javascad.StlValidator
import eu.printingin3d.javascad.vrl.CSG
import eu.printingin3d.javascad.vrl.Node
import eu.printingin3d.javascad.vrl.Polygon

object BspBackend {

    fun applyOperator(
        geometries: List<PolySetGeometry>,
        op: OpenSCADOperator,
        autoRepair: Boolean = false
    ): PolySetGeometry {
        if (geometries.isEmpty()) return PolySetGeometry.empty()
        if (geometries.size == 1) return geometries[0]

        val polygonLists = geometries.map { it.toPolygons() }

        val resultCsg: CSG = when (op) {
            OpenSCADOperator.UNION -> {
                var csg = CSG(polygonLists[0])
                for (i in 1 until polygonLists.size) {
                    val a = csg
                    val b = CSG(polygonLists[i])
                    csg = a.union(b)
                }
                csg
            }

            OpenSCADOperator.DIFFERENCE -> {
                var csg = CSG(polygonLists[0])
                for (i in 1 until polygonLists.size) {
                    val a = csg
                    val b = CSG(polygonLists[i])
                    csg = a.difference(b)
                }
                csg
            }

            OpenSCADOperator.INTERSECTION -> {
                var csg = CSG(polygonLists[0])
                for (i in 1 until polygonLists.size) {
                    val a = csg
                    val b = CSG(polygonLists[i])
                    csg = a.intersect(b)
                }
                csg
            }
        }

        val resultPolygons = resultCsg.getPolygons()
        val result = PolySetGeometry.fromPolygons(resultPolygons)

        val validator = StlValidator()
        val validationResult = validator.validate(resultPolygons)
        result.isManifold = validationResult.isManifold

        if (!result.isManifold && autoRepair) {
            val (repaired, _) = StlRepairer().repair(resultPolygons)
            val repairedResult = PolySetGeometry.fromPolygons(repaired)
            val repairedValidation = validator.validate(repaired)
            repairedResult.isManifold = repairedValidation.isManifold
            return repairedResult
        }

        return result
    }
}
