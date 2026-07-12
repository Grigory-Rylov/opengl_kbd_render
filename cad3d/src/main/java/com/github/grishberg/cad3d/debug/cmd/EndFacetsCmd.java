package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;

import java.util.List;

import com.github.grishberg.openscad.coords.V3d;
import com.github.grishberg.openscad.utils.Color;
import com.github.grishberg.openscad.vrl.Facet;

public class EndFacetsCmd implements DebugCmd {

    private final List<Facet> facets;

    private final Color[] colors = {
        Color.CYAN,
        Color.GREEN,
        Color.YELLOW,
        Color.MAGENTA
    };

    public EndFacetsCmd(List<Facet> facets) {
        this.facets = facets;
    }

    @Override
    public String getDescription() {
        return "End - facets: " + facets.size();
    }

    @Override
    public void render(DebugVisualizer debugVisualizer) {

        // Рисуем фасеты с циклической сменой цветов
        for (int i = 0; i < facets.size(); i++) {
            Facet facet = facets.get(i);
            Color color = DbgConfig.COLORS[i % DbgConfig.COLORS.length]; // Циклический выбор цвета
            debugVisualizer.drawDebugPolygon(facet.getTriangle().getPoints(), DbgConfig.LINE_THICKNESS, color);

            List<V3d> vertex = facet.getTriangle().getPoints();
            for (int j = 0; j < vertex.size(); j++) {
                V3d p = vertex.get(j);
                Color vertexColor = colors[j % colors.length];
                debugVisualizer.drawDebugPoint(p, DbgConfig.POINT_THICKNESS, vertexColor);
            }
        }
    }
}
