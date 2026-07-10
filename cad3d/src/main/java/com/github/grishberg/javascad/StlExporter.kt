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

object StlExporter {

    private const val X = 0
    private const val Y = 0
    private const val Z = 0

    fun saveStl(polygons: List<Polygon>, fileName: String) {
        println(
            "saveStl: Start generating polygons from: " + polygons.size + " " + fileName
        )

        val file = File(fileName)
        val startTime = System.currentTimeMillis()

        println(
            "saveStl to bin: " + fileName + " fix polygons completed, takes " + (System.currentTimeMillis() - startTime) + " ms"
        )

        val fixPolygons = PolygonValidatorMultithreading().fixPolygons(
            polygons, object : ProgressObserver {
                override fun onProgress(progress: Int) {
                    println(file.getName() + " : progress = " + progress)
                }
            })

        println(
            "saveStl: " + fileName + " fix polygons completed, takes " + (System.currentTimeMillis() - startTime) + " ms"
        )

        val triangulationStartTime = System.currentTimeMillis()
        val facetsFromPolygons: MutableList<Facet> = ArrayList<Facet>()
        for (p in fixPolygons) {
            val triangles = Triangulator.triangulate(p.getVertices(), p.getNormal())
            for (t in triangles) {
                val rounded = ArrayList<V3d>()
                for (trianglePoint in t.getPoints()) {
                    rounded.add(trianglePoint.roundedToEpsilon())
                }
                //facetsFromPolygons.add(Facet(t, p.getNormal(), p.getColor()))
                val newT = Triangle3d(rounded.get(0), rounded.get(1), rounded.get(2))
                facetsFromPolygons.add(Facet(newT, p.getNormal(), p.getColor()));
            }
        }

        println(
            "saveStl: " + fileName + " triangulation completed, takes " + (System.currentTimeMillis() - triangulationStartTime) + " ms"
        )

        println("saveStl: validating and repairing ${facetsFromPolygons.size} facets...")
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
