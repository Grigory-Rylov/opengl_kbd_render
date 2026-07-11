package com.github.grishberg.javascad

import com.github.grishberg.javascad.optimizator.PolygonValidatorMultithreading
import com.github.grishberg.javascad.optimizator.ProgressObserver
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.vrl.Facet
import eu.printingin3d.javascad.vrl.Polygon
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.WritableByteChannel

/** Key for shared vertex map — exact float32 matching (OpenSCAD Reindexer approach). */
data class FloatTriple(val x: Float, val y: Float, val z: Float)

object StlExporter {

    private const val X = 0
    private const val Y = 0
    private const val Z = 0

    fun saveStl(polygons: List<Polygon>, fileName: String) {
        saveStl(polygons, fileName, null)
    }

    fun saveStl(polygons: List<Polygon>, fileName: String, onProgress: ((String) -> Unit)?) {
        val file = File(fileName)
        val startTime = System.currentTimeMillis()

        val fixPolygons = PolygonValidatorMultithreading().fixPolygons(
            polygons, object : ProgressObserver {
                override fun onProgress(progress: Int) {
                    onProgress?.invoke("Fix polygons $progress%")
                }
            })

        onProgress?.invoke("Fix polygons done")

        onProgress?.invoke("Триангуляция...")

        // OpenSCAD approach: collect ALL vertices into a shared vertex array first,
        // so that adjacent polygons share the exact same vertex instances.
        // This eliminates floating-point gaps between neighboring facets.
        val sharedVertices: MutableMap<FloatTriple, V3d> = mutableMapOf()

        fun getSharedVertex(v: V3d): V3d {
            // Round to float32 precision first, then share.
            val key = FloatTriple(v.x.toFloat(), v.y.toFloat(), v.z.toFloat())
            return sharedVertices.getOrPut(key) { V3d(key.x.toDouble(), key.y.toDouble(), key.z.toDouble()) }
        }

        val facetsFromPolygons: MutableList<Facet> = ArrayList<Facet>()
        for (p in fixPolygons) {
            // Round polygon vertices to float32 precision BEFORE triangulation.
            // This ensures adjacent polygons share exactly the same vertices.
            val roundedVerts = p.getVertices().map { getSharedVertex(it) }
            val triangles = Triangulator.triangulate(roundedVerts, p.getNormal())
            for (t in triangles) {
                val shared = arrayOf(
                    getSharedVertex(t.getPoints()[0]),
                    getSharedVertex(t.getPoints()[1]),
                    getSharedVertex(t.getPoints()[2])
                )
                // Skip degenerate triangles (collapsed after sharing)
                if (shared[0] == shared[1] || shared[1] == shared[2] || shared[0] == shared[2]) continue
                val newT = Triangle3d(shared[0], shared[1], shared[2])
                facetsFromPolygons.add(Facet(newT, p.getNormal(), p.getColor()))
            }
        }

        println(
            "saveStl: " + fileName + " triangulation completed, takes " + (System.currentTimeMillis() - startTime) + " ms"
        )

        onProgress?.invoke("Валидация и репарация ${facetsFromPolygons.size} facets...")
        val validatedFacets = StlValidator.validateAndRepair(facetsFromPolygons) as MutableList<Facet>
        println("saveStl: after repair: ${validatedFacets.size} facets")

        try {
            FileOutputStream(fileName).getChannel().use { channel ->
                writeBinaryStl(validatedFacets, channel)
                println("Export to " + fileName + " is done.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun writeBinaryStl(
        facets: MutableList<Facet>, fileName: String
    ) {
        try {
            FileOutputStream(fileName).getChannel().use { channel ->
                writeBinaryStl(facets, channel)
                println("Export to " + fileName + " is done.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @Throws(IOException::class)
    fun writeBinaryStl(
        facets: MutableList<Facet>, channel: WritableByteChannel
    ) {
        // Заголовок файла (80 байт)

        val header = ByteArray(80)
        val buffer = ByteBuffer.allocate(84 + 50 * facets.size).order(ByteOrder.LITTLE_ENDIAN).put(header)

        // Количество треугольников (4 байта)
        buffer.putInt(facets.size)

        // Запись каждого треугольника
        for (facet in facets) {
            val normal = facet.getNormal()
            val triangle = facet.getTriangle()
            val points = triangle.getPoints()

            // Нормаль (3 float)
            buffer.putFloat(normal.getX().toFloat())
            buffer.putFloat(normal.getY().toFloat())
            buffer.putFloat(normal.getZ().toFloat())

            // Координаты вершин (3 точки по 3 float)
            for (point in points) {
                buffer.putFloat(point.getX().toFloat())
                buffer.putFloat(point.getY().toFloat())
                buffer.putFloat(point.getZ().toFloat())
            }

            // Атрибуты (2 байта)
            buffer.putShort(0.toShort())
        }

        buffer.flip()
        channel.write(buffer)
    }
}
