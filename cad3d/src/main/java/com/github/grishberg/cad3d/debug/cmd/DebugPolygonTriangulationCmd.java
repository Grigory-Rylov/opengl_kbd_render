package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;

import java.util.List;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.vrl.Facet;
import com.github.grishberg.javascad.vrl.Polygon;

public class DebugPolygonTriangulationCmd implements DebugCmd {
    private final Color[] colors = {
        Color.CYAN,
        Color.GREEN,
        Color.YELLOW,
        Color.MAGENTA
    };

    private final Polygon polygon;
    private final List<Facet> facets;

    public DebugPolygonTriangulationCmd(Polygon polygon, List<Facet> facets) {
        this.polygon = polygon;
        this.facets = facets;
    }

    @Override
    public String getDescription() {
        String verticesInfo = EndCmd.verticesToString(polygon.getVertices());
        return "DebugPolygonTriangulationCmd polygon = " + verticesInfo + ", triangles = " + facets.size();
    }

    @Override
    public void render(DebugVisualizer debugVisualizer) {
        debugVisualizer.drawDebugPolygon(
            polygon.getVertices(),
            DbgConfig.LINE_THICKNESS,
            DbgConfig.POINT_THICKNESS,
            Color.CYAN,
            Color.BLUE
        );

        for (int i = 0; i < facets.size(); i++) {
            Facet facet = facets.get(i);
            debugVisualizer.drawDebugPolygon(
                facet.getTriangle().getPoints(),
                DbgConfig.LINE_THICKNESS,
                DbgConfig.COLORS[i % DbgConfig.COLORS.length]
            );

            List<V3d> vertex = facet.getTriangle().getPoints();
            for (int j = 0; j < vertex.size(); j++) {
                V3d p = vertex.get(j);
                Color vertexColor = colors[j % colors.length];
                debugVisualizer.drawDebugPoint(p, DbgConfig.POINT_THICKNESS_1, vertexColor);
            }
        }
    }
}
