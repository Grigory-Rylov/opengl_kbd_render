package com.github.grishberg.javascad

import com.github.grishberg.csg.adapter.JscadAdapter
import com.github.grishberg.csg.export.StlExporter as NewStlExporter
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3
import com.github.grishberg.javascad.optimizator.ProgressObserver
import com.github.grishberg.javascad.optimizator.PolygonValidatorMultithreading
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.CSG as JscadCSG
import eu.printingin3d.javascad.vrl.Facet
import eu.printingin3d.javascad.vrl.Polygon as JscadPolygon
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.WritableByteChannel

object StlExporter {

    // ---- Публичный API: новый движок (основной пайплайн) ----

    /** Экспорт JSCAD CSG через новый BSP движок. */
    fun saveStlNew(csg: JscadCSG, fileName: String, onProgress: ((String) -> Unit)? = null) {
        saveStlNew(csg.getPolygons(), fileName, onProgress)
    }

    /**
     * Экспорт JSCAD полигонов через новый движок.
     * Пайплайн: fixPolygons -> JscadAdapter(polySet3 with shared verts) -> writeBinaryStl
     */
    fun saveStlNew(polygons: List<JscadPolygon>, fileName: String, onProgress: ((String) -> Unit)? = null) {
        val startTime = System.currentTimeMillis()

        // Step 1: fixPolygons (исправляет общие рёбра между полигонами)
        onProgress?.invoke("Fix polygons...")
        val fixed = PolygonValidatorMultithreading().fixPolygons(
            polygons, object : ProgressObserver {
                override fun onProgress(progress: Int) {
                    onProgress?.invoke("Fix polygons $progress%")
                }
            })
        onProgress?.invoke("Fix polygons done: ${fixed.size} polygons")

        // Step 2: JscadAdapter -> PolySet3 (shared vertices, триангуляция)
        onProgress?.invoke("Конвертация в PolySet3...")
        val polyset = JscadAdapter.polygonsToPolySet3(fixed)
        println("saveStl: PolySet3 -> ${polyset.indices.size} tris, ${polyset.vertices.size} verts")

        // Step 3: Write binary STL directly (double precision from new engine)
        onProgress?.invoke("Запись STL...")
        try {
            FileOutputStream(fileName).channel.use { channel ->
                NewStlExporter.writeBinaryStl(polyset, channel)
                println("Export to $fileName is done. (${System.currentTimeMillis() - startTime} ms total)")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    // ---- Публичный API: перенаправляет на новый движок ----

    fun saveStl(polygons: List<JscadPolygon>, fileName: String) {
        saveStl(polygons, fileName, null)
    }

    fun saveStl(polygons: List<JscadPolygon>, fileName: String, onProgress: ((String) -> Unit)?) {
        saveStlNew(polygons, fileName, onProgress)
    }

    // ---- Legacy: прямая запись Facet -> STL (без репарации, для тестов) ----

    fun writeBinaryStl(facets: MutableList<Facet>, fileName: String) {
        try {
            FileOutputStream(fileName).channel.use { channel ->
                writeBinaryStl(facets, channel)
                println("Export to $fileName is done.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /** Write binary STL from Facet list — converts double -> float32. */
    @Throws(IOException::class)
    fun writeBinaryStl(facets: MutableList<Facet>, channel: WritableByteChannel) {
        val header = ByteArray(80)
        val buffer = ByteBuffer.allocate(84 + 50 * facets.size).order(ByteOrder.LITTLE_ENDIAN).put(header)
        buffer.putInt(facets.size)

        for (facet in facets) {
            val normal = facet.getNormal()
            val points = facet.getTriangle().getPoints()

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
