package com.github.grishberg.cad3d.debug.cmd;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;
import com.github.grishberg.javascad.optimizator.PolygonValidator;

import java.util.List;

import com.github.grishberg.openscad.coords.V3d;
import com.github.grishberg.openscad.utils.Color;
import com.github.grishberg.openscad.vrl.Polygon;

public class DebugGroupedEdgeCmd implements DebugCmd {

    private final PolygonValidator.LineKey key;
    private final List<PolygonValidator.PolygonEdge> polygons;

    public DebugGroupedEdgeCmd(PolygonValidator.LineKey key, List<PolygonValidator.PolygonEdge> polygons) {
        this.key = key;
        this.polygons = polygons;
    }

    @Override
    public String getDescription() {
        return "DebugGroupedEdgeCmd edge: polygons =" + polygons.size() + " point = " + key.getPointOnLine() + ", " +
            "direction = " + key.getDirectionUnitVector();
    }

    @Override
    public void render(DebugVisualizer debugVisualizer) {

        double length = 30;
        // Половина длины отрезка
        double halfLength = length / 2.0;

        // Вычисляем смещение по направлению вектора
        // Так как вектор единичный, умножение дает вектор длины halfLength
        double dx = key.getDirectionUnitVector().x * halfLength;
        double dy = key.getDirectionUnitVector().y * halfLength;
        double dz = key.getDirectionUnitVector().z * halfLength;

        // Точка p0: от pointOnLine на halfLength в направлении, противоположном directionUnitVector
        V3d p0 = new V3d(key.getPointOnLine().x - dx, key.getPointOnLine().y - dy, key.getPointOnLine().z - dz);

        // Точка p1: от pointOnLine на halfLength в направлении directionUnitVector
        V3d p1 = new V3d(key.getPointOnLine().x + dx, key.getPointOnLine().y + dy, key.getPointOnLine().z + dz);
        debugVisualizer.drawDebugLine(p0, p1, DbgConfig.LINE_THICKNESS_2, Color.CYAN);

        for (int i = 0; i < polygons.size(); i++) {
            Polygon polygon = polygons.get(i).polygon;
            Color color = DbgConfig.COLORS[i % DbgConfig.COLORS.length]; // Циклический выбор цвета
            debugVisualizer.drawDebugPolygon(polygon.getVertices(), DbgConfig.LINE_THICKNESS_1, color);
        }
    }
}
