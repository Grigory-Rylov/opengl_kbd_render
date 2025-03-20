package eu.printingin3d.javascad.models

import eu.printingin3d.javascad.context.IColorGenerationContext
import eu.printingin3d.javascad.coords.Boundary
import eu.printingin3d.javascad.coords.V3d
import eu.printingin3d.javascad.coords2d.Boundaries2d
import eu.printingin3d.javascad.coords2d.Coords2d
import eu.printingin3d.javascad.exceptions.IllegalValueException
import eu.printingin3d.javascad.models2d.Abstract2dModel
import eu.printingin3d.javascad.models2d.Area2d
import eu.printingin3d.javascad.vrl.CSG
import eu.printingin3d.javascad.vrl.FacetGenerationContext
import java.util.stream.Collectors

class Projection(private val model: Abstract3dModel) : Abstract2dModel(Coords2d.ZERO) {

    override fun innerToScad(context: IColorGenerationContext?): SCAD {
        return SCAD("projection(cut=false) {\n").append(model.toScad(context)).append("}\n")
    }

    override fun getInnerPointCircle(context: FacetGenerationContext?): Collection<Area2d> {
        val originalCSG: CSG = model.toCSG(context)
        val polygons = originalCSG.polygons

        // Проецируем все полигоны и фильтруем вырожденные
        return polygons.stream().map { p ->
            projectPolygon(
                p.vertices
            )
        }.filter { area -> area != null && !isDegenerate(area) }.collect(Collectors.toList())
    }

    private fun projectPolygon(vertices3d: List<V3d>): Area2d {
        val vertices2d = vertices3d.stream().map { v: V3d -> Coords2d(v.getX(), v.getY()) }.collect(Collectors.toList())
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
