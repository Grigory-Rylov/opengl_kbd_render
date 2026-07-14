package eu.printingin3d.javascad.manifold

import eu.printingin3d.javascad.coords.V3d

class Face(val id: Long) {
    var halfedge: HalfEdge? = null
    var normal: V3d = V3d.ZERO
    var inside: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return other is Face && id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
