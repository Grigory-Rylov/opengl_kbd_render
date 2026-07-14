package com.github.grishberg.javascad

import com.github.grishberg.javascad.optimizator.PolygonValidatorMultithreading
import com.github.grishberg.javascad.optimizator.ProgressObserver
import eu.printingin3d.javascad.coords.Triangle3d
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.Facet
import eu.printingin3d.javascad.vrl.Polygon
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.WritableByteChannel

object StlExporter {

    private const val DEGENERATE_AREA_THRESHOLD = 1e-10

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
        val rawFacets: MutableList<RawFacet> = ArrayList<RawFacet>()
        for (p in fixPolygons) {
            val verts = p.getVertices()
            if (verts.size < 3) continue
            val triangles = Triangulator.triangulate(verts, p.getNormal())
            for (t in triangles) {
                rawFacets.add(RawFacet(t.getPoints()[0], t.getPoints()[1], t.getPoints()[2], p.getNormal(), p.getColor()))
            }
        }
        println("saveStl: " + fileName + " triangulation took " + (System.currentTimeMillis() - triangulationStartTime) + " ms, raw facets=${rawFacets.size}")

        val canonStartTime = System.currentTimeMillis()
        val globalCanon = HashMap<String, V3d>()
        var degenerateRemoved = 0
        val canonizedFacets: MutableList<Facet> = ArrayList<Facet>()
        for (rf in rawFacets) {
            val c0 = canonize(globalCanon, rf.a.roundedToEpsilon())
            val c1 = canonize(globalCanon, rf.b.roundedToEpsilon())
            val c2 = canonize(globalCanon, rf.c.roundedToEpsilon())
            if (isDegenerateTriangle(c0, c1, c2)) {
                degenerateRemoved++
                continue
            }
            canonizedFacets.add(Facet(Triangle3d(c0, c1, c2), rf.normal, rf.color))
        }
        println("saveStl: " + fileName + " global canonization took " + (System.currentTimeMillis() - canonStartTime) + " ms, unique vertices=${globalCanon.size}, degenerate removed=$degenerateRemoved")

        val dedupStartTime = System.currentTimeMillis()
        val dedupedFacets = deduplicateFacets(canonizedFacets)
        println("saveStl: " + fileName + " facet dedup took " + (System.currentTimeMillis() - dedupStartTime) + " ms, before=${canonizedFacets.size}, after=${dedupedFacets.size}")

        val nonManifoldCount = StlValidator.countNonManifoldEdges(dedupedFacets)
        println("saveStl: ${fileName} facets=${dedupedFacets.size}, non-manifold edges=$nonManifoldCount")

        try {
            FileOutputStream(fileName).getChannel().use { channel ->
                writeBinaryStl(dedupedFacets, channel)
                println("Export to " + fileName + " is done.")
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private data class RawFacet(val a: V3d, val b: V3d, val c: V3d, val normal: V3d, val color: Color)

    private fun canonize(global: HashMap<String, V3d>, v: V3d): V3d {
        val key = canonKey(v)
        return global.computeIfAbsent(key) { v }
    }

    private fun canonKey(v: V3d): String {
        return "${v.x}/${v.y}/${v.z}"
    }

    private fun isDegenerateTriangle(a: V3d, b: V3d, c: V3d): Boolean {
        val ab = b.subtract(a)
        val ac = c.subtract(a)
        val cross = ab.cross(ac)
        return (cross.magnitude() * cross.magnitude()) < DEGENERATE_AREA_THRESHOLD
    }

    private fun deduplicateFacets(facets: List<Facet>): MutableList<Facet> {
        val seen = HashSet<String>()
        val result = ArrayList<Facet>()
        for (f in facets) {
            val pts = f.getTriangle().getPoints()
            val sorted = listOf(pts[0], pts[1], pts[2]).sortedWith(compareBy({ it.x }, { it.y }, { it.z }))
            val key = "${sorted[0].x}/${sorted[0].y}/${sorted[0].z}|${sorted[1].x}/${sorted[1].y}/${sorted[1].z}|${sorted[2].x}/${sorted[2].y}/${sorted[2].z}"
            if (seen.add(key)) {
                result.add(f)
            }
        }
        return result
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
