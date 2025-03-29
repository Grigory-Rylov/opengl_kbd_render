package eu.printingin3d.javascad.coords2d

import com.menecats.polybool.PolyBool
import com.menecats.polybool.helpers.PolyBoolHelper.epsilon
import com.menecats.polybool.helpers.PolyBoolHelper.point
import com.menecats.polybool.helpers.PolyBoolHelper.polygon
import com.menecats.polybool.helpers.PolyBoolHelper.region
import com.menecats.polybool.models.Point2d
import com.menecats.polybool.models.Polygon
import eu.printingin3d.javascad.models2d.Area2d

class PolygonMerger {

    private val eps = epsilon()
    fun mergePolygons(polygons: List<Area2d>): List<Area2d> {
        if (polygons.isEmpty()){
            return emptyList()
        }

        var resultPolygon : Polygon = polygons.first().toPolygon()
        for (i in 1 until polygons.size) {
            if (polygons[i].size >= 3) {
                val other = polygons[i].toPolygon()
                resultPolygon = PolyBool.union(eps, resultPolygon, other)
            }
        }

        return resultPolygon.regions.map { region -> Area2d(region.map { p -> Coords2d(p.x, p.y) }) }.filter { it.size > 2 }
    }

    private fun Area2d.toPolygon(): Polygon {
        val pointsList = mutableListOf<Point2d>()
        for(p in this.points){
            pointsList.add(point(p.x, p.y))
        }

        return polygon(pointsList)
    }
}
