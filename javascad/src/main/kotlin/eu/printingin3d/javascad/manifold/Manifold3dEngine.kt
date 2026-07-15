package eu.printingin3d.javascad.manifold

import com.cadoodlecad.manifold.ManifoldBindings
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.Polygon
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

object Manifold3dEngine {

    private val lock = ReentrantLock()
    private var bindings: ManifoldBindings? = null

    private fun getBindings(): ManifoldBindings =
        bindings ?: lock.withLock { bindings ?: ManifoldBindings().also { bindings = it } }

    // ---- CSG operations (Polygon) ----

    fun union(a: List<Polygon>, b: List<Polygon>): List<Polygon> =
        operatePolygons(a, b, ManifoldBindings.OPTYPE_UNION)

    fun difference(a: List<Polygon>, b: List<Polygon>): List<Polygon> =
        operatePolygons(a, b, ManifoldBindings.OPTYPE_DIFFERENCE)

    fun intersection(a: List<Polygon>, b: List<Polygon>): List<Polygon> =
        operatePolygons(a, b, ManifoldBindings.OPTYPE_INTERSECTION)

    // ---- CSG operations (native handles) ----

    fun unionNative(a: Long, b: Long): Long = operateNative(a, b, ManifoldBindings.OPTYPE_UNION)

    fun differenceNative(a: Long, b: Long): Long = operateNative(a, b, ManifoldBindings.OPTYPE_DIFFERENCE)

    fun intersectionNative(a: Long, b: Long): Long = operateNative(a, b, ManifoldBindings.OPTYPE_INTERSECTION)

    // ---- Hull (native handles) ----

    fun hullNative(handles: LongArray): Long {
        if (handles.isEmpty()) return empty()
        if (handles.size == 1) return handles[0]
        val mb = getBindings()
        return mb.batchHull(handles)
    }

    fun hull(a: List<Polygon>, b: List<Polygon>): List<Polygon> {
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a
        val mb = getBindings()
        val manA = polygonsToManifold(mb, a)
        val manB = polygonsToManifold(mb, b)
        return try {
            val result = mb.batchHull(longArrayOf(manA, manB))
            if (mb.isEmpty(result)) emptyList() else manifoldToPolygons(mb, result)
        } catch (e: Exception) {
            println("  [hull] error: ${e.message}")
            emptyList()
        }
    }

    // ---- Native primitives ----

    fun sphere(radius: Double, segments: Int): List<Polygon> {
        val mb = getBindings()
        val man = mb.sphere(radius, segments)
        return manifoldToPolygons(mb, man)
    }

    fun cube(w: Double, h: Double, d: Double, center: Boolean = true): List<Polygon> {
        val mb = getBindings()
        val man = mb.cube(w, h, d, center)
        return manifoldToPolygons(mb, man)
    }

    fun cylinder(radius: Double, height: Double, segments: Int): List<Polygon> {
        val mb = getBindings()
        val man = mb.cylinder(radius, height, segments.toDouble(), segments, segments)
        return manifoldToPolygons(mb, man)
    }

    // ---- Transform primitives (native handles) ----

    // ---- Native transform operations ----

    fun translate(manifold: Long, tx: Double, ty: Double, tz: Double): Long {
        val mb = getBindings()
        return mb.translate(manifold, tx, ty, tz)
    }

    fun rotate(manifold: Long, rx: Double, ry: Double, rz: Double): Long {
        val mb = getBindings()
        return mb.rotate(manifold, rx, ry, rz)
    }

    fun scale(manifold: Long, sx: Double, sy: Double, sz: Double): Long {
        val mb = getBindings()
        return mb.scale(manifold, sx, sy, sz)
    }

    fun transform(manifold: Long, m: DoubleArray): Long {
        val mb = getBindings()
        return mb.transform(manifold,
            m[0], m[1], m[2], m[3],
            m[4], m[5], m[6], m[7],
            m[8], m[9], m[10], m[11])
    }

