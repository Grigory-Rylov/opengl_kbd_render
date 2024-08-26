package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.Polygon;
import java.util.List;

class Utils {
    static void addQuadAsTriangles(
        List<Polygon> polygons,
        V3d v1, V3d v2, V3d v3, V3d v4,
        FacetGenerationContext context
    ) {
        polygons.add(Polygon.fromPolygons(v1, v2, v3, context.getColor()));
        polygons.add(Polygon.fromPolygons (v1, v3, v4, context.getColor()));
    }
}
