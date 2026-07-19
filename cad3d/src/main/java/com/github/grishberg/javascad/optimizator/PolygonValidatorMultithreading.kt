package com.github.grishberg.javascad.optimizator

import com.github.grishberg.javascad.coords.V3d
import com.github.grishberg.javascad.vrl.Polygon
import java.util.Collections
import java.util.TreeMap
import java.util.function.ToDoubleFunction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

/**
 * Утилита для исправления проблем в полигонах
 */
class PolygonValidatorMultithreading {

    /**
     * Исправляет проблемы в полигонах: коллинеарные точки, близкие вершины, naked edges
     */
    @JvmOverloads
    fun fixPolygons(
        polygons: List<Polygon>, progressObserver: ProgressObserver = ProgressObserver.STUB
    ): List<Polygon> {

        val availableProcessors = Runtime.getRuntime().availableProcessors()
        val optimalThreadCount = availableProcessors.coerceAtLeast(1)

        val processor: EdgesFinder = CommonPolygonFinderMultithreading(optimalThreadCount, progressObserver)
        val startTime = System.currentTimeMillis()
        val edges = runBlocking(Dispatchers.Default) {
            // Вызываем suspend функцию
            processor.groupPolygonsByEdges(polygons)
        }

        val newPointsFinder: NewPointsFinder = NewPointsFinderMultithread(optimalThreadCount, progressObserver)
        val mergedPoints = runBlocking(Dispatchers.Default) {
            newPointsFinder.findNewPoints(edges)
        }

        val result = addPolygonNewVertices(mergedPoints)
        println(
            "Polygon validator duration = " + (System.currentTimeMillis() - startTime) + " ms"
        )
        return result
    }

    private fun addPolygonNewVertices(newVerticesInfo: Map<Polygon, Set<PointInsert>>): List<Polygon> {
        val polygons = ArrayList<Polygon>()
        for (entry in newVerticesInfo.entries) {
            val currentPolygon: Polygon = entry.key
            val currentPolygonVertices = ArrayList<V3d>(currentPolygon.getVertices())

            val groupedPoints = TreeMap<Int, MutableSet<V3d>>(Collections.reverseOrder<Int>())

            // group vertices
            for (pointInsert in entry.value) {
                val pointsOfGroup = groupedPoints.computeIfAbsent(pointInsert!!.position) { k: Int -> HashSet<V3d>() }
                pointsOfGroup.add(pointInsert.point)
            }

            // Теперь итерация будет от большего ключа к меньшему
            for (verticesEntry in groupedPoints.entries) {
                val key: Int = verticesEntry.key
                val points = verticesEntry.value
                val sortedPoints = sortedPoints(currentPolygonVertices.get(key), points)

                if (key == currentPolygonVertices.size - 1) {
                    for (i in sortedPoints.indices.reversed()) {
                        currentPolygonVertices.add(sortedPoints.get(i))
                    }
                } else {
                    for (pointToBeAdded in sortedPoints) {
                        currentPolygonVertices.add(key + 1, pointToBeAdded)
                    }
                }
            }
            if (Polygon.isValid(
                    currentPolygonVertices, currentPolygon.getNormal(), currentPolygon.getDist()
                )
            ) {
                polygons.add(
                    Polygon.fromPolygons(
                        currentPolygonVertices, currentPolygon.getNormal(), currentPolygon.getColor()
                    )
                )
            } else {
                println("Invalid triangle")
            }
        }
        return polygons
    }

    private fun sortedPoints(startPoint: V3d, newPoints: Set<V3d>): List<V3d> {
        val sortedNewPoints = ArrayList<V3d>(newPoints)
        sortedNewPoints.sortWith(Comparator.comparingDouble(ToDoubleFunction { p: V3d ->
            -distanceSquared(
                startPoint, p
            )
        }))
        return sortedNewPoints
    }

    companion object {

        /**
         * Вычисляет квадрат расстояния между двумя точками (для избежания вычисления sqrt).
         */
        private fun distanceSquared(a: V3d, b: V3d): Double {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val dz = b.z - a.z
            return dx * dx + dy * dy + dz * dz
        }
    }
}
