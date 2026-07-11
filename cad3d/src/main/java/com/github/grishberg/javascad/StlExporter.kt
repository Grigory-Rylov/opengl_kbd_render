package com.github.grishberg.javascad

import com.github.grishberg.cad3d.plugin.StlExportListener
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

    fun saveStl(
        polygons: List<Polygon>,
        fileName: String,
        autoRepair: Boolean = false,
        progressListener: StlExportListener? = null
    ) {
        println(
            "saveStl: Start generating polygons from: " + polygons.size + " " + fileName
        )

        val file = File(fileName)
        val startTime = System.currentTimeMillis()

        progressListener?.onExportProgress(fileName, 0)

        val fixPolygons = PolygonValidatorMultithreading().fixPolygons(
            polygons, object : ProgressObserver {
                override fun onProgress(progress: Int) {
                    println(file.getName() + " : progress = " + progress)
                    progressListener?.onExportProgress(fileName, (progress * 0.5).toInt())
                }
            })

        println(
            "saveStl: " + fileName + " fix polygons completed, takes " + (System.currentTimeMillis() - startTime) + " ms"
        )

        progressListener?.onExportProgress(fileName, 50)

        val triangulationStartTime = System.currentTimeMillis()
        val facetsFromPolygons: MutableList<Facet> = ArrayList()
        val fixPolygonsSize = fixPolygons.size
        for ((idx, p) in fixPolygons.withIndex()) {
            val triangles = Triangulator.triangulate(p.getVertices(), p.getNormal())
            for (t in triangles) {
                val rounded = ArrayList<V3d>()
                for (trianglePoint in t.getPoints()) {
                    rounded.add(trianglePoint.roundedToEpsilon())
                }
                val newT = Triangle3d(rounded[0], rounded[1], rounded[2])
                facetsFromPolygons.add(Facet(newT, p.getNormal(), p.getColor()))
            }
            if (fixPolygonsSize > 0) {
                val pct = 50 + ((idx + 1) * 30 / fixPolygonsSize)
                progressListener?.onExportProgress(fileName, pct)
            }
        }

        println(
            "saveStl: " + fileName + " triangulation completed, takes " + (System.currentTimeMillis() - triangulationStartTime) + " ms"
        )

        progressListener?.onExportProgress(fileName, 80)

        if (autoRepair) {
            val repairStartTime = System.currentTimeMillis()
            val facetsAsPolygons = facetsFromPolygons.map { f ->
                Polygon.fromPolygons(f.getTriangle().getPoints(), f.getNormal(), f.getColor())
            }
            val validationResult = StlValidator().validate(facetsAsPolygons)
            if (!validationResult.isManifold) {
                println(
                    "saveStl: non-manifold mesh detected — " +
                        "${validationResult.openEdges} open edges, " +
                        "${validationResult.degenerateTriangles} degenerate"
                )
                val (repairedPolygons, repairResult) = StlRepairer().repair(facetsAsPolygons)
                println("saveStl: repair result — $repairResult")

                facetsFromPolygons.clear()
                for (p in repairedPolygons) {
                    val triangles = Triangulator.triangulate(p.getVertices(), p.getNormal())
                    for (t in triangles) {
                        val rounded = ArrayList<V3d>()
                        for (point in t.getPoints()) {
                            rounded.add(point.roundedToEpsilon())
                        }
                        val newT = Triangle3d(rounded[0], rounded[1], rounded[2])
                        facetsFromPolygons.add(Facet(newT, p.getNormal(), p.getColor()))
                    }
                }
                println(
                    "saveStl: repair completed, takes " +
                        (System.currentTimeMillis() - repairStartTime) + " ms"
                )
            } else {
                println("saveStl: mesh is already manifold, no repair needed")
            }
            progressListener?.onExportProgress(fileName, 95)
        }

        try {
            FileOutputStream(fileName).getChannel().use { channel ->
                writeBinaryStl(facetsFromPolygons, channel)
                println("Export to " + fileName + " is done.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        progressListener?.onExportProgress(fileName, 100)
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
