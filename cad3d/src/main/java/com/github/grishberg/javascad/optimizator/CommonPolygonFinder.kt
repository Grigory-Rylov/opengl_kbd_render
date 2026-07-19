package com.github.grishberg.javascad.optimizator

import com.github.grishberg.javascad.vrl.Polygon

class CommonPolygonFinder(private val progressObserver: ProgressObserver) : EdgesFinder {

    override suspend fun groupPolygonsByEdges(polygons: List<Polygon>): Map<LineKey, List<PolygonEdge>> {
        val map = HashMap<LineKey, MutableList<PolygonEdge>>()

        var percent = 0
        var percentF = 0f

        var iter = 0
        for (polygon in polygons) {
            val vertices = polygon.getVertices()
            for (i in vertices.indices) {
                val a = vertices.get(i)
                val b = vertices.get((i + 1) % vertices.size)
                val key: LineKey? = LineKey.Companion.fromSegment(a, b)
                if (key != null) {
                    val currentList = map.computeIfAbsent(key) { k: LineKey -> ArrayList<PolygonEdge>() }
                    currentList.add(PolygonEdge(polygon, a, b, i))
                } else {
                    println("Found closes points " + a + " - " + b)
                }
            }

            percentF = (iter.toFloat() / polygons.size.toFloat()) * 50f
            val newPercent = percentF.toInt()
            if (newPercent > percent) {
                progressObserver.onProgress(newPercent)
            }
            percent = newPercent
            iter++
        }

        return map
    }
}
