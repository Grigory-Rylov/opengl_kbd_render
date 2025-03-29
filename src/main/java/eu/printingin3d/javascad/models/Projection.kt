package eu.printingin3d.javascad.models

import eu.printingin3d.javascad.context.IColorGenerationContext
import eu.printingin3d.javascad.coords.Boundary
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.coords2d.Boundaries2d
import eu.printingin3d.javascad.coords2d.Coords2d
import eu.printingin3d.javascad.coords2d.PolygonMerger
import eu.printingin3d.javascad.exceptions.IllegalValueException
import eu.printingin3d.javascad.models2d.Abstract2dModel
import eu.printingin3d.javascad.models2d.Area2d
import eu.printingin3d.javascad.vrl.CSG
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import eu.printingin3d.javascad.vrl.Polygon

class Projection(private val model: Abstract3dModel) : Abstract2dModel(Coords2d.ZERO) {

    override fun innerToScad(context: IColorGenerationContext?): SCAD {
        return SCAD("projection(cut=false) {\n").append(model.toScad(context)).append("}\n")
    }

//    override fun getInnerPointCircle(context: FacetGenerationContext?): Collection<Area2d> {
//        val originalCSG: CSG = model.toCSG(context)
//        val polygons = originalCSG.polygons
//
//        val startTime = System.currentTimeMillis()
//        // Проецируем все полигоны и фильтруем вырожденные
//        val areas = polygons.map { p ->
//            projectPolygon(
//                p.vertices
//            )
//        }.filter { area -> !isDegenerate(area) }
//        val time2 = System.currentTimeMillis()
//
//        val merger = PolygonMerger()
//        val merged = merger.mergePolygons(areas)
//
//        val time3 = System.currentTimeMillis()
//
//        println("Calculation t1 = ${time2 - startTime}, t2 = ${time3 - time2}")
//
//        return merged
//    }

    override fun getInnerPointCircle(context: FacetGenerationContext?): Collection<Area2d> {
        val originalCSG: CSG = model.toCSG(context)
        val polygons = originalCSG.polygons

        val startTime = System.currentTimeMillis()
        // Проецируем все полигоны и фильтруем вырожденные
        val areas = doProjection(polygons)

        val time3 = System.currentTimeMillis()

        println("Calculation t1 = ${time3 - startTime}")

        return areas
    }

    private fun doProjection(polygons: List<Polygon>): List<Area2d> {
        return listOf(Area2d(projectTo2D(polygons)))
    }

    fun projectTo2D(polygons: List<Polygon>): List<Coords2d> {
        // Проецируем все полигоны на плоскость Z=0

        val projectedPolygons = polygons.map { polygon ->
            polygon.vertices.map { Coords2d(it.x, it.y) }
        }

        // Находим внешние рёбра
        val edges = mutableMapOf<Pair<Coords2d, Coords2d>, Int>()
        for (polygon in projectedPolygons) {
            for (i in polygon.indices) {
                val start = polygon[i]
                val end = polygon[(i + 1) % polygon.size]
                val edge = if (start < end) Pair(start, end) else Pair(end, start)
                edges[edge] = edges.getOrDefault(edge, 0) + 1
            }
        }

        // Оставляем только внешние рёбра (те, которые встречаются только один раз)
        val outerEdges = edges.filter { it.value == 1 }.keys.toList()

        // Сортируем рёбра, чтобы получить последовательный контур
        val sortedEdges = sortEdges(outerEdges)

        // Извлекаем уникальные точки из отсортированных рёбер
        return sortedEdges.flatMap { listOf(it.first, it.second) }.distinct()
    }

    private fun sortEdges(edges: List<Pair<Coords2d, Coords2d>>): List<Pair<Coords2d, Coords2d>> {
        val sortedEdges = mutableListOf<Pair<Coords2d, Coords2d>>()
        val remainingEdges = edges.toMutableList()

        var currentEdge = remainingEdges.removeAt(0)
        sortedEdges.add(currentEdge)

        while (remainingEdges.isNotEmpty()) {
            val nextEdge = remainingEdges.find { it.first == currentEdge.second || it.second == currentEdge.second }
            if (nextEdge != null) {
                remainingEdges.remove(nextEdge)
                if (nextEdge.first == currentEdge.second) {
                    sortedEdges.add(nextEdge)
                    currentEdge = nextEdge
                } else {
                    sortedEdges.add(Pair(nextEdge.second, nextEdge.first))
                    currentEdge = Pair(nextEdge.second, nextEdge.first)
                }
            } else {
                // Если не нашли следующее ребро, начинаем новый контур
                currentEdge = remainingEdges.removeAt(0)
                sortedEdges.add(currentEdge)
            }
        }

        return sortedEdges
    }

    private operator fun Coords2d.compareTo(other: Coords2d): Int {
        return when {
            this.x != other.x -> this.x.compareTo(other.x)
            else -> this.y.compareTo(other.y)
        }
    }

    private fun projectPolygon(vertices3d: List<V3d>): Area2d {
        val vertices2d = vertices3d.map { v: V3d -> Coords2d(v.getX(), v.getY()) }
        return Area2d(vertices2d)
    }

    private fun isDegenerate(area: Area2d): Boolean {
        if (area.size < 3) return true

        var areaValue = 0.0
        val points = area.points
        for (i in points.indices) {
            val current = points[i]
            val next = points[(i + 1) % points.size]
            areaValue += current.x * next.y - next.x * current.y
        }
        return Math.abs(areaValue) < 1e-6
    }

    //------------------

    override fun move(delta: Coords2d): Abstract2dModel {
        return Projection(model.move(V3d(delta.x, delta.y, 0.0)))
    }

    override fun getModelBoundaries(): Boundaries2d {
        val original3dBoundaries = model.boundaries
        return Boundaries2d(
            Boundary(original3dBoundaries.getMinCorner().getX(), original3dBoundaries.getMaxCorner().getX()),
            Boundary(original3dBoundaries.getMinCorner().getY(), original3dBoundaries.getMaxCorner().getY())
        )
    }

    private fun safeUnion(areas: List<Area2d>): Area2d? {
        var result: Area2d? = null
        for (area in areas) {
            if (area?.points == null || area.points.isEmpty()) {
                continue
            }
            if (result == null) {
                result = area
            } else {
                try {
                    result = result.union(area)
                } catch (e: NullPointerException) {
                    e.printStackTrace()
                    // Логирование ошибки
                } catch (e: IllegalValueException) {
                    e.printStackTrace()
                }
            }
        }
        return result
    }

    private fun unionAllAreas(areas: List<Area2d>): Area2d? {
        if (areas.isEmpty()) {
            throw IllegalValueException("No areas to union")
        }
        var result = areas[0]
        for (i in 1 until areas.size) {
            val current = areas[i]
            if (current != null && !current.points.isEmpty()) {
                result = result.union(current)
            }
        }
        return result
    }
}
