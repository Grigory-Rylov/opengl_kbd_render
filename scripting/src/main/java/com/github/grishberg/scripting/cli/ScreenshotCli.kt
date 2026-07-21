package com.github.grishberg.scripting.cli

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.imageio.ImageIO
import kotlin.math.*

fun main(args: Array<String>) {
    if (args.size < 2) {
        println("Usage: ScreenshotCli <stl-file> <output-png> [width] [height]")
        return
    }

    val stlFile = File(args[0])
    val pngFile = File(args[1])
    val width = if (args.size > 2) args[2].toInt() else 1024
    val height = if (args.size > 3) args[3].toInt() else 768

    println("Loading STL: ${stlFile.absolutePath}")
    val triangles = readStl(stlFile)
    println("Triangles: ${triangles.size}")

    println("Rendering ${width}x${height}...")
    val img = render(triangles, width, height)

    println("Saving: ${pngFile.absolutePath}")
    ImageIO.write(img, "PNG", pngFile)
    println("Done!")
}

data class Triangle(
    val v0x: Float, val v0y: Float, val v0z: Float,
    val v1x: Float, val v1y: Float, val v1z: Float,
    val v2x: Float, val v2y: Float, val v2z: Float,
    val nx: Float, val ny: Float, val nz: Float,
    val r: Int, val g: Int, val b: Int
)

fun readStl(file: File): List<Triangle> {
    val triangles = mutableListOf<Triangle>()
    file.inputStream().use { stream ->
        val header = ByteArray(80)
        stream.read(header)
        val triCount = readInt(stream)
        repeat(triCount) {
            val nx = readFloat(stream); val ny = readFloat(stream); val nz = readFloat(stream)
            val v0x = readFloat(stream); val v0y = readFloat(stream); val v0z = readFloat(stream)
            val v1x = readFloat(stream); val v1y = readFloat(stream); val v1z = readFloat(stream)
            val v2x = readFloat(stream); val v2y = readFloat(stream); val v2z = readFloat(stream)
            val attributeByteCount = readShort(stream)
            triangles.add(Triangle(v0x, v0y, v0z, v1x, v1y, v1z, v2x, v2y, v2z, nx, ny, nz, 180, 180, 180))
        }
    }
    return triangles
}

fun readFloat(stream: java.io.InputStream): Float {
    val buf = ByteArray(4)
    stream.read(buf)
    return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).float
}

fun readInt(stream: java.io.InputStream): Int {
    val buf = ByteArray(4)
    stream.read(buf)
    return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).int
}

fun readShort(stream: java.io.InputStream): Short {
    val buf = ByteArray(2)
    stream.read(buf)
    return ByteBuffer.wrap(buf).order(ByteOrder.LITTLE_ENDIAN).short
}

data class ProjTri(val z: Float, val sx0: Int, val sy0: Int, val sx1: Int, val sy1: Int, val sx2: Int, val sy2: Int, val r: Int, val g: Int, val b: Int)

