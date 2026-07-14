package eu.printingin3d.javascad.manifold

import com.cadoodlecad.manifold.ManifoldBindings
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.Polygon
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Thin wrapper over native manifold3d (JNI) that works with [Polygon] lists.
 *
 * Converts [Polygon] -> manifold mesh -> operation -> manifold mesh -> [Polygon].
 */
object Manifold3dEngine {

    private val lock = ReentrantLock()
    private var bindings: ManifoldBindings? = null

    private fun getBindings(): ManifoldBindings {
        return bindings ?: lock.withLock {
            bindings ?: ManifoldBindings().also { bindings = it }
        }
    }

    // ---- public API (same signature as old ManifoldEngine) ----

    fun union(a: List<Polygon>, b: List<Polygon>): List<Polygon> =
        operate(a, b, ManifoldBindings.OPTYPE_UNION)

    fun difference(a: List<Polygon>, b: List<Polygon>): List<Polygon> =
        operate(a, b, ManifoldBindings.OPTYPE_DIFFERENCE)

    fun intersection(a: List<Polygon>, b: List<Polygon>): List<Polygon> =
        operate(a, b, ManifoldBindings.OPTYPE_INTERSECTION)

    // ---- core ----

    private fun operate(a: List<Polygon>, b: List<Polygon>, opType: Int): List<Polygon> {
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a

        val mb = getBindings()
        val manA = polygonsToManifold(mb, a)
        val manB = polygonsToManifold(mb, b)

        return try {
            val result = when (opType) {
                ManifoldBindings.OPTYPE_UNION -> mb.union(manA, manB)
                ManifoldBindings.OPTYPE_DIFFERENCE -> mb.difference(manA, manB)
                ManifoldBindings.OPTYPE_INTERSECTION -> mb.intersection(manA, manB)
                else -> throw IllegalArgumentException("Unknown op: $opType")
            }
            if (mb.isEmpty(result)) emptyList()
            else manifoldToPolygons(mb, result)
        } finally {
            mb.delete(manA)
            mb.delete(manB)
        }
    }

    // ---- Polygon -> manifold ----

    private fun polygonsToManifold(mb: ManifoldBindings, polygons: List<Polygon>): Long {
        val vertices = java.util.ArrayList<Double>()
        val triangles = java.util.ArrayList<Long>()

        for (poly in polygons) {
            val pts = poly.vertices
            if (pts.size < 3) continue
            val v0 = pts[0]
            for (i in 1 until pts.size - 1) {
                val v1 = pts[i]
                val v2 = pts[i + 1]
                triangles.add(addVertex(vertices, v0))
                triangles.add(addVertex(vertices, v1))
                triangles.add(addVertex(vertices, v2))
            }
        }

        if (vertices.isEmpty()) return mb.empty()

        val vertArray = DoubleArray(vertices.size)
        for (i in vertices.indices) vertArray[i] = vertices[i]

        val triCount = triangles.size / 3
        val triArray = LongArray(triangles.size)
        for (i in triangles.indices) triArray[i] = triangles[i]

        return mb.importMeshGL64(vertArray, triArray, (vertices.size / 3).toLong(), triCount.toLong())
    }

    private fun addVertex(vertices: java.util.ArrayList<Double>, v: V3d): Long {
        val idx = (vertices.size / 3).toLong()
        vertices.add(v.x)
        vertices.add(v.y)
        vertices.add(v.z)
        return idx
    }

    // ---- manifold -> Polygon (one triangle = one polygon) ----

    private fun manifoldToPolygons(mb: ManifoldBindings, manifold: Long): List<Polygon> {
        try {
            val data = mb.exportMeshGL64(manifold)
            val verts = data.vertices()
            val tris = data.triangles()
            val triCount = data.triCount().toInt()

            if (triCount == 0) return emptyList()

            val result = ArrayList<Polygon>(triCount)
            for (i in 0 until triCount) {
                val i0 = (tris[i * 3].toInt() * 3)
                val i1 = (tris[i * 3 + 1].toInt() * 3)
                val i2 = (tris[i * 3 + 2].toInt() * 3)

                val a = V3d(verts[i0], verts[i0 + 1], verts[i0 + 2])
                val b = V3d(verts[i1], verts[i1 + 1], verts[i1 + 2])
                val c = V3d(verts[i2], verts[i2 + 1], verts[i2 + 2])

                val n = b.subtract(a).cross(c.subtract(a)).unit()
                val pa = a.subtract(n.scale(a.dot(n) - n.dot(a)))
                val pb = b.subtract(n.scale(b.dot(n) - n.dot(b)))
                val pc = c.subtract(n.scale(c.dot(n) - n.dot(c)))
                result.add(Polygon.fromPolygons(listOf(pa, pb, pc), n, Color.white))
            }
            return result
        } finally {
            mb.delete(manifold)
        }
    }
}
