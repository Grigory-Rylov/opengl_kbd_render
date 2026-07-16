package eu.printingin3d.javascad.manifold

import eu.printingin3d.javascad.coords.V3d

/**
 * Static operations on Half-edge meshes: split, merge, collapse, flip, verify.
 */
object MeshOps {

    /**
     * Splits the halfedge at a new vertex position.
     * Returns the new halfedge pointing to the new vertex.
     */
    fun splitEdge(mesh: Mesh, he: HalfEdge, newPos: V3d): HalfEdge {
        val newVertex = mesh.newVertex(newPos)
        val twin = he.twin ?: throw IllegalStateException("Halfedge has no twin")

        val newHe = mesh.newHalfEdge()
        val newTwin = mesh.newHalfEdge()
        val newEdge = mesh.newEdge()

        newHe.vertex = newVertex
        newHe.face = he.face
        newHe.edge = newEdge
        newHe.twin = newTwin
        newHe.prev = he
        newHe.next = he.next
        he.next?.prev = newHe
        he.next = newHe

        newTwin.vertex = twin.vertex
        newTwin.face = twin.face
        newTwin.edge = newEdge
        newTwin.twin = newHe
        newTwin.prev = twin.prev
        newTwin.next = twin
        twin.prev?.next = newTwin
        twin.prev = newTwin

        if (he.vertex?.outgoing == he) {
            he.vertex?.outgoing = newHe
        }
        if (twin.vertex?.outgoing == twin) {
            twin.vertex?.outgoing = newTwin
        }

        newVertex.outgoing = newHe
        he.edge = newEdge
        twin.edge = newEdge

        he.vertex = newVertex
        return newHe
    }

    /**
     * Splits a face by adding a diagonal between two existing vertices on the face boundary.
     */
    fun splitFace(mesh: Mesh, face: Face, v1: Vertex, v2: Vertex) {
        val he1 = findHalfEdgeStartingAt(face, v1)
        val he2 = findHalfEdgeStartingAt(face, v2)
        if (he1 == null || he2 == null) return

        val newEdge = mesh.newEdge()
        val he1New = mesh.newHalfEdge()
        val he2New = mesh.newHalfEdge()

        he1New.vertex = v1
        he1New.face = face
        he1New.edge = newEdge
        he1New.twin = he2New
        he1New.next = he2
        he1New.prev = he1.prev
        he1.prev?.next = he1New
        he2.prev = he1New

        he2New.vertex = v2
        he2New.face = face
        he2New.edge = newEdge
        he2New.twin = he1New
        he2New.next = he1
        he2New.prev = he2.prev
        he2.prev?.next = he2New
        he1.prev = he2New

        newEdge.halfedge = he1New
    }

    /**
     * Collapses an edge, merging head and tail vertices.
     */
    fun collapseEdge(mesh: Mesh, edge: Edge) {
        val he = edge.halfedge ?: return
        val twin = he.twin ?: return
        val fromV = he.vertex ?: return
        val toV = he.vertex ?: return

        fromV.position = V3d(
            (fromV.position.x + toV.position.x) / 2.0,
            (fromV.position.y + toV.position.y) / 2.0,
            (fromV.position.z + toV.position.z) / 2.0
        )

        // Rewire faces
        val face1 = he.face
        val face2 = twin.face

        he.prev?.next = he.next
        he.next?.prev = he.prev
        twin.prev?.next = twin.next
        twin.next?.prev = twin.prev

        mesh.faces.remove(face1?.id)
        mesh.faces.remove(face2?.id)
        mesh.vertices.remove(fromV.id)
        mesh.halfedges.remove(he.id)
        mesh.halfedges.remove(twin.id)
        mesh.edges.remove(edge.id)
    }

    /**
     * Flips an internal edge (swaps diagonal in a quad formed by two triangles).
     */
    fun flipEdge(mesh: Mesh, edge: Edge) {
        val he = edge.halfedge ?: return
        val twin = he.twin ?: return
        val face1 = he.face ?: return
        val face2 = twin.face ?: return

        val v1 = he.vertex
        val v2 = twin.vertex
        val v3 = he.next?.vertex
        val v4 = twin.next?.vertex

        if (v1 == null || v2 == null || v3 == null || v4 == null) return

        val newEdge = mesh.newEdge()
        val newHe = mesh.newHalfEdge()
        val newTwin = mesh.newHalfEdge()

        newHe.vertex = v3
        newHe.face = face1
        newHe.edge = newEdge
        newHe.twin = newTwin
        newHe.prev = he
        newHe.next = twin.next ?: return

        newTwin.vertex = v4
        newTwin.face = face2
        newTwin.edge = newEdge
        newTwin.twin = newHe
        newTwin.prev = twin
        newTwin.next = he.next ?: return

        face1.halfedge = newHe
        face2.halfedge = newTwin
        newEdge.halfedge = newHe

        mesh.halfedges.remove(he.id)
        mesh.halfedges.remove(twin.id)
        mesh.edges.remove(edge.id)
    }

    /**
     * Verifies manifold invariants. Returns list of violation descriptions.
     */
    fun verifyManifold(mesh: Mesh): List<String> {
        val errors = mutableListOf<String>()

        for (he in mesh.halfedges.values) {
            if (he.twin?.twin != he) errors.add("he.twin.twin != he for HE ${he.id}")
            if (he.next?.prev != he) errors.add("he.next.prev != he for HE ${he.id}")
            if (he.face == null) errors.add("he.face is null for HE ${he.id}")
            if (he.vertex == null) errors.add("he.vertex is null for HE ${he.id}")
        }

        for (face in mesh.faces.values) {
            var count = 0
            var curr = face.halfedge
            while (curr != null && count < 1000) {
                if (curr.face != face) errors.add("Face ${face.id} contains HE ${curr.id} from different face")
                curr = curr.next
                count++
            }
            if (count >= 1000) errors.add("Infinite loop in face ${face.id}")
            if (count < 3) errors.add("Face ${face.id} has less than 3 halfedges")
        }

        return errors
    }

    private fun findHalfEdgeStartingAt(face: Face, vertex: Vertex): HalfEdge? {
        var he = face.halfedge ?: return null
        do {
            if (he.vertex == vertex) return he
            he = he.next ?: break
        } while (he != face.halfedge)
        return null
    }
}
