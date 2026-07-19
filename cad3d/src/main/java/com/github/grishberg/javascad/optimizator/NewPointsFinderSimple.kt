package com.github.grishberg.javascad.optimizator

import com.github.grishberg.javascad.vrl.Polygon

class NewPointsFinderSimple(private val progressObserver: ProgressObserver) : NewPointsFinder {

    override suspend fun findNewPoints(edges: Map<LineKey, List<PolygonEdge>>): Map<Polygon, MutableSet<PointInsert>> {
        val mergedPoints = HashMap<Polygon, MutableSet<PointInsert>>()

        var iter = 0
        var percent = 0
        var percentF: Float

        for (entry in edges.entries) {
            val newPoints = findNewPoints(entry.value)

            // Объединяем новые точки с общим результатом
            for (newEntry in newPoints.entries) {
                val polygon = newEntry.key
                val points = newEntry.value

                val dst = mergedPoints.computeIfAbsent(polygon) { k: Polygon -> HashSet() }
                dst.addAll(points)
            }

            percentF = ((iter++).toFloat() / edges.size.toFloat()) * 50f
            val newPercent = percentF.toInt()
            if (newPercent > percent) {
                progressObserver.onProgress(newPercent + 50)
            }
            percent = newPercent
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