    fun transformAndReturn(manifold: Long, tx: Double, ty: Double, tz: Double): Long {
        val mb = getBindings()
        return mb.transform(manifold,
            1.0, 0.0, 0.0, tx,
            0.0, 1.0, 0.0, ty,
            0.0, 0.0, 1.0, tz)
    }

    fun empty(): Long {
        val mb = getBindings()
        return mb.empty()
    }

    fun isEmpty(manifold: Long): Boolean {
        val mb = getBindings()
        return mb.isEmpty(manifold)
    }

    fun delete(manifold: Long) {
        val mb = getBindings()
        mb.delete(manifold)
    }

    fun centerOfPolygons(polygons: List<Polygon>): V3d {
        val mb = getBindings()
        val man = polygonsToManifold(mb, polygons)
        return try {
            val b = mb.getBounds(man)
            V3d(b.centerX, b.centerY, b.centerZ)
        } finally {
            mb.delete(man)
        }
    }

    fun centerOfNative(manifold: Long): V3d {
        val mb = getBindings()
        val b = mb.getBounds(manifold)
        return V3d(b.centerX, b.centerY, b.centerZ)
    }

    fun manifoldToPolygonsExport(manifold: Long): List<Polygon> {
        val mb = getBindings()
        val data = mb.exportMeshGL64(manifold)
        val verts = data.vertices()
        val tris = data.triangles()
        val triCount = data.triCount().toInt()
        val vertCount = verts.size / 3
        println("  [export] triCount=$triCount, vertCount=$vertCount")
        if (triCount == 0) return emptyList()

        // Validate triangle indices
        var maxIdx = 0L
        var minIdx = Long.MAX_VALUE
        for (i in 0 until triCount * 3) {
            maxIdx = maxOf(maxIdx, tris[i])
            minIdx = minOf(minIdx, tris[i])
        }
        println("  [export] tri indices range: [$minIdx, $maxIdx], verts=$vertCount")

        // Check vertex range
        var vxMin = Double.MAX_VALUE
        var vxMax = -Double.MAX_VALUE
        var vyMin = Double.MAX_VALUE
        var vyMax = -Double.MAX_VALUE
        var vzMin = Double.MAX_VALUE
        var vzMax = -Double.MAX_VALUE
        for (v in 0 until vertCount) {
            vxMin = minOf(vxMin, verts[v * 3])
            vxMax = maxOf(vxMax, verts[v * 3])
            vyMin = minOf(vyMin, verts[v * 3 + 1])
            vyMax = maxOf(vyMax, verts[v * 3 + 1])
            vzMin = minOf(vzMin, verts[v * 3 + 2])
            vzMax = maxOf(vzMax, verts[v * 3 + 2])
        }
        println("  [export] vertex range: X[$vxMin, $vxMax] Y[$vyMin, $vyMax] Z[$vzMin, $vzMax]")

        val result = ArrayList<Polygon>(triCount)
        for (i in 0 until triCount) {
            val vi0 = tris[i * 3].toInt()
            val vi1 = tris[i * 3 + 1].toInt()
            val vi2 = tris[i * 3 + 2].toInt()

            if (vi0 < 0 || vi0 >= vertCount || vi1 < 0 || vi1 >= vertCount || vi2 < 0 || vi2 >= vertCount) {
                println("  [export] INVALID tri $i: indices ($vi0, $vi1, $vi2) vertCount=$vertCount")
                continue
            }

            val a = V3d(verts[vi0 * 3], verts[vi0 * 3 + 1], verts[vi0 * 3 + 2])
            val b = V3d(verts[vi1 * 3], verts[vi1 * 3 + 1], verts[vi1 * 3 + 2])
            val c = V3d(verts[vi2 * 3], verts[vi2 * 3 + 1], verts[vi2 * 3 + 2])

            val ab = b.subtract(a)
            val ac = c.subtract(a)
            val n = ab.cross(ac).unit()
            result.add(Polygon.fromPolygons(listOf(a, b, c), n, Color.white))
        }
        mb.delete(manifold)
        return result
    }

