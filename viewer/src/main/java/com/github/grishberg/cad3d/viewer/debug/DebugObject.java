package com.github.grishberg.cad3d.viewer.debug;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;
import java.util.List;

/**
 * Класс для хранения отладочных объектов
 */
public class DebugObject {

    public enum Type {
        POINT, LINE, POLYGON
    }

    private final Type type;
    private final List<V3d> vertices;
    private final double lineThickness;
    private final double pointThickness;
    private final Color color;
    private final Color vertexColor; // Дополнительный цвет для вершин

    public DebugObject(
        Type type,
        List<V3d> vertices,
        double lineThickness,
        double pointThickness,
        Color color
    ) {
        this(type, vertices, lineThickness, pointThickness, color, null);
    }

    public DebugObject(
        Type type,
        List<V3d> vertices,
        double lineThickness,
        double pointThickness,
        Color color,
        Color vertexColor
    ) {
        this.type = type;
        this.vertices = vertices;
        this.lineThickness = lineThickness;
        this.pointThickness = pointThickness;
        this.color = color;
        this.vertexColor = vertexColor;
    }

    public Type getType() {
        return type;
    }

    public List<V3d> getVertices() {
        return vertices;
    }

    public double getLineThickness() {
        return lineThickness;
    }

    public double getPointThickness() {
        return pointThickness;
    }

    public Color getColor() {
        return color;
    }

    public Color getVertexColor() {
        return vertexColor;
    }

    public boolean hasVertexColor() {
        return vertexColor != null;
    }
} 
