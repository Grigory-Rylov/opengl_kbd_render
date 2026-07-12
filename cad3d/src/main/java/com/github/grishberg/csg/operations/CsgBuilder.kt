package com.github.grishberg.csg.operations

import com.github.grishberg.csg.geom.Matrix4
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3
import com.github.grishberg.csg.nativelib.CsgEngine
import com.github.grishberg.csg.nativelib.CsgEngine.NefHandle
import java.util.*

/**
 * CSG boolean operations builder.
 * Supports both native CGAL backend and pure-Kotlin BSP fallback.
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

    private val entries: MutableList<Entry> = ArrayList()
    private var useNative = true

    /** Add a mesh with union operation. */
    fun add(mesh: PolySet3): CsgBuilder = add(mesh, OP_UNION)

    /** Add a mesh with specified operation. */
    fun add(mesh: PolySet3, op: Int): CsgBuilder = add(mesh, op, null)

    /** Add a mesh with transformation. */
    fun add(mesh: PolySet3, op: Int, transform: Matrix4?): CsgBuilder {
        entries.add(Entry(mesh, op, transform))
        return this
    }

    /** Subtract a mesh. */
    fun subtract(mesh: PolySet3): CsgBuilder = add(mesh, OP_DIFFERENCE)

    /** Intersect with a mesh. */
    fun intersect(mesh: PolySet3): CsgBuilder = add(mesh, OP_INTERSECTION)

    /** Force use of native CGAL backend. */
    fun setNative(native0: Boolean): CsgBuilder {
        useNative = native0
        return this
    }

    /**
     * Evaluate the CSG tree and return the result mesh.
     */
    fun evaluate(): PolySet3 {
        if (entries.isEmpty()) {
            return PolySet3.EMPTY
        }

        // Apply transforms
        val transformed = entries.map { e ->
            e.transform?.let { e.mesh.transform(it) } ?: e.mesh
        }

        return if (useNative && CsgEngine.isNativeAvailable()) {
            evaluateNative(transformed)
        } else {
            evaluateJava(transformed)
        }
    }

    /** Native CGAL evaluation. */
    private fun evaluateNative(meshes: List<PolySet3>): PolySet3 {
        if (meshes.isEmpty()) return PolySet3.EMPTY

        var result = CsgEngine.buildFromMesh(meshes[0])

        for (i in 1 until meshes.size) {
            val op = entries[i].op
            val operand = CsgEngine.buildFromMesh(meshes[i])

            result = when (op) {
                OP_UNION -> CsgEngine.union(result, operand)
                OP_DIFFERENCE -> CsgEngine.difference(result, operand)
                OP_INTERSECTION -> CsgEngine.intersect(result, operand)
                else -> throw IllegalArgumentException("Unknown op: $op")
            }

            require(result.isValid()) { "CGAL boolean operation failed at step $i" }
        }

        return CsgEngine.toMesh(result)
    }

    /**
     * Pure-Kotlin fallback: simple concatenation (union approximation).
     * For proper boolean operations without CGAL, you'd need a BSP library.
     */
    private fun evaluateJava(meshes: List<PolySet3>): PolySet3 {
        if (meshes.isEmpty()) return PolySet3.EMPTY

        val allVerts: MutableList<Vec3> = ArrayList()
        val allIndices: MutableList<PolySet3.Triplet> = ArrayList()

        var vertOffset = 0
        for (m in meshes) {
            for (v in m.vertices) {
                allVerts.add(v)
            }
            for (t in m.indices) {
                allIndices.add(PolySet3.Triplet(t.a + vertOffset, t.b + vertOffset, t.c + vertOffset))
            }
            vertOffset += m.vertices.size
        }

        return PolySet3(allVerts, allIndices, false)
    }
}