    fun bindings(): ManifoldBindings = getBindings()

    // ---- Low-level native operations ----

    fun operateNative(a: Long, b: Long, opType: Int): Long {
        val mb = getBindings()
        return when (opType) {
            ManifoldBindings.OPTYPE_UNION -> mb.union(a, b)
            ManifoldBindings.OPTYPE_DIFFERENCE -> mb.difference(a, b)
            ManifoldBindings.OPTYPE_INTERSECTION -> mb.intersection(a, b)
            else -> throw IllegalArgumentException("Unknown op: $opType")
        }
    }

    fun nativeHull(handles: LongArray): Long {
        val mb = getBindings()
        return mb.batchHull(handles)
    }

    fun nativeDelete(handle: Long) {
        val mb = getBindings()
        mb.delete(handle)
    }

    // ---- Polygon -> manifold ----

    fun polygonsToManifold(mb: ManifoldBindings, polygons: List<Polygon>): Long {
        val vertices = java.util.ArrayList<Double>()
        val triangles = java.util.ArrayList<Long>()

        for (poly in polygons) {
            val pts = poly.vertices
            if (pts.size < 3) continue
            val v0 = pts[0]
            for (i in 1 until pts.size - 1) {
                triangles.add(addVertex(vertices, v0))
                triangles.add(addVertex(vertices, pts[i]))
                triangles.add(addVertex(vertices, pts[i + 1]))
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

    // ---- manifold -> Polygon ----

    private fun operatePolygons(a: List<Polygon>, b: List<Polygon>, opType: Int): List<Polygon> {
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
            if (mb.isEmpty(result)) emptyList() else manifoldToPolygons(mb, result)
        } finally {
            mb.delete(manA)
            mb.delete(manB)
        }
    }

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

                val ab = b.subtract(a)
                val ac = c.subtract(a)
                val n = ab.cross(ac).unit()
                result.add(Polygon.fromPolygons(listOf(a, b, c), n, Color.white))
            }
            return result
        } finally {
            mb.delete(manifold)
        }
    }

    // ---- STL export ----

    fun writeStl(polygons: List<Polygon>, file: File) {
        val tris = mutableListOf<Triangle>()
        for (poly in polygons) {
            val pts = poly.vertices
            if (pts.size == 3) {
                val n = poly.normal
                tris.add(Triangle(pts[0], pts[1], pts[2], n))
            } else if (pts.size > 3) {
                val n = poly.normal
                val v0 = pts[0]
                for (i in 1 until pts.size - 1) {
                    tris.add(Triangle(v0, pts[i], pts[i + 1], n))
                }
            }
        }

        FileOutputStream(file).channel.use { channel ->
            val bb = ByteBuffer.allocate(80 + 4 + tris.size * 50).order(ByteOrder.LITTLE_ENDIAN)
            // 80 byte header
            val header = "binary stl - manifold3d engine".toByteArray()
            for (b in header) bb.put(b)
            for (i in header.size until 80) bb.put(0.toByte())
            bb.putInt(tris.size)

            for (t in tris) {
                bb.putFloat(t.n.x.toFloat())
                bb.putFloat(t.n.y.toFloat())
                bb.putFloat(t.n.z.toFloat())
                for (v in listOf(t.a, t.b, t.c)) {
                    bb.putFloat(v.x.toFloat())
                    bb.putFloat(v.y.toFloat())
                    bb.putFloat(v.z.toFloat())
                }
                bb.putShort(0.toShort())
            }

            bb.flip()
            channel.write(bb)
        }
    }
}

private data class Triangle(val a: V3d, val b: V3d, val c: V3d, val n: V3d)
