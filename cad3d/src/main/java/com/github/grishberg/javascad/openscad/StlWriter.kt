package com.github.grishberg.javascad.openscad

import eu.printingin3d.javascad.coords.V3d
import java.io.Writer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.WritableByteChannel

object StlWriter {

    fun writeBinary(polySet: PolySetGeometry, channel: WritableByteChannel) {
        val count = polySet.indices.size
        val recordSize = 50
        val headerSize = 84
        val buffer = ByteBuffer.allocate(headerSize + count * recordSize)
            .order(ByteOrder.LITTLE_ENDIAN)

        val header = ByteArray(80)
        val headerStr = "OpenSCAD Model"
        headerStr.toByteArray().copyInto(header, 0, 0, minOf(headerStr.length, 80))
        buffer.put(header)
        buffer.putInt(count)

        for (tri in polySet.indices) {
            val p0 = polySet.vertices[tri.v0]
            val p1 = polySet.vertices[tri.v1]
            val p2 = polySet.vertices[tri.v2]
            val normal = computeNormal(p0, p1, p2)

            buffer.putFloat(normal.x.toFloat())
            buffer.putFloat(normal.y.toFloat())
            buffer.putFloat(normal.z.toFloat())

            buffer.putFloat(p0.x.toFloat())
            buffer.putFloat(p0.y.toFloat())
            buffer.putFloat(p0.z.toFloat())

            buffer.putFloat(p1.x.toFloat())
            buffer.putFloat(p1.y.toFloat())
            buffer.putFloat(p1.z.toFloat())

            buffer.putFloat(p2.x.toFloat())
            buffer.putFloat(p2.y.toFloat())
            buffer.putFloat(p2.z.toFloat())

            buffer.putShort(0)
        }

        buffer.flip()
        channel.write(buffer)
    }

    fun writeAscii(polySet: PolySetGeometry, writer: Writer) {
        writer.write("solid OpenSCAD_Model\n")
        for (tri in polySet.indices) {
            val p0 = polySet.vertices[tri.v0]
            val p1 = polySet.vertices[tri.v1]
            val p2 = polySet.vertices[tri.v2]
            val normal = computeNormal(p0, p1, p2)

            writer.write("  facet normal ${formatVec(normal)}\n")
            writer.write("    outer loop\n")
            writer.write("      vertex ${formatVec(p0)}\n")
            writer.write("      vertex ${formatVec(p1)}\n")
            writer.write("      vertex ${formatVec(p2)}\n")
            writer.write("    endloop\n")
            writer.write("  endfacet\n")
        }
        writer.write("endsolid OpenSCAD_Model\n")
    }

    fun computeNormal(p0: V3d, p1: V3d, p2: V3d): V3d {
        val v1 = p1.subtract(p0)
        val v2 = p2.subtract(p0)
        val cross = v1.cross(v2)
        val mag = cross.magnitude()
        return if (mag > 1e-12) {
            cross.scale(1.0 / mag)
        } else {
            V3d(0.0, 0.0, 1.0)
        }
    }

    private fun formatVec(v: V3d): String {
        return "${formatDouble(v.x)} ${formatDouble(v.y)} ${formatDouble(v.z)}"
    }

    private fun formatDouble(d: Double): String {
        val s = d.toFloat().toString()
        return if (s.contains('.')) s else "$s.0"
    }
}
