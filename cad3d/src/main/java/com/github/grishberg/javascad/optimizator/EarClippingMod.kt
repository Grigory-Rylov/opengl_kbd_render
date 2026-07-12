package com.github.grishberg.javascad.optimizator

import com.github.grishberg.openscad.coords.V3d
import com.github.grishberg.openscad.vrl.Triangle3d
import java.util.*

class EarClippingMod {
    companion object {
        @JvmStatic
        fun triangulate(vertices: List<V3d>, normal: V3d): List<Triangle3d> {
            val result = ArrayList<Triangle3d>()
            if (vertices.size < 3) return result
            for (i in 1 until vertices.size - 1) {
                result.add(Triangle3d(vertices[0], vertices[i], vertices[i + 1]))
            }
            return result
        }
    }
}
