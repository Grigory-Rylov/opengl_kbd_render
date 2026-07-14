package eu.printingin3d.javascad.manifold

import eu.printingin3d.javascad.vrl.CSG
import eu.printingin3d.javascad.vrl.Facet
import eu.printingin3d.javascad.vrl.Polygon

/**
 * Native manifold3d-based CSG wrapper.
 *
 * Usage:
 * ```kotlin
 * val csg = ManifoldCsg(polygons)
 * val result = csg.union(otherCsg.polygons)
 * val result2 = result.difference(anotherCsg.polygons)
 * ```
 */
class ManifoldCsg(val polygons: List<Polygon>) {

    fun union(other: List<Polygon>): ManifoldCsg =
        ManifoldCsg(Manifold3dEngine.union(polygons, other))

    fun difference(other: List<Polygon>): ManifoldCsg =
        ManifoldCsg(Manifold3dEngine.difference(polygons, other))

    fun intersect(other: List<Polygon>): ManifoldCsg =
        ManifoldCsg(Manifold3dEngine.intersection(polygons, other))

    fun toFacets(): List<Facet> {
        val facets = mutableListOf<Facet>()
        for (poly in polygons) {
            val pts = poly.vertices
            if (pts.size >= 3) {
                facets.addAll(poly.toFacets())
            }
        }
        return facets
    }

    companion object {
        fun fromCSG(csg: CSG): ManifoldCsg = ManifoldCsg(csg.polygons)
        fun empty(): ManifoldCsg = ManifoldCsg(emptyList())
    }
}

/**
 * Extends CSG to add native manifold operations.
 */
fun CSG.manifoldUnion(other: CSG): CSG {
    val result = Manifold3dEngine.union(this.polygons, other.polygons)
    return CSG(result)
}

fun CSG.manifoldDifference(other: CSG): CSG {
    val result = Manifold3dEngine.difference(this.polygons, other.polygons)
    return CSG(result)
}

fun CSG.manifoldIntersect(other: CSG): CSG {
    val result = Manifold3dEngine.intersection(this.polygons, other.polygons)
    return CSG(result)
}
