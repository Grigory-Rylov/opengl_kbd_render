package com.github.grishberg.csg.operations

import com.github.grishberg.csg.bsp.CsgOp
import com.github.grishberg.csg.bsp.csgOperation
import com.github.grishberg.csg.geom.Matrix4
import com.github.grishberg.csg.geom.PolySet3

/**
 * CSG boolean operations builder — pure Kotlin BSP engine.
 *
 * Usage:
 * ```
 * val result = CsgBuilder()
 *     .add(cube)
 *     .subtract(cylinder)
 *     .evaluate()
 * ```
 */
class CsgBuilder {

    companion object {
        const val OP_UNION = 0
        const val OP_DIFFERENCE = 1
        const val OP_INTERSECTION = 2
    }

    private data class Entry(
        val mesh: PolySet3,
        val op: Int,
        val transform: Matrix4?
    )

    private val entries: MutableList<Entry> = mutableListOf()

    fun add(mesh: PolySet3): CsgBuilder = add(mesh, OP_UNION)

    fun add(mesh: PolySet3, op: Int): CsgBuilder = add(mesh, op, null)

    fun add(mesh: PolySet3, op: Int, transform: Matrix4?): CsgBuilder {
        entries.add(Entry(mesh, op, transform))
        return this
    }

    fun subtract(mesh: PolySet3): CsgBuilder = add(mesh, OP_DIFFERENCE)

    fun intersect(mesh: PolySet3): CsgBuilder = add(mesh, OP_INTERSECTION)

    /**
     * Evaluate CSG tree sequentially: left-to-right, applying each operation
     * to the accumulated result.
     */
    fun evaluate(): PolySet3 {
        if (entries.isEmpty()) return PolySet3.EMPTY

        // Apply transforms
        val transformed = entries.map { e ->
            e.transform?.let { e.mesh.transform(it) } ?: e.mesh
        }

        var result = transformed[0]
        for (i in 1 until transformed.size) {
            val op = when (entries[i].op) {
                OP_UNION -> CsgOp.UNION
                OP_DIFFERENCE -> CsgOp.DIFFERENCE
                OP_INTERSECTION -> CsgOp.INTERSECTION
                else -> throw IllegalArgumentException("Unknown op: ${entries[i].op}")
            }
            result = csgOperation(result, transformed[i], op)
        }

        return result
    }
}
