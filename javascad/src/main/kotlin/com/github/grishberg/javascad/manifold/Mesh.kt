package com.github.grishberg.javascad.manifold

import com.github.grishberg.javascad.coords.Triangle3d
import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.utils.Color
import com.github.grishberg.javascad.vrl.Facet

class Mesh {
    val vertices = mutableMapOf<Long, Vertex>()
    val edges = mutableMapOf<Long, Edge>()
    val faces = mutableMapOf<Long, Face>()
    val halfedges = mutableMapOf<Long, HalfEdge>()

    private var nextVertexId = 0L
    private var nextEdgeId = 0L
    private var nextFaceId = 0L
    private var nextHalfEdgeId = 0L

    fun newVertex(pos: V3d): Vertex {
        val v = Vertex(nextVertexId++, pos)
        vertices[v.id] = v
        return v
    }

    fun newEdge(): Edge {
        val e = Edge(nextEdgeId++)
        edges[e.id] = e
        return e
    }

    fun newFace(): Face {
        val f = Face(nextFaceId++)
        faces[f.id] = f
        return f
    }

    fun newHalfEdge(): HalfEdge {
        val he = HalfEdge(nextHalfEdgeId++)
        halfedges[he.id] = he
        return he
    }

    /**
     * Converts the mesh into a list of Facets for OpenGL/STL export.
     * Performs fan triangulation for N-gons.
     */
    fun toFacets(): List<Facet> {
        val facets = mutableListOf<Facet>()
        for (face in faces.values) {
            val verts = faceVertices(face)
            if (verts.size < 3) continue

            // Fan triangulation
            val v0 = verts[0]
            for (i in 1 until verts.size - 1) {
                facets.add(Facet(Triangle3d(verts[i], v0, verts[i + 1]), face.normal, Color.white))
            }
        }
        return facets
    }

    private fun faceVertices(face: Face): List<V3d> {
        val result = mutableListOf<V3d>()
        var he = face.halfedge ?: return result
        do {
            result.add(he.vertex?.position ?: continue)
            he = he.next ?: break
        } while (he != face.halfedge)
        return result
    }

    fun computeNormals() {
        for (face in faces.values) {
            val verts = faceVertices(face)
            if (verts.size >= 3) {
                face.normal = faceNormal(verts)
            }
        }
        for (vertex in vertices.values) {
            var nx = 0.0
            var ny = 0.0
            var nz = 0.0
            vertex.outgoing?.let { he ->
                var curr = he
                do {
                    nx += curr.face?.normal?.x ?: 0.0
                    ny += curr.face?.normal?.y ?: 0.0
                    nz += curr.face?.normal?.z ?: 0.0
                    curr = curr.prev?.twin ?: break
                } while (curr != he)
            }
            val len = Math.sqrt(nx * nx + ny * ny + nz * nz)
            if (len > 1e-10) {
                vertex.normal = V3d(nx / len, ny / len, nz / len)
            }
        }
    }

    private fun faceNormal(verts: List<V3d>): V3d {
        var nx = 0.0
        var ny = 0.0
        var nz = 0.0
        for (i in verts.indices) {
            val p0 = verts[i]
            val p1 = verts[(i + 1) % verts.size]
            nx += (p0.y - p1.y) * (p0.z + p1.z)
            ny += (p0.z - p1.z) * (p0.x + p1.x)
            nz += (p0.x - p1.x) * (p0.y + p1.y)
        }
        val len = Math.sqrt(nx * nx + ny * ny + nz * nz)
        return if (len > 1e-10) V3d(nx / len, ny / len, nz / len) else V3d.ZERO
    }
}
