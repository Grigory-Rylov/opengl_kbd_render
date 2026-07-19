package com.github.grishberg.javascad

import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.utils.Color
import com.github.grishberg.javascad.vrl.Polygon
import java.io.DataInputStream
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

class StlImporter {
    fun loadBinarySTL(filePath: String, color: Color = Color.GRAY): List<Polygon> {
        val polygons = mutableListOf<Polygon>()

        DataInputStream(FileInputStream(filePath)).use { dis ->
            // Пропускаем заголовок (80 байт)
            val header = ByteArray(80)
            dis.readFully(header)

            // Читаем количество треугольников (4 байта) - используем исправленный метод
            val numberOfTriangles = readLittleEndianInt(dis)

            // Проверяем корректность количества треугольников
            if (numberOfTriangles <= 0 || numberOfTriangles > 10_000_000) {
                throw IllegalArgumentException("Invalid number of triangles: $numberOfTriangles")
            }

            println("Reading $numberOfTriangles triangles...")

            var percent = 0
            var percentF: Float
            for (i in 0 until numberOfTriangles) {
                try {
                    val polygon = readTriangle(dis, color)
                    polygons.add(polygon)
                } catch (e: Exception) {
                    println("Error reading triangle $i: ${e.message}")
                    // В зависимости от требований, можно либо прервать, либо пропустить
                    // break // Закомментировано, чтобы продолжать чтение
                }

                percentF = (i/ numberOfTriangles.toFloat()) * 100f
                val newPercent = percentF.roundToInt()
                if (newPercent > percent) {
                    println("Progress $newPercent")
                }
                percent = newPercent
            }
        }

        return polygons
    }

    private fun readLittleEndianInt(dis: DataInputStream): Int {
        val bytes = ByteArray(4)
        dis.readFully(bytes)
        return ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .int
    }

    // Используем ByteBuffer для чтения float в Little-Endian
    private fun readLittleEndianFloat(dis: DataInputStream): Float {
        val bytes = ByteArray(4)
        dis.readFully(bytes)
        return ByteBuffer.wrap(bytes)
            .order(ByteOrder.LITTLE_ENDIAN)
            .float
    }

    private fun readTriangle(dis: DataInputStream, color: Color): Polygon {
        // Читаем нормаль (3 * 4 байта = 12 байт)
        val normal = readVector3D(dis)

        // Читаем 3 вершины (3 * 3 * 4 байта = 36 байт)
        val vertices = listOf(
            readVector3D(dis),
            readVector3D(dis),
            readVector3D(dis)
        )

        // Читаем атрибуты (2 байта)
        // Используем readUnsignedShort() или читаем как байты, если нужен signed short
        val attributesBytes = ByteArray(2)
        dis.readFully(attributesBytes)
        val attributes = ByteBuffer.wrap(attributesBytes).order(ByteOrder.LITTLE_ENDIAN).short

        // Проверяем корректность нормали (должна быть единичной)
        val length = sqrt(normal.x * normal.x + normal.y * normal.y + normal.z * normal.z)
        if (abs(length - 1.0f) > 0.1f && length > 0f) {
            // Ненормализованная нормаль - можно нормализовать здесь если нужно
            println("Warning: Non-unit normal vector in triangle (length: $length)")
        }

        return Polygon.fromPolygons(vertices, color)
    }

    private fun readVector3D(dis: DataInputStream): V3d {
        // Используем исправленный метод для чтения float
        val x = readLittleEndianFloat(dis).toDouble()
        val y = readLittleEndianFloat(dis).toDouble()
        val z = readLittleEndianFloat(dis).toDouble()
        return V3d(x, y, z)
    }
}
