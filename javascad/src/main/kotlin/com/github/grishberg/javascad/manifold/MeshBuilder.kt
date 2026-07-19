package com.github.grishberg.javascad.manifold

import com.github.grishberg.javascad.coords.V3d

/**
 * DSL for building Half-edge meshes.
 */
class MeshBuilder {
    private val mesh = Mesh()

    /**
     * Adds an N-gon face defined by a list of vertex positions (CCW order).
     */
    fun addFace(positions: List<V3d>): Face {
        if (positions.size < 3) throw IllegalArgumentException("Face must have at least 3 vertices")

        val vertices = positions.map { mesh.newVertex(it) }.toMutableList()
        val face = mesh.newFace()
        val halfedges = mutableListOf<HalfEdge>()
        val edges = mutableListOf<Edge>()

        for (i in vertices.indices) {
            val v = vertices[i]
            val nextV = vertices[(i + 1) % vertices.size]

            val he = mesh.newHalfEdge()
            val tHe = mesh.newHalfEdge()
            val edge = mesh.newEdge()

            he.vertex = v
            he.face = face
            he.edge = edge
            he.twin = tHe

            tHe.vertex = nextV
            tHe.edge = edge
            tHe.twin = he

            v.outgoing = he
            edge.halfedge = he

            halfedges.add(he)
            edges.add(edge)
        }

        for (i in halfedges.indices) {
            val he = halfedges[i]
            val nextHe = halfedges[(i + 1) % halfedges.size]
            he.next = nextHe
            he.prev = halfedges[(i - 1 + halfedges.size) % halfedges.size]
        }

        face.halfedge = halfedges[0]
        return face
    }

    /**
     * Merges two vertices that are within a given threshold distance.
     */
    fun mergeVertices(threshold: Double = 1e-8) {
        val vertices = mesh.vertices.values.toList()
        val merged = mutableMapOf<Long, Long>()

        for (i in vertices.indices) {
            if (i.toLong() in merged.values) continue
            for (j in (i + 1) until vertices.size) {
                if (j.toLong() in merged.values) continue
                val v1 = vertices[i]
                val v2 = vertices[j]
                val dx = v1.position.x - v2.position.x
                val dy = v1.position.y - v2.position.y
                val dz = v1.position.z - v2.position.z
                if (dx * dx + dy * dy + dz * dz < threshold * threshold) {
                    mergeTwoVertices(v1, v2)
                    merged[j.toLong()] = i.toLong()
                }
            }
        }
    }

    private fun mergeTwoVertices(from: Vertex, to: Vertex) {
        to.position = V3d(
            (from.position.x + to.position.x) / 2.0,
            (from.position.y + to.position.y) / 2.0,
            (from.position.z + to.position.z) / 2.0
        )

        from.outgoing?.let { he ->
            var curr = he
            do {
                curr.vertex = to
                curr = curr.prev?.twin ?: break
            } while (curr != he)
        }

        if (to.outgoing == null) {
            to.outgoing = from.outgoing
        }

        mesh.vertices.remove(from.id)
    }

    /**
     * Finalize the mesh: merge close vertices and compute normals.
     */
    fun build(threshold: Double = 1e-8): Mesh {
        mergeVertices(threshold)
        mesh.computeNormals()
        return mesh
    }

    fun buildRaw(): Mesh = mesh
}
