package com.github.grishberg.javascad.optimizator

import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.vrl.Polygon
import java.util.Objects

class PolygonEdge(val polygon: Polygon, val p0: V3d, val p1: V3d, val firstPointIndex: Int) {

    override fun equals(o: Any?): Boolean {
        if (o == null || javaClass != o.javaClass) {
            return false
        }
        val that = o as PolygonEdge
        return firstPointIndex == that.firstPointIndex && polygon == that.polygon && p0 == that.p0 && p1 == that.p1
    }

    override fun hashCode(): Int {
        return Objects.hash(polygon, p0, p1, firstPointIndex)
    }
}
