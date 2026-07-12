package com.github.grishberg.cad3d.ui;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.openscad.coords.V3d;
import com.github.grishberg.openscad.utils.Color;
import com.github.grishberg.openscad.vrl.Facet;
import com.github.grishberg.openscad.vrl.Polygon;
import java.util.List;

/**
 * Интерфейс для визуализации отладочных состояний
 */
public interface DebugVisualizer {
    /**
     * Применяет визуализацию для отладочной команды
     * @param cmd отладочная команда
     */
    void applyDebugVisualization(DebugCmd cmd);

    /**
     * Очищает все визуальные эффекты
     */
    void clearVisualization();

    /**
     * Рисует отладочный полигон
     * @param vertices список вершин полигона
     * @param lineColor цвет полигона
     */
    void drawDebugPolygon(List<V3d> vertices, double lineThickness, Color lineColor);
    /**
     * Рисует отладочный полигон
     * @param vertices список вершин полигона
     * @param lineColor цвет полигона
     * @param vertexColor цвет вершины
     */
    void drawDebugPolygon(List<V3d> vertices, double lineThickness,
                          double pointThickness, Color lineColor, Color vertexColor);

    void drawDebugPolygon(Polygon polygon, double lineThickness,
                          double pointThickness, Color lineColor, Color vertexColor);

    void drawDebugFacet(Facet facet, double lineThickness,
                        double pointThickness, Color lineColor, Color vertexColor);

    /**
     * Рисует отладочную линию
     * @param p0 начальная точка
     * @param p1 конечная точка
     * @param color цвет линии
     */
    void drawDebugLine(V3d p0, V3d p1, double thickness, Color color);

    /**
     * Рисует отладочную точку (куб размером 1)
     * @param p позиция точки
     * @param color цвет точки
     */
    void drawDebugPoint(V3d p, double thickness, Color color);

}
