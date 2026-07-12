package com.github.grishberg.javascad.optimizator

import com.github.grishberg.openscad.vrl.Polygon
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class NewPointsFinderMultithread(private val numChunks: Int, private val progressObserver: ProgressObserver) :
    NewPointsFinder {

    override suspend fun findNewPoints(edges: Map<LineKey, List<PolygonEdge>>): Map<Polygon, MutableSet<PointInsert>> =
        withContext(Dispatchers.Default) {
            val mergedPoints = HashMap<Polygon, MutableSet<PointInsert>>()

            val keys = edges.keys.toList()
            val chunks = keys.chunked(
                if (keys.size < numChunks) 1
                else max(1, keys.size / numChunks)
            )

            val deferredResults = chunks.map { chunk ->
                async {
                    processEdgesChunk(edges, chunk)
                }
            }

            val results: List<Map<Polygon, Set<PointInsert>>> = deferredResults.awaitAll()

            results.forEach { map ->
                map.forEach { (polygon, points) ->
                    mergedPoints.getOrPut(polygon) { mutableSetOf() }.addAll(points)
                }
            }
            mergedPoints
        }

    private fun processEdgesChunk(
        edges: Map<LineKey, List<PolygonEdge>>,
        keys: List<LineKey>,
    ): Map<Polygon, Set<PointInsert>> {
        val mergedPoints = HashMap<Polygon, MutableSet<PointInsert>>()

        keys.forEach { key ->
            val polygonEdgesList = edges[key]!!

            val newPoints = findNewPoints(polygonEdgesList)

            // Объединяем новые точки с общим результатом
            for (newEntry in newPoints.entries) {
                val polygon = newEntry.key
                val points = newEntry.value

                val dst = mergedPoints.computeIfAbsent(polygon) { k: Polygon -> HashSet() }
                dst.addAll(points)
            }
        }
        return mergedPoints
    }

    private fun findNewPoints(polygons: List<PolygonEdge>): Map<Polygon, Set<PointInsert>> {
        val result = HashMap<Polygon, MutableSet<PointInsert>>()

        if (polygons.size < 2) {
            result.put(polygons[0].polygon, mutableSetOf())
            return result
        }
        for (i in 0..<polygons.size - 1) {
            val currentPolygon = polygons[i]
            for (j in i + 1..<polygons.size) {
                val otherPolygon = polygons[j]
                val a1 = currentPolygon.p0
                val b1 = currentPolygon.p1

                val a2 = otherPolygon.p0
                val b2 = otherPolygon.p1

                // should insert points into current
                val currentPolygonToBeAdded =
                    result.computeIfAbsent(currentPolygon.polygon) { k: Polygon -> HashSet<PointInsert>() }

                if (CrossEdgeValidator.isPointBetween(a2, a1, b1)) {
                    currentPolygonToBeAdded.add(
                        PointInsert(
                            a2, currentPolygon.firstPointIndex
                        )
                    )
                }
                if (CrossEdgeValidator.isPointBetween(b2, a1, b1)) {
                    currentPolygonToBeAdded.add(
                        PointInsert(
                            b2, currentPolygon.firstPointIndex
                        )
                    )
                }

                // should insert points into other
                val otherPolygonToBeAdded =
                    result.computeIfAbsent(otherPolygon.polygon) { k: Polygon -> HashSet<PointInsert>() }
                if (CrossEdgeValidator.isPointBetween(a1, a2, b2)) {
                    otherPolygonToBeAdded.add(PointInsert(a1, otherPolygon.firstPointIndex))
                }
                if (CrossEdgeValidator.isPointBetween(b1, a2, b2)) {
                    otherPolygonToBeAdded.add(PointInsert(b1, otherPolygon.firstPointIndex))
                }
            }
        }
        return result
    }
}
