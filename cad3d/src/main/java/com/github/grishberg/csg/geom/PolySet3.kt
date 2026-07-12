package com.github.grishberg.csg.geom

/**
 * 3D polygon mesh: vertex buffer + triangular face index buffer.
 * Mirrors OpenSCAD's PolySet (triangular mode).
 */
class PolySet3(
    vertices: List<Vec3>,
    indices: List<Triplet>,
    isManifold: Boolean = false
) {
    /** A face triplet: 3 vertex indices. */
    class Triplet(public val a: Int, public val b: Int, public val c: Int)

    public val vertices: List<Vec3> = vertices
    public val indices: List<Triplet> = indices
    public val isManifold: Boolean = isManifold
    public val faceCount: Int = indices.size
    public val vertexCount: Int = vertices.size

    /** Return vertices of face at [fi] as a list of 3 Vec3. */
    fun faceVertices(fi: Int): List<Vec3> {
        val t = indices[fi]
        return listOf(vertices[t.a], vertices[t.b], vertices[t.c])
    }

    /** Compute face normal (non-normalized). */
    fun faceNormal(fi: Int): Vec3 {
        val (p0, p1, p2) = faceVertices(fi)
        return (p1 - p0).cross(p2 - p0)
    }

    /** Compute bounding box. */
    fun boundingBox(): Pair<Vec3, Vec3> {
        if (vertices.isEmpty()) return Vec3.ZERO to Vec3.ZERO
        var min = Vec3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE)
        var max = Vec3(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE)
        for (v in vertices) {
            min = Vec3(minOf(min.x, v.x), minOf(min.y, v.y), minOf(min.z, v.z))
            max = Vec3(maxOf(max.x, v.x), maxOf(max.y, v.y), maxOf(max.z, v.z))
        }
        return min to max
    }

    /** Transform all vertices by a 4x4 matrix (column-major). */
    fun transform(m: Matrix4): PolySet3 {
        val transformed = vertices.map { v ->
            Vec3(
                m.m00 * v.x + m.m01 * v.y + m.m02 * v.z + m.m03,
                m.m10 * v.x + m.m11 * v.y + m.m12 * v.z + m.m13,
                m.m20 * v.x + m.m21 * v.y + m.m22 * v.z + m.m23
            )
        }
        return PolySet3(transformed, indices, isManifold)
    }

    /** Translate mesh. */
    fun translate(dx: Double, dy: Double, dz: Double): PolySet3 =
        transform(Matrix4.translation(dx, dy, dz))

    fun isEmpty(): Boolean = vertices.isEmpty() || indices.isEmpty()

    companion object {
        val EMPTY = PolySet3(emptyList(), emptyList(), false)

        /** Build a PolySet3 from raw double arrays (native interface). */
        @JvmStatic
        fun fromArrays(verts: DoubleArray, faces: IntArray): PolySet3 {
            val vs = mutableListOf<Vec3>()
            for (i in verts.indices step 3) {
                vs.add(Vec3(verts[i], verts[i + 1], verts[i + 2]))
            }
            val fs = mutableListOf<Triplet>()
            for (i in faces.indices step 3) {
                fs.add(Triplet(faces[i], faces[i + 1], faces[i + 2]))
            }
            return PolySet3(vs, fs, true)
        }
    }
}
