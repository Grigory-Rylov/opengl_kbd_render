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

    fun hullNative(handles: LongArray): Long {
        if (handles.isEmpty()) return empty()
        if (handles.size == 1) {
            return copyNative(handles[0])
        }
        val mb = getBindings()
        return try {
            mb.batchHull(handles)
        } catch (e: Exception) {
            println("  [hullNative] error: ${e.message}")
            empty()
        }
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

    // ---- Native transform operations ----

    fun translate(manifold: Long, tx: Double, ty: Double, tz: Double): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.translate(manifold, tx, ty, tz)
    }

    fun rotate(manifold: Long, rx: Double, ry: Double, rz: Double): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.rotate(manifold, rx, ry, rz)
    }

    fun scale(manifold: Long, sx: Double, sy: Double, sz: Double): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.scale(manifold, sx, sy, sz)
    }

    fun transform(manifold: Long, m: DoubleArray): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.transform(manifold,
            m[0], m[1], m[2], m[3],
            m[4], m[5], m[6], m[7],
            m[8], m[9], m[10], m[11])
    }

    fun transformAndReturn(manifold: Long, tx: Double, ty: Double, tz: Double): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.transform(manifold,
            1.0, 0.0, 0.0, tx,
            0.0, 1.0, 0.0, ty,
            0.0, 0.0, 1.0, tz)
    }

    fun empty(): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.empty()
    }

    fun isEmpty(manifold: Long): Boolean = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.isEmpty(manifold)
    }

    fun copyNative(manifold: Long): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.copy(manifold)
    }

    fun delete(manifold: Long) = synchronized(JNI_SYNC) {
        if (manifold == 0L) return
        try {
            val mb = getBindings()
            mb.delete(manifold)
        } catch (e: Exception) {
            println("  [Manifold3dEngine.delete] suppressed error for handle $manifold: ${e.message}")
        }
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

    fun manifoldToPolygonsExport(manifold: Long): List<Polygon> = synchronized(JNI_SYNC) {
        val mb = getBindings()
        val data = mb.exportMeshGL64(manifold)
        val verts = data.vertices()
        val tris = data.triangles()
        val triCount = data.triCount().toInt()
        val vertCount = verts.size / 3
        println("  [export] triCount=$triCount, vertCount=$vertCount")
        if (triCount == 0) return emptyList()

        var maxIdx = 0L
        var minIdx = Long.MAX_VALUE
        for (i in 0 until triCount * 3) {
            maxIdx = maxOf(maxIdx, tris[i])
            minIdx = minOf(minIdx, tris[i])
        }
        println("  [export] tri indices range: [$minIdx, $maxIdx], verts=$vertCount")

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

    fun operateNative(a: Long, b: Long, opType: Int): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        try {
            when (opType) {
                ManifoldBindings.OPTYPE_UNION -> mb.union(a, b)
                ManifoldBindings.OPTYPE_DIFFERENCE -> mb.difference(a, b)
                ManifoldBindings.OPTYPE_INTERSECTION -> mb.intersection(a, b)
                else -> throw IllegalArgumentException("Unknown op: $opType")
            }
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: Exception) {
            throw RuntimeException("Native CSG operation failed (op=$opType): ${e.message}", e)
        }
    }

    fun nativeHull(handles: LongArray): Long = synchronized(JNI_SYNC) {
        val mb = getBindings()
        mb.batchHull(handles)
    }

    fun nativeDelete(handle: Long) = synchronized(JNI_SYNC) {
        if (handle == 0L) return
        try {
            val mb = getBindings()
            mb.delete(handle)
        } catch (e: Exception) {
            println("  [Manifold3dEngine.nativeDelete] suppressed error for handle $handle: ${e.message}")
        }
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
                if (mb.isEmpty(result)) emptyList() else manifoldToPolygonsUnlocked(mb, result)
            } finally {
                mb.delete(manA)
                mb.delete(manB)
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

    fun exportStl(nativeMesh: Long, file: File) {
        getBindings().exportSTL(nativeMesh, file)
    }

    fun toVertexHolder(nativeMesh: Long, color: Color): NativeVertexHolder {
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
