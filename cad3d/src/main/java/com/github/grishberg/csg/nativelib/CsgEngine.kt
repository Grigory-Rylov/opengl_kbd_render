package com.github.grishberg.csg.nativelib

import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3

/**
 * JNI bindings to native CGAL CSG engine.
 * Provides exact boolean operations via OpenSCAD's CGAL Nef polyhedra.
 */
object CsgEngine {

    private var nativeAvailable: Boolean = false

    init {
        try {
            System.loadLibrary("csg_native")
            nativeAvailable = true
        } catch (e: UnsatisfiedLinkError) {
            System.err.println("Warning: Native CSG engine not loaded. Falling back to pure-Java mode.")
        }
    }

    fun isNativeAvailable(): Boolean = nativeAvailable

    /** Handle to a native CGAL Nef polyhedron. */
    class NefHandle(val ptr: Long) {
        fun isValid(): Boolean = ptr != 0L
    }

    /** Build a Nef polyhedron from a triangular mesh. */
    private external fun nativeBuildFromMesh(verts: DoubleArray, faces: IntArray): Long

    /** Boolean operation. opType: 0=union, 1=intersection, 2=difference */
    private external fun nativeBooleanOp(handleA: Long, handleB: Long, opType: Int): Long

    /** Convert Nef to mesh. */
    private external fun nativeToMesh(handle: Long, outVerts: DoubleArray, outIndices: IntArray)

    /** Export Nef directly to binary STL. */
    private external fun nativeExportStl(handle: Long, filename: String): Boolean

    /** Build a Nef polyhedron from a PolySet3. */
    fun buildFromMesh(mesh: PolySet3): NefHandle {
        require(nativeAvailable) { "Native CSG not available" }

        val verts = DoubleArray(mesh.vertexCount * 3)
        var idx = 0
        for (v in mesh.vertices) {
            verts[idx++] = v.x
            verts[idx++] = v.y
            verts[idx++] = v.z
        }

        val faces = IntArray(mesh.faceCount * 3)
        idx = 0
        for (t in mesh.indices) {
            faces[idx++] = t.a
            faces[idx++] = t.b
            faces[idx++] = t.c
        }

        val ptr = nativeBuildFromMesh(verts, faces)
        return NefHandle(ptr)
    }

    /** Union two meshes. */
    fun union(a: NefHandle, b: NefHandle): NefHandle {
        require(nativeAvailable) { "Native CSG not available" }
        return NefHandle(nativeBooleanOp(a.ptr, b.ptr, 0))
    }

    /** Intersection two meshes. */
    fun intersect(a: NefHandle, b: NefHandle): NefHandle {
        require(nativeAvailable) { "Native CSG not available" }
        return NefHandle(nativeBooleanOp(a.ptr, b.ptr, 1))
    }

    /** Difference two meshes. */
    fun difference(a: NefHandle, b: NefHandle): NefHandle {
        require(nativeAvailable) { "Native CSG not available" }
        return NefHandle(nativeBooleanOp(a.ptr, b.ptr, 2))
    }

    /** Convert Nef handle back to PolySet3. */
    fun toMesh(handle: NefHandle): PolySet3 {
        require(nativeAvailable) { "Native CSG not available" }
        val verts = DoubleArray(1_000_000)
        val indices = IntArray(1_000_000)
        nativeToMesh(handle.ptr, verts, indices)
        return PolySet3.fromArrays(verts, indices)
    }

    /** Export Nef directly to binary STL file. */
    fun exportStl(handle: NefHandle, filename: String): Boolean {
        require(nativeAvailable) { "Native CSG not available" }
        return nativeExportStl(handle.ptr, filename)
    }
}
