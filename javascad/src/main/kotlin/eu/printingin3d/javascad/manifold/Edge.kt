package eu.printingin3d.javascad.manifold

class Edge(val id: Long) {
    var halfedge: HalfEdge? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Edge && id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
