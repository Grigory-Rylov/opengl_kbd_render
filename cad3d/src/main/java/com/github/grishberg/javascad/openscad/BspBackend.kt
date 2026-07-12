package com.github.grishberg.javascad.openscad

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

        val resultPolygons: List<Polygon> = when (op) {
            OpenSCADOperator.UNION -> {
                var current = polygonLists[0]
                for (i in 1 until polygonLists.size) {
                    current = RobustCsg.union(current, polygonLists[i], autoRepair)
                }
                current
            }

            OpenSCADOperator.DIFFERENCE -> {
                var current = polygonLists[0]
                for (i in 1 until polygonLists.size) {
                    current = RobustCsg.difference(current, polygonLists[i], autoRepair)
                }
                current
            }

            OpenSCADOperator.INTERSECTION -> {
                var current = polygonLists[0]
                for (i in 1 until polygonLists.size) {
                    current = RobustCsg.intersect(current, polygonLists[i], autoRepair)
                }
                current
            }
        }

        val vr = com.github.grishberg.javascad.StlValidator().validate(resultPolygons)
        val result = PolySetGeometry.fromPolygons(resultPolygons)
        result.isManifold = vr.isManifold

        return result
    }
}
