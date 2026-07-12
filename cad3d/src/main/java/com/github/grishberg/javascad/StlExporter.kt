package com.github.grishberg.javascad

import com.github.grishberg.csg.adapter.JscadAdapter
import com.github.grishberg.csg.export.StlExporter as NewStlExporter
import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3
import com.github.grishberg.javascad.optimizator.PolygonValidatorMultithreading
import com.github.grishberg.javascad.optimizator.ProgressObserver
import com.github.grishberg.openscad.vrl.CSG as JscadCSG
import com.github.grishberg.openscad.vrl.Facet
import com.github.grishberg.openscad.vrl.Polygon as JscadPolygon
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.WritableByteChannel

object StlExporter {

    // ---- Публичный API: новый движок ----

    fun saveStlNew(csg: JscadCSG, fileName: String, onProgress: ((String) -> Unit)? = null) {
        saveStlNew(csg.polygons, fileName, onProgress)
    }

    /**
     * Пайплайн: fixPolygons → JscadAdapter → writeBinaryStl → load + StlValidator.validateAndRepair
     *
     * StlValidator работает на float32 — там где STL реально живёт.
     */
    fun saveStlNew(polygons: List<JscadPolygon>, fileName: String, onProgress: ((String) -> Unit)? = null) {
        val startTime = System.currentTimeMillis()

        // Step 1: fixPolygons — исправляет общие рёбра (11607 → 52 open edges)
        onProgress?.invoke("Fix polygons...")
        val fixed = PolygonValidatorMultithreading().fixPolygons(
            polygons, object : ProgressObserver {
                override fun onProgress(progress: Int) {
                    onProgress?.invoke("Fix polygons $progress%")
                }
            })
        onProgress?.invoke("Fix polygons done: ${fixed.size} polygons")

        // Step 2: JscadAdapter → PolySet3 (shared vertices, exact double matching)
        onProgress?.invoke("Конвертация в PolySet3...")
        val polyset = JscadAdapter.polygonsToPolySet3(fixed)
        println("saveStl: PolySet3 -> ${polyset.indices.size} tris, ${polyset.vertices.size} verts")

        // Step 3: Write initial STL to temp file
        val tempFile = java.io.File(fileName).let { f ->
            java.io.File(f.parentFile, "${f.nameWithoutExtension}_raw${f.extension}")
        }
        onProgress?.invoke("Запись raw STL...")
        FileOutputStream(tempFile).channel.use { channel ->
            NewStlExporter.writeBinaryStl(polyset, channel)
        }

        // Step 4: Load as float32 Facets + StlValidator.validateAndRepair
        onProgress?.invoke("Репарация (float32)...")
        val rawFacets = StlValidator.loadStl(tempFile.absolutePath)
        val initialOpen = StlValidator.countOpenEdgesOrcaStyle(rawFacets)
        println("saveStl: raw float32 -> ${rawFacets.size} facets, open=$initialOpen")

        val repaired = StlValidator.validateAndRepair(rawFacets) as MutableList<Facet>
        val finalOpen = StlValidator.countOpenEdgesOrcaStyle(repaired)
        println("saveStl: repaired -> ${repaired.size} facets, open=$finalOpen")

        // Step 5: Write final STL
        onProgress?.invoke("Запись финального STL...")
        FileOutputStream(fileName).channel.use { channel ->
            writeBinaryStl(repaired, channel)
        }

        // Cleanup
        tempFile.delete()
        println("Export to $fileName done. (${System.currentTimeMillis() - startTime} ms total)")
    }

    // ---- Перенаправление старого API на новый ----

    fun saveStl(polygons: List<JscadPolygon>, fileName: String) {
        saveStl(polygons, fileName, null)
    }

    fun saveStl(polygons: List<JscadPolygon>, fileName: String, onProgress: ((String) -> Unit)?) {
        saveStlNew(polygons, fileName, onProgress)
    }

    // ---- Legacy: запись JSCAD Facets (для тестов) ----

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

    @Throws(IOException::class)
    fun writeBinaryStl(facets: MutableList<Facet>, channel: WritableByteChannel) {
        val header = ByteArray(80)
        val buffer = ByteBuffer.allocate(84 + 50 * facets.size).order(ByteOrder.LITTLE_ENDIAN).put(header)
        buffer.putInt(facets.size)

        for (facet in facets) {
            val normal = facet.normal
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
