package com.github.grishberg.csg.export

import com.github.grishberg.csg.geom.PolySet3
import com.github.grishberg.csg.geom.Vec3
import java.io.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.WritableByteChannel

/**
 * STL exporter following OpenSCAD's export_stl.cc format exactly.
 * Supports both binary and ASCII STL.
 */
object StlExporter {

    /**
     * Export PolySet3 to binary STL file.
     */
    @Throws(IOException::class)
    fun exportBinary(mesh: PolySet3, filename: String) {
        FileOutputStream(filename).channel.use { channel ->
            writeBinaryStl(mesh, channel)
        }
    }

    /**
     * Export PolySet3 to ASCII STL file.
     */
    @Throws(IOException::class)
    fun exportAscii(mesh: PolySet3, filename: String) {
        BufferedWriter(FileWriter(filename)).use { bw ->
            bw.write("solid CSG_Model\n")

            for (tri in mesh.indices) {
                val p0 = mesh.vertices[tri.a]
                val p1 = mesh.vertices[tri.b]
                val p2 = mesh.vertices[tri.c]
                val normal = computeNormal(p0, p1, p2)

                bw.write(String.format("  facet normal %.17g %.17g %.17g\n",
                    normal.x, normal.y, normal.z))
                bw.write("    outer loop\n")
                bw.write(String.format("      vertex %.17g %.17g %.17g\n",
                    p0.x, p0.y, p0.z))
                bw.write(String.format("      vertex %.17g %.17g %.17g\n",
                    p1.x, p1.y, p1.z))
                bw.write(String.format("      vertex %.17g %.17g %.17g\n",
                    p2.x, p2.y, p2.z))
                bw.write("    endloop\n")
                bw.write("  endfacet\n")
            }

            bw.write("endsolid CSG_Model\n")
        }
    }

    /**
     * Write binary STL to a channel.
     * Format: 80-byte header + 4-byte LE triangle count + 50 bytes per triangle.
     */
    @Throws(IOException::class)
    fun writeBinaryStl(mesh: PolySet3, channel: WritableByteChannel) {
        // Header: 80 bytes + 4 bytes triangle count
        val header = ByteBuffer.allocate(84).order(ByteOrder.LITTLE_ENDIAN)
        val headerStr = "CSG Engine Model".toByteArray(Charsets.US_ASCII)
        System.arraycopy(headerStr, 0, header.array(), 0, headerStr.size)
        header.putInt(mesh.indices.size)

        val triBuf = ByteBuffer.allocate(50).order(ByteOrder.LITTLE_ENDIAN)

        val buffers = arrayOfNulls<java.nio.ByteBuffer>(mesh.indices.size + 1)
        buffers[0] = header

        var bufIdx = 1
        for (tri in mesh.indices) {
            val p0 = mesh.vertices[tri.a]
            val p1 = mesh.vertices[tri.b]
            val p2 = mesh.vertices[tri.c]
            val normal = computeNormal(p0, p1, p2)

            triBuf.clear()
            triBuf.putFloat(normal.x.toFloat())
            triBuf.putFloat(normal.y.toFloat())
            triBuf.putFloat(normal.z.toFloat())
            triBuf.putFloat(p0.x.toFloat())
            triBuf.putFloat(p0.y.toFloat())
            triBuf.putFloat(p0.z.toFloat())
            triBuf.putFloat(p1.x.toFloat())
            triBuf.putFloat(p1.y.toFloat())
            triBuf.putFloat(p1.z.toFloat())
            triBuf.putFloat(p2.x.toFloat())
            triBuf.putFloat(p2.y.toFloat())
            triBuf.putFloat(p2.z.toFloat())
            triBuf.putShort(0.toShort())
            triBuf.flip()

            buffers[bufIdx++] = triBuf.duplicate()
        }

        for (buf in buffers) {
            buf?.let { channel.write(it) }
        }
    }

    /** Compute normalized face normal from 3 vertices. */
    private fun computeNormal(p0: Vec3, p1: Vec3, p2: Vec3): Vec3 {
        val ax = p1.x - p0.x
        val ay = p1.y - p0.y
        val az = p1.z - p0.z
        val bx = p2.x - p0.x
        val by = p2.y - p0.y
        val bz = p2.z - p0.z

        var nx = ay * bz - az * by
        var ny = az * bx - ax * bz
        var nz = ax * by - ay * bx

        val len = Math.sqrt(nx * nx + ny * ny + nz * nz)
        if (len > 0) {
            nx /= len
            ny /= len
            nz /= len
        }

        return Vec3(nx, ny, nz)
    }
}
