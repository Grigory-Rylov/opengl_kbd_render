package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import com.github.grishberg.javascad.vrl.Polygon;
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
