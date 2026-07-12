package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;

import com.github.grishberg.openscad.coords.V3d;
import com.github.grishberg.openscad.utils.Color;
import com.github.grishberg.openscad.vrl.Polygon;
import com.github.grishberg.openscad.vrl.VertexPosition;

public class SplitStartedCmd implements DebugCmd {

    private final Polygon originalPolygon;
    private final Polygon splitPolygon;
    private final VertexPosition polygonType;

    public SplitStartedCmd(
        Polygon originalPolygon, Polygon splitPolygon,
        VertexPosition polygonType
    ) {
        this.originalPolygon = originalPolygon;
        this.splitPolygon = splitPolygon;
        this.polygonType = polygonType;
    }

    @Override
    public String getDescription() {
        return "Split polygon started - Original: " + originalPolygon.getVertices().size() +
            " vertices, Split: " + splitPolygon.getVertices().size() + " vertices, polygonType = " +
            polygonType;
    }

    @Override
    public void render(DebugVisualizer debugVisualizer) {
        // Рисуем оригинальный полигон
        debugVisualizer.drawDebugPolygon(originalPolygon.getVertices(), DbgConfig.LINE_THICKNESS, Color.PINK);

        // Рисуем разделенный полигон
        debugVisualizer.drawDebugPolygon(splitPolygon.getVertices(), DbgConfig.LINE_THICKNESS, Color.CYAN);

        // Рисуем точки вершин оригинального полигона
        for (V3d vertex : originalPolygon.getVertices()) {
            debugVisualizer.drawDebugPoint(vertex, DbgConfig.POINT_THICKNESS, Color.RED);
        }

        // Рисуем точки вершин разделенного полигона
        for (V3d vertex : splitPolygon.getVertices()) {
            debugVisualizer.drawDebugPoint(vertex, DbgConfig.POINT_THICKNESS_1, Color.BLUE);
        }
    }
}
