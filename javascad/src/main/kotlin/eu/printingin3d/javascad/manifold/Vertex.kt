package eu.printingin3d.javascad.manifold

import eu.printingin3d.javascad.coords.V3d

class Vertex(val id: Long, var position: V3d) {
    var outgoing: HalfEdge? = null
    var normal: V3d = V3d.ZERO

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Vertex && id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
