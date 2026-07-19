package com.github.grishberg.javascad.optimizator

import com.github.grishberg.javascad.vrl.Polygon
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

class CommonPolygonFinderMultithreading(private val numChunks: Int, progressObserver: ProgressObserver) : EdgesFinder {

    override suspend fun groupPolygonsByEdges(polygons: List<Polygon>): Map<LineKey, List<PolygonEdge>> =
        withContext(Dispatchers.Default) {

            val chunks = polygons.chunked(
                if (polygons.size < numChunks) 1
                else max(1, polygons.size / numChunks)
            )

            val deferredResults = chunks.map { chunk ->
                async {
                    processPolygonChunk(chunk)
                }
            }

            val results: List<Map<LineKey, List<PolygonEdge>>> = deferredResults.awaitAll()

            val mergedMap = results.fold(mutableMapOf<LineKey, MutableList<PolygonEdge>>()) { acc, map ->
                map.forEach { (key, edges) ->
                    acc.getOrPut(key) { mutableListOf() }.addAll(edges)
                }
                acc
            }

            mergedMap
        }

    /**
     * Обрабатывает один чанк полигонов и возвращает карту рёбер для этого чанка.
     */
    private fun processPolygonChunk(chunk: List<Polygon>): Map<LineKey, List<PolygonEdge>> {
        val localMap = mutableMapOf<LineKey, MutableList<PolygonEdge>>()

        for (polygon in chunk) {
            val vertices = polygon.vertices
            for (i in vertices.indices) {
                val a = vertices[i]
                val b = vertices[(i + 1) % vertices.size]
                val key: LineKey? = LineKey.fromSegment(a, b)
                if (key != null) {
                    val currentList = localMap.computeIfAbsent(key) { mutableListOf() }
                    currentList.add(PolygonEdge(polygon, a, b, i))
                }
            }
        }

        return localMap
    }
}
