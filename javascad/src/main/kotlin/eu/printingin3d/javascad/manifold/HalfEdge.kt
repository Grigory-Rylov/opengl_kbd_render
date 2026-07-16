package eu.printingin3d.javascad.manifold

class HalfEdge(val id: Long) {
    var vertex: Vertex? = null
    var twin: HalfEdge? = null
    var next: HalfEdge? = null
    var prev: HalfEdge? = null
    var face: Face? = null
    var edge: Edge? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is HalfEdge && id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
