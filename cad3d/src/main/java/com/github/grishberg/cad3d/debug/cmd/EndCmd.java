package com.github.grishberg.cad3d.debug.cmd;


import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.List;

public class EndCmd implements DebugCmd {

    private final List<Polygon> polygons;
    private final List<Polygon> allPolygons;


    public EndCmd(List<Polygon> polygons) {
        this.polygons = polygons;
        allPolygons = new ArrayList<>();
    }

    public EndCmd(Polygon p) {
        this.polygons = new ArrayList<>();
        polygons.add(p);
        allPolygons = new ArrayList<>();
    }

    public EndCmd(Polygon p, List<Polygon> allPolygons) {
        this.polygons = new ArrayList<>();
        polygons.add(p);
        this.allPolygons = allPolygons;
    }

    @Override
    public String getDescription() {
        Polygon polygon = polygons.get(0);
        return "End - polygons: vertex =" + polygon.getVertices().size() + " , "
            //+ verticesToString(polygon.getVertices());
            + polygon.toJson();
    }

    public static String verticesToString(List<V3d> vertices) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vertices.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(vertices.get(i));
        }
        return sb.toString();
    }

    public void render(DebugVisualizer debugVisualizer) {
        for (Polygon p : allPolygons) {
            debugVisualizer.drawDebugPolygon(
                p, DbgConfig.LINE_THICKNESS,
                DbgConfig.POINT_THICKNESS, Color.lightGray, Color.WHITE
            );
        }
        // Рисуем полигоны с циклической сменой цветов
        for (int i = 0; i < polygons.size(); i++) {
            Polygon p = polygons.get(i);
            Color color = DbgConfig.COLORS[i % DbgConfig.COLORS.length]; // Циклический выбор цвета
            debugVisualizer.drawDebugPolygon(
                p.getVertices(),
                DbgConfig.LINE_THICKNESS,
                DbgConfig.POINT_THICKNESS,
                color, Color.BLUE
            );

            int j = 0;
            for (V3d vertex : p.getVertices()) {
                debugVisualizer.drawDebugPoint(
                    vertex,
                    DbgConfig.POINT_THICKNESS_1,
                    DbgConfig.COLORS[j % DbgConfig.COLORS.length]
                );
                j++;
            }
        }
    }
}