fun render(triangles: List<Triangle>, width: Int, height: Int): java.awt.image.BufferedImage {
    val img = java.awt.image.BufferedImage(width, height, java.awt.image.BufferedImage.TYPE_INT_RGB)

    // Camera setup
    val eyeX = 80.0f; val eyeY = 60.0f; val eyeZ = 120.0f
    val lookX = 0.0f; val lookY = 0.0f; val lookZ = 0.0f
    val upX = 0.0f; val upY = 1.0f; val upZ = 0.0f

    // Camera basis
    val fwdLen = sqrt((lookX - eyeX) * (lookX - eyeX) + (lookY - eyeY) * (lookY - eyeY) + (lookZ - eyeZ) * (lookZ - eyeZ)).toFloat()
    val fx = (lookX - eyeX) / fwdLen; val fy = (lookY - eyeY) / fwdLen; val fz = (lookZ - eyeZ) / fwdLen
    val rLen = sqrt((fy * upZ - fz * upY) * (fy * upZ - fz * upY) + (fz * upX - fx * upZ) * (fz * upX - fx * upZ) + (fx * upY - fy * upX) * (fx * upY - fy * upX))
    val rx = (fy * upZ - fz * upY) / rLen; val ry = (fz * upX - fx * upZ) / rLen; val rz = (fx * upY - fy * upX) / rLen
    val ux = fx * ry - fy * rx; val uy = fy * rz - fz * ry; val uz = fz * rx - fx * rz

    // Projection
    val fov = 50.0
    val focal = height / (2 * tan(fov * 0.5 * PI / 180))

    // Transform triangles to view space and project
    val projected = mutableListOf<ProjTri>()
    for (tri in triangles) {
        // View space
        val v0xv = (tri.v0x - eyeX) * rx + (tri.v0y - eyeY) * ry + (tri.v0z - eyeZ) * rz
        val v0yv = (tri.v0x - eyeX) * ux + (tri.v0y - eyeY) * uy + (tri.v0z - eyeZ) * uz
        val v0zv = (tri.v0x - eyeX) * fx + (tri.v0y - eyeY) * fy + (tri.v0z - eyeZ) * fz

        val v1xv = (tri.v1x - eyeX) * rx + (tri.v1y - eyeY) * ry + (tri.v1z - eyeZ) * rz
        val v1yv = (tri.v1x - eyeX) * ux + (tri.v1y - eyeY) * uy + (tri.v1z - eyeZ) * uz
        val v1zv = (tri.v1x - eyeX) * fx + (tri.v1y - eyeY) * fy + (tri.v1z - eyeZ) * fz

        val v2xv = (tri.v2x - eyeX) * rx + (tri.v2y - eyeY) * ry + (tri.v2z - eyeZ) * rz
        val v2yv = (tri.v2x - eyeX) * ux + (tri.v2y - eyeY) * uy + (tri.v2z - eyeZ) * uz
        val v2zv = (tri.v2x - eyeX) * fx + (tri.v2y - eyeY) * fy + (tri.v2z - eyeZ) * fz

        // Project (only if in front of camera)
        if (v0zv < 1 || v1zv < 1 || v2zv < 1) continue

        // Backface culling
        val edge1x = v1xv - v0xv; val edge1y = v1yv - v0yv
        val edge2x = v2xv - v0xv; val edge2y = v2yv - v0yv
        if (edge1x * edge2y - edge2x * edge1y <= 0) continue

        // Project to screen
        val sx0 = (v0xv * focal / v0zv).toInt() + width / 2
        val sy0 = (-v0yv * focal / v0zv).toInt() + height / 2
        val sx1 = (v1xv * focal / v1zv).toInt() + width / 2
        val sy1 = (-v1yv * focal / v1zv).toInt() + height / 2
        val sx2 = (v2xv * focal / v2zv).toInt() + width / 2
        val sy2 = (-v2yv * focal / v2zv).toInt() + height / 2

        // Lighting
        val ndotl = maxOf(0f, tri.nx * fx + tri.ny * fy + tri.nz * fz)
        val ambient = 0.3f
        val light = (ambient + (1 - ambient) * ndotl)
        val r = (tri.r * light).toInt().coerceIn(0, 255)
        val g = (tri.g * light).toInt().coerceIn(0, 255)
        val b = (tri.b * light).toInt().coerceIn(0, 255)

        val avgZ = (v0zv + v1zv + v2zv) / 3
        projected.add(ProjTri(avgZ, sx0, sy0, sx1, sy1, sx2, sy2, r, g, b))
    }

    // Sort back to front
    projected.sortByDescending { it.z }

    // Paint
    for (p in projected) {
        fillTriangle(img, p.sx0, p.sy0, p.sx1, p.sy1, p.sx2, p.sy2, p.r, p.g, p.b)
    }

    return img
}

fun fillTriangle(img: java.awt.image.BufferedImage, x0: Int, y0: Int, x1: Int, y1: Int, x2: Int, y2: Int, r: Int, g: Int, b: Int) {
    val minX = minOf(x0, x1, x2).coerceAtLeast(0)
    val maxX = maxOf(x0, x1, x2).coerceAtMost(img.width - 1)
    val minY = minOf(y0, y1, y2).coerceAtLeast(0)
    val maxY = maxOf(y0, y1, y2).coerceAtMost(img.height - 1)

    val det = (y1 - y2) * (x0 - x2) - (x1 - x2) * (y0 - y2)
    if (det == 0) return
    val invDet = 1.0 / det

    val rgb = -16777216 or (r shl 16) or (g shl 8) or b

    for (y in minY..maxY) {
        for (x in minX..maxX) {
            val w1 = ((y1 - y2) * (x - x2) + (x2 - x1) * (y - y2)) * invDet
            val w2 = ((y2 - y0) * (x - x2) + (x0 - x2) * (y - y2)) * invDet
            if (w1 >= -0.0001 && w2 >= -0.0001 && (1.0 - w1 - w2) >= -0.0001) {
                img.setRGB(x, y, rgb)
            }
        }
    }
}