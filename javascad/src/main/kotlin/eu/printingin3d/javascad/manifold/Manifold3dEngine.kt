package eu.printingin3d.javascad.manifold

import com.cadoodlecad.manifold.ManifoldBindings
import com.cadoodlecad.manifold.ManifoldBindings.ManifoldError
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
    @JvmField
    val JNI_SYNC = Any() // Manifold is not thread-safe, use synchronized(JNI_SYNC)

    fun initialize() {
        getBindings()
    }

    // Serialize a block of manifold operations (not thread-safe inside)
    fun <T> synchronizedBlock(block: () -> T): T = synchronized(JNI_SYNC) { block() }

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

    fun hullNative(handles: LongArray): Long = synchronized(JNI_SYNC) {
        if (handles.isEmpty()) return emptyManifold().also { markOwned(it) }
        val mb = getBindings()
        val r = mb.batchHull(handles)
        markOwned(r)
        r
    }

    fun hull(a: List<Polygon>, b: List<Polygon>): List<Polygon> = synchronized(JNI_SYNC) {
        if (a.isEmpty()) return b
        if (b.isEmpty()) return a
        val mb = getBindings()
        val manA = polygonsToManifoldUnlocked(mb, a)
        val manB = polygonsToManifoldUnlocked(mb, b)
        try {
            val result = mb.batchHull(longArrayOf(manA, manB))
            markOwned(result)
            if (mb.isEmpty(result)) {
                deleteTracked(mb, result)
                emptyList()
            } else {
                manifoldToPolygonsUnlocked(mb, result)
            }
        } catch (e: Exception) {
            println("  [hull] error: ${e.message}")
            emptyList()
        } finally {
            deleteTracked(mb, manA)
            deleteTracked(mb, manB)
        }
    }

    // ---- Native primitives ----

    fun sphere(radius: Double, segments: Int): List<Polygon> = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val man = mb.sphere(radius, segments)
        markOwned(man)
        manifoldToPolygonsUnlocked(mb, man)
    }

    fun cube(w: Double, h: Double, d: Double, center: Boolean = true): List<Polygon> = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val man = mb.cube(w, h, d, center)
        markOwned(man)
        manifoldToPolygonsUnlocked(mb, man)
    }

    fun cylinder(radius: Double, height: Double, segments: Int): List<Polygon> = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val man = mb.cylinder(radius, height, segments.toDouble(), segments, segments)
        markOwned(man)
        manifoldToPolygonsUnlocked(mb, man)
    }

    // ---- Native primitives (return owned handles, tracked in liveHandles) ----

    fun sphereNative(radius: Double, segments: Int): Long = synchronized(JNI_SYNC) {
        val r = getBindings().sphere(radius, segments)
        markOwned(r)
        r
    }

    fun cubeNative(w: Double, h: Double, d: Double, center: Boolean): Long = synchronized(JNI_SYNC) {
        val r = getBindings().cube(w, h, d, center)
        markOwned(r)
        r
    }

    fun cylinderNative(
        length: Double,
        bottomRadius: Double,
        topRadius: Double,
        segments: Int,
        circularSegments: Int,
    ): Long = synchronized(JNI_SYNC) {
        val r = getBindings().cylinder(length, bottomRadius, topRadius, segments, circularSegments)
        markOwned(r)
        r
    }

    // ---- Transform primitives (native handles) ----

    // ---- Native transform operations ----

    fun translate(manifold: Long, tx: Double, ty: Double, tz: Double): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val r = mb.translate(manifold, tx, ty, tz)
        markOwned(r)
        r
    }

    fun rotate(manifold: Long, rx: Double, ry: Double, rz: Double): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val r = mb.rotate(manifold, rx, ry, rz)
        markOwned(r)
        r
    }

    fun scale(manifold: Long, sx: Double, sy: Double, sz: Double): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val r = mb.scale(manifold, sx, sy, sz)
        markOwned(r)
        r
    }

    fun transform(manifold: Long, m: DoubleArray): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val r = mb.transform(manifold,
            m[0], m[1], m[2], m[3],
            m[4], m[5], m[6], m[7],
            m[8], m[9], m[10], m[11])
        markOwned(r)
        r
    }

    fun transformAndReturn(manifold: Long, tx: Double, ty: Double, tz: Double): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val r = mb.transform(manifold,
            1.0, 0.0, 0.0, tx,
            0.0, 1.0, 0.0, ty,
            0.0, 0.0, 1.0, tz)
        markOwned(r)
        r
    }

    fun empty(): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val r = mb.empty()
        markOwned(r)
        r
    }

    fun isEmpty(manifold: Long): Boolean = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.isEmpty(manifold)
    }

    private val liveHandles = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()

    fun delete(manifold: Long) = synchronized(JNI_SYNC) {
        if (manifold == 0L) return
        // Only delete handles the engine actually owns. Handles created elsewhere
        // (e.g. imported meshes or bindings-level helpers) are not tracked; skipping
        // them here avoids a double-free / use-after-free that previously crashed the
        // JVM inside the native boolean/manifold code.
        if (!liveHandles.remove(manifold)) {
            return
        }
        val mb = getBindings()
        mb.delete(manifold)
    }

    /** Marks a handle as owned by the engine (created by a native op). */
    fun markOwned(handle: Long) {
        if (handle != 0L) liveHandles.add(handle)
    }

    /**
     * Frees a native handle and drops it from [liveHandles] in one step. Used by
     * engine-internal helpers that consume a handle (e.g. export/bounds) so no
     * stale entry is left behind for [clearAll] to double-free. Idempotent.
     */
    private fun deleteTracked(mb: ManifoldBindings, handle: Long) {
        if (handle == 0L) return
        if (liveHandles.remove(handle)) {
            mb.delete(handle)
        }
    }

    /**
     * Releases every native manifold handle the engine still owns. Intended for
     * long-lived hosts (e.g. the viewer/plugin system) to call between model
     * rebuilds or on plugin unload so leaked intermediate handles do not
     * accumulate in native memory. Safe to call multiple times.
     */
    fun clearAll() = synchronized(JNI_SYNC) {
        if (liveHandles.isEmpty()) return
        val mb = bindings ?: return
        val handles = liveHandles.toList()
        liveHandles.clear()
        for (h in handles) {
            try {
                mb.delete(h)
            } catch (e: Exception) {
                println("  [clearAll] failed to delete handle $h: ${e.message}")
            }
        }
    }

    fun centerOfPolygons(polygons: List<Polygon>): V3d = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val man = polygonsToManifoldUnlocked(mb, polygons)
        try {
            val b = mb.getBounds(man)
            V3d(b.centerX, b.centerY, b.centerZ)
        } finally {
            deleteTracked(mb, man)
        }
    }

    fun centerOfNative(manifold: Long): V3d = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val b = mb.getBounds(manifold)
        V3d(b.centerX, b.centerY, b.centerZ)
    }

    fun manifoldToPolygonsExport(manifold: Long): List<Polygon> = synchronized(JNI_SYNC) {
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
        deleteTracked(mb, manifold)
        return result
    }

    fun bindings(): ManifoldBindings = getBindings()

    // ---- Low-level native operations ----

    fun operateNative(a: Long, b: Long, opType: Int): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val r = when (opType) {
            ManifoldBindings.OPTYPE_UNION -> mb.union(a, b)
            ManifoldBindings.OPTYPE_DIFFERENCE -> mb.difference(a, b)
            ManifoldBindings.OPTYPE_INTERSECTION -> mb.intersection(a, b)
            else -> throw IllegalArgumentException("Unknown op: $opType")
        }
        markOwned(r)
        r
    }

    fun nativeHull(handles: LongArray): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.batchHull(handles)
    }

    fun nativeDelete(handle: Long) = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.delete(handle)
    }

    /** Returns an empty manifold handle (never 0L; safe to pass to native ops). */
    fun emptyManifold(): Long = synchronized(JNI_SYNC) {
        val r = getBindings().empty()
        markOwned(r)
        r
    }

    // ---- Polygon -> manifold ----

    fun polygonsToManifold(mb: ManifoldBindings, polygons: List<Polygon>): Long = synchronized(JNI_SYNC) {
        polygonsToManifoldUnlocked(mb, polygons)
    }

    private fun polygonsToManifoldUnlocked(mb: ManifoldBindings, polygons: List<Polygon>): Long {
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

        if (vertices.isEmpty()) {
            val e = mb.empty()
            markOwned(e)
            return e
        }

        val vertArray = DoubleArray(vertices.size)
        for (i in vertices.indices) vertArray[i] = vertices[i]

        val triCount = triangles.size / 3
        val triArray = LongArray(triangles.size)
        for (i in triangles.indices) triArray[i] = triangles[i]

        val r = mb.importMeshGL64(vertArray, triArray, (vertices.size / 3).toLong(), triCount.toLong())
        markOwned(r)
        return r
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
        synchronized(JNI_SYNC) {
            val mb = getBindings()
            val manA = polygonsToManifoldUnlocked(mb, a)
            val manB = polygonsToManifoldUnlocked(mb, b)
            return try {
                val result = when (opType) {
                    ManifoldBindings.OPTYPE_UNION -> mb.union(manA, manB)
                    ManifoldBindings.OPTYPE_DIFFERENCE -> mb.difference(manA, manB)
                    ManifoldBindings.OPTYPE_INTERSECTION -> mb.intersection(manA, manB)
                    else -> throw IllegalArgumentException("Unknown op: $opType")
                }
                markOwned(result)
                if (mb.isEmpty(result)) {
                    deleteTracked(mb, result)
                    emptyList()
                } else {
                    manifoldToPolygonsUnlocked(mb, result)
                }
            } finally {
                deleteTracked(mb, manA)
                deleteTracked(mb, manB)
            }
        }
    }

    private fun manifoldToPolygons(mb: ManifoldBindings, manifold: Long): List<Polygon> = synchronized(JNI_SYNC) {
        manifoldToPolygonsUnlocked(mb, manifold)
    }

    private fun manifoldToPolygonsUnlocked(mb: ManifoldBindings, manifold: Long): List<Polygon> {
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
            deleteTracked(mb, manifold)
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

    fun exportStl(nativeMesh: Long, file: File) = synchronized(JNI_SYNC) {
        getBindings().exportSTL(nativeMesh, file)
    }

    fun toVertexHolder(nativeMesh: Long, color: Color): NativeVertexHolder = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val data = mb.exportMeshGL64(nativeMesh)
        val verts = data.vertices()
        val tris = data.triangles()
        val triCount = data.triCount().toInt()
        val vertCount = triCount * 3

        val verticesArray = FloatArray(vertCount * 7)
        val normalsArray = FloatArray(vertCount * 3)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f

        var vi = 0
        var ni = 0
        for (i in 0 until triCount) {
            val i0 = tris[i * 3].toInt() * 3
            val i1 = tris[i * 3 + 1].toInt() * 3
            val i2 = tris[i * 3 + 2].toInt() * 3

            val ax = verts[i0].toFloat()
            val ay = verts[i0 + 1].toFloat()
            val az = verts[i0 + 2].toFloat()
            val bx = verts[i1].toFloat()
            val by = verts[i1 + 1].toFloat()
            val bz = verts[i1 + 2].toFloat()
            val cx = verts[i2].toFloat()
            val cy = verts[i2 + 1].toFloat()
            val cz = verts[i2 + 2].toFloat()

            val abx = bx - ax
            val aby = by - ay
            val abz = bz - az
            val acx = cx - ax
            val acy = cy - ay
            val acz = cz - az

            var nx = aby * acz - abz * acy
            var ny = abz * acx - abx * acz
            var nz = abx * acy - aby * acx

            val len = Math.sqrt((nx * nx + ny * ny + nz * nz).toDouble())
            if (len > 0.0) {
                nx /= len.toFloat()
                ny /= len.toFloat()
                nz /= len.toFloat()
            }

            val vertices = arrayOf(arrayOf(ax, ay, az), arrayOf(bx, by, bz), arrayOf(cx, cy, cz))
            for (v in vertices) {
                verticesArray[vi++] = v[0]
                verticesArray[vi++] = v[1]
                verticesArray[vi++] = v[2]
                verticesArray[vi++] = r
                verticesArray[vi++] = g
                verticesArray[vi++] = b
                verticesArray[vi++] = a

                normalsArray[ni++] = nx
                normalsArray[ni++] = ny
                normalsArray[ni++] = nz
            }
        }

        return NativeVertexHolder(verticesArray, normalsArray, vertCount)
    }
}

data class NativeVertexHolder(val vertex: FloatArray, val normals: FloatArray, val verticesCount: Int)

private data class Triangle(val a: V3d, val b: V3d, val c: V3d, val n: V3d)
