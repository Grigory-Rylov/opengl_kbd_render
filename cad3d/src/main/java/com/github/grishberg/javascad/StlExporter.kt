package com.github.grishberg.javascad

import com.github.grishberg.javascad.optimizator.PolygonValidatorMultithreading
import com.github.grishberg.javascad.optimizator.ProgressObserver
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.manifold.Manifold3dEngine
import eu.printingin3d.javascad.vrl.Facet
import eu.printingin3d.javascad.vrl.Polygon
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.WritableByteChannel

/** Key for shared vertex map — EXACT double precision matching. */
data class DoubleTriple(val x: Double, val y: Double, val z: Double)

object StlExporter {

    private const val X = 0
    private const val Y = 0
    private const val Z = 0

    fun saveStl(nativeMesh: Long, fileName: String) {
        val polygons = Manifold3dEngine.manifoldToPolygonsExport(nativeMesh)
        saveStl(polygons, fileName)
    }

    fun saveStl(polygons: List<Polygon>, fileName: String) {
        saveStl(polygons, fileName, null)
    }

    fun saveStl(polygons: List<Polygon>, fileName: String, onProgress: ((String) -> Unit)?) {
        val startTime = System.currentTimeMillis()

        // Step 1: fixPolygons (double precision)
        val fixedPolygons = PolygonValidatorMultithreading().fixPolygons(
            polygons, object : ProgressObserver {
                override fun onProgress(progress: Int) {
                    onProgress?.invoke("Fix polygons $progress%")
                }
            })
        onProgress?.invoke("Fix polygons done")

        // Step 2: Triangulation with SHARED double-precision vertices
        // CRITICAL: keep everything in double precision until STL write.
        // This eliminates floating-point gaps between neighboring facets.
        onProgress?.invoke("Триангуляция...")
        val sharedVertices: MutableMap<DoubleTriple, V3d> = mutableMapOf()

        fun getSharedVertex(v: V3d): V3d {
            val key = DoubleTriple(v.x, v.y, v.z)
            return sharedVertices.getOrPut(key) { v }
        }

        val facetsFromPolygons: MutableList<Facet> = ArrayList()
        for (p in fixedPolygons) {
            // Share vertices at EXACT double precision BEFORE triangulation
            val sharedVerts = p.getVertices().map { getSharedVertex(it) }
            val triangles = Triangulator.triangulate(sharedVerts, p.getNormal())
            for (t in triangles) {
                val pts = t.getPoints()
                val s0 = getSharedVertex(pts[0])
                val s1 = getSharedVertex(pts[1])
                val s2 = getSharedVertex(pts[2])
                if (s0 == s1 || s1 == s2 || s0 == s2) continue
                facetsFromPolygons.add(Facet(Triangle3d(s0, s1, s2), p.getNormal(), p.getColor()))
            }
        }

        println("saveStl: ${fileName} triangulation completed, ${facetsFromPolygons.size} facets, ${sharedVertices.size} unique vertices, takes ${System.currentTimeMillis() - startTime} ms")

        // Step 3: Validate and repair (double precision)
        onProgress?.invoke("Валидация и репарация ${facetsFromPolygons.size} facets...")
        val validatedFacets = StlValidator.validateAndRepair(facetsFromPolygons) as MutableList<Facet>
        println("saveStl: after repair: ${validatedFacets.size} facets")

        // Step 4: Write binary STL (ONLY HERE we convert to float32)
        try {
            FileOutputStream(fileName).getChannel().use { channel ->
                writeBinaryStl(validatedFacets, channel)
                println("Export to $fileName is done.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun writeBinaryStl(facets: MutableList<Facet>, fileName: String) {
        try {
            FileOutputStream(fileName).getChannel().use { channel ->
                writeBinaryStl(facets, channel)
                println("Export to $fileName is done.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /** Write binary STL — converts double -> float32 ONLY at this final step */
    @Throws(IOException::class)
    fun writeBinaryStl(facets: MutableList<Facet>, channel: WritableByteChannel) {
        val header = ByteArray(80)
        val buffer = ByteBuffer.allocate(84 + 50 * facets.size).order(ByteOrder.LITTLE_ENDIAN).put(header)
        buffer.putInt(facets.size)

        for (facet in facets) {
            val normal = facet.getNormal()
            val points = facet.getTriangle().getPoints()

            // Convert to float32 for STL format (OpenSCAD does the same at export time)
            buffer.putFloat(normal.getX().toFloat())
            buffer.putFloat(normal.getY().toFloat())
            buffer.putFloat(normal.getZ().toFloat())
            for (point in points) {
                buffer.putFloat(point.getX().toFloat())
                buffer.putFloat(point.getY().toFloat())
                buffer.putFloat(point.getZ().toFloat())
            }
            buffer.putShort(0.toShort())
        }

        buffer.flip()
        channel.write(buffer)
    }
}
