package eu.printingin3d.javascad.manifold

import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.utils.Color
import eu.printingin3d.javascad.vrl.Polygon
import java.io.*
import java.nio.*

object TestModel {

    @JvmStatic
    fun main(args: Array<String>) {
        val sphere = makeSphere(10.0, 32, 16)
        val cube = makeCube(10.0, 10.0, 10.0, 25.0, 0.0, 0.0)
        val cyl = makeCylinder(3.5, 30.0, 32)

        println("Sphere: ${sphere.size} polys")
        println("Cube: ${cube.size} polys")
        println("Cylinder: ${cyl.size} polys")

        val union = Manifold3dEngine.union(sphere, cube)
        println("Union (sphere+cube): ${union.size} polys")

        val result = Manifold3dEngine.difference(union, cyl)
        println("Difference (union-cyl): ${result.size} polys")

        val file = File("test.stl")
        writeStl(result, file)
        println("Exported ${file.absolutePath} (${file.length()} bytes)")
    }

    private fun makeSphere(radius: Double, slices: Int, stacks: Int): List<Polygon> {
        val polys = mutableListOf<Polygon>()
        for (i in 0 until stacks) {
            val theta1 = Math.PI * (i / stacks.toDouble())
            val theta2 = Math.PI * ((i + 1) / stacks.toDouble())
            val r1 = radius * Math.sin(theta1)
            val y1 = radius * Math.cos(theta1)
            val r2 = radius * Math.sin(theta2)
            val y2 = radius * Math.cos(theta2)

            val ring1 = mutableListOf<V3d>()
            val ring2 = mutableListOf<V3d>()
            for (j in 0 until slices) {
                val phi = 2.0 * Math.PI * (j / slices.toDouble())
                ring1.add(V3d(r1 * Math.cos(phi), y1, r1 * Math.sin(phi)))
                ring2.add(V3d(r2 * Math.cos(phi), y2, r2 * Math.sin(phi)))
            }

            if (r1 > 1e-10 && r2 > 1e-10) {
                for (j in 0 until slices) {
                    val nj = (j + 1) % slices
                    polys.add(Polygon.fromPolygons(ring1[j], ring1[nj], ring2[nj], Color.white))
                    polys.add(Polygon.fromPolygons(ring1[j], ring2[nj], ring2[j], Color.white))
                }
            }
        }
        return polys
    }

    private fun makeCube(w: Double, h: Double, d: Double, cx: Double, cy: Double, cz: Double): List<Polygon> {
        val hw = w / 2.0
        val hh = h / 2.0
        val hd = d / 2.0
        val faces = listOf(
            listOf(V3d(cx-hw, cy-hh, cz+hd), V3d(cx+hw, cy-hh, cz+hd), V3d(cx+hw, cy+hh, cz+hd), V3d(cx-hw, cy+hh, cz+hd)),
            listOf(V3d(cx+hw, cy-hh, cz-hd), V3d(cx-hw, cy-hh, cz-hd), V3d(cx-hw, cy+hh, cz-hd), V3d(cx+hw, cy+hh, cz-hd)),
            listOf(V3d(cx-hw, cy-hh, cz-hd), V3d(cx-hw, cy-hh, cz+hd), V3d(cx-hw, cy+hh, cz+hd), V3d(cx-hw, cy+hh, cz-hd)),
            listOf(V3d(cx+hw, cy-hh, cz+hd), V3d(cx+hw, cy-hh, cz-hd), V3d(cx+hw, cy+hh, cz-hd), V3d(cx+hw, cy+hh, cz+hd)),
            listOf(V3d(cx-hw, cy-hh, cz-hd), V3d(cx+hw, cy-hh, cz-hd), V3d(cx+hw, cy-hh, cz+hd), V3d(cx-hw, cy-hh, cz+hd)),
            listOf(V3d(cx-hw, cy+hh, cz+hd), V3d(cx+hw, cy+hh, cz+hd), V3d(cx+hw, cy+hh, cz-hd), V3d(cx-hw, cy+hh, cz-hd)),
        )
        return faces.map { Polygon.fromPolygons(it, Color.white) }
    }

    private fun makeCylinder(radius: Double, height: Double, segments: Int): List<Polygon> {
        val polys = mutableListOf<Polygon>()
        val halfH = height / 2.0
        val bottom = mutableListOf<V3d>()
        val top = mutableListOf<V3d>()

        for (i in 0 until segments) {
            val theta = 2.0 * Math.PI * (i / segments.toDouble())
            bottom.add(V3d(radius * Math.cos(theta), -halfH, radius * Math.sin(theta)))
            top.add(V3d(radius * Math.cos(theta), halfH, radius * Math.sin(theta)))
        }

        for (i in 0 until segments) {
            val ni = (i + 1) % segments
            polys.add(Polygon.fromPolygons(bottom[i], bottom[ni], top[ni], Color.white))
            polys.add(Polygon.fromPolygons(bottom[i], top[ni], top[i], Color.white))
        }

        for (i in 1 until segments - 1) {
            polys.add(Polygon.fromPolygons(bottom[0], bottom[i], bottom[i + 1], Color.white))
        }
        for (i in 1 until segments - 1) {
            polys.add(Polygon.fromPolygons(top[0], top[i + 1], top[i], Color.white))
        }

        return polys
    }

    private fun writeStl(polygons: List<Polygon>, file: File) {
        val tris = mutableListOf<Triangle>()
        for (poly in polygons) {
            val pts = poly.vertices
            val n = poly.normal
            if (pts.size == 3) {
                tris.add(Triangle(pts[0], pts[1], pts[2], n))
            } else if (pts.size > 3) {
                val v0 = pts[0]
                for (i in 1 until pts.size - 1) {
                    tris.add(Triangle(v0, pts[i], pts[i + 1], n))
                }
            }
        }

        FileOutputStream(file).channel.use { channel ->
            val bb = ByteBuffer.allocate(80 + 4 + tris.size * 50).order(ByteOrder.LITTLE_ENDIAN)
            bb.position(80)
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
