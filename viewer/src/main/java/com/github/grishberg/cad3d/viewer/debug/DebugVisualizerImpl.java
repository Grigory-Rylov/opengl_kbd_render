package com.github.grishberg.cad3d.viewer.debug;

import com.github.grishberg.cad3d.debug.DebugCmd;
import com.github.grishberg.cad3d.ui.DebugVisualizer;
import com.jogamp.opengl.GL2;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.vrl.Facet;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Реализация DebugVisualizer для работы с JOGL GL2
 */
public class DebugVisualizerImpl implements DebugVisualizer {


    private final List<DebugObject> debugObjects = new ArrayList<>();
    private GL2 gl;

    public DebugVisualizerImpl() {
    }

    /**
     * Устанавливает текущий GL контекст для рендеринга
     */
    public void setGL(GL2 gl) {
        this.gl = gl;
    }

    @Override
    public void applyDebugVisualization(DebugCmd cmd) {
        cmd.render(this);
        System.out.println(cmd.getDescription());
    }

    @Override
    public void clearVisualization() {
        debugObjects.clear();
    }

    @Override
    public void drawDebugPolygon(List<V3d> vertices, double thickness, Color color) {
        if (vertices.size() < 2) {
            return;
        }
        debugObjects.add(new DebugObject(
            DebugObject.Type.POLYGON,
            vertices,
            thickness,
            thickness,
            color
        ));
    }

    @Override
    public void drawDebugPolygon(
        List<V3d> vertices,
        double lineThickness,
        double pointThickness,
        Color lineColor,
        Color vertexColor
    ) {
        if (vertices.size() < 2) {
            return;
        }
        debugObjects.add(new DebugObject(
            DebugObject.Type.POLYGON,
            vertices,
            lineThickness,
            pointThickness,
            lineColor,
            vertexColor
        ));
    }

    @Override
    public void drawDebugPolygon(
        Polygon polygon,
        double lineThickness,
        double pointThickness,
        Color lineColor,
        Color vertexColor
    ) {
        drawDebugPolygon(
            polygon.getVertices(),
            lineThickness,
            pointThickness,
            lineColor,
            vertexColor
        );
    }

    @Override
    public void drawDebugFacet(
        Facet facet,
        double lineThickness,
        double pointThickness,
        Color lineColor,
        Color vertexColor
    ) {
        drawDebugPolygon(
            facet.getTriangle().getPoints(),
            lineThickness,
            pointThickness,
            lineColor,
            vertexColor
        );
    }

    @Override
    public void drawDebugLine(V3d p0, V3d p1, double thickness, Color color) {
        List<V3d> vertices = Arrays.asList(p0, p1);
        debugObjects.add(new DebugObject(
            DebugObject.Type.LINE,
            vertices,
            thickness,
            thickness,
            color
        ));
    }

    @Override
    public void drawDebugPoint(V3d p, double thickness, Color color) {
        List<V3d> vertices = Arrays.asList(p);
        debugObjects.add(new DebugObject(
            DebugObject.Type.POINT,
            vertices,
            thickness,
            thickness,
            color
        ));
    }

    public void renderDebugObjects() {
        if (gl == null) {
            return;
        }

        // Отключаем освещение для debug объектов
        boolean lightingEnabled = gl.glIsEnabled(GL2.GL_LIGHTING);
        if (lightingEnabled) {
            gl.glDisable(GL2.GL_LIGHTING);
        }

        // Включаем режим смешивания для прозрачности
        gl.glEnable(GL2.GL_BLEND);
        gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);

        for (DebugObject obj : debugObjects) {
            renderDebugObject(obj);
        }

        gl.glDisable(GL2.GL_BLEND);

        // Восстанавливаем освещение
        if (lightingEnabled) {
            gl.glEnable(GL2.GL_LIGHTING);
        }
    }

    private void renderDebugObject(DebugObject obj) {
        switch (obj.getType()) {
            case POINT:
                renderPoint(obj);
                break;
            case LINE:
                renderLine(obj);
                break;
            case POLYGON:
                renderPolygon(obj);
                break;
        }
    }

    private void renderPoint(DebugObject obj) {
        if (obj.getVertices().isEmpty()) {
            return;
        }

        V3d point = obj.getVertices().get(0);
        Color color = obj.getColor();
        double size = obj.getLineThickness();

        setColor(color);
        renderCube(point, size);
    }

    private void renderLine(DebugObject obj) {
        if (obj.getVertices().size() < 2) {
            return;
        }

        V3d p0 = obj.getVertices().get(0);
        V3d p1 = obj.getVertices().get(1);
        Color color = obj.getColor();
        double thickness = obj.getLineThickness();

        setColor(color);

        // Рисуем линию как прямоугольный параллелепипед
        renderLineAsBox(p0, p1, thickness);
    }

    private void renderPolygon(DebugObject obj) {
        List<V3d> vertices = obj.getVertices();
        if (vertices.size() < 2) {
            return;
        }

        Color lineColor = obj.getColor();
        Color vertexColor = obj.getVertexColor();
        double lineThickness = obj.getLineThickness();
        double pointThickness = obj.getPointThickness();

        // Рисуем линии полигона
        for (int i = 0; i < vertices.size(); i++) {
            V3d currentVertex = vertices.get(i);
            V3d nextVertex = vertices.get((i + 1) % vertices.size()); // Замыкаем полигон

            setColor(lineColor);
            renderLineAsBox(currentVertex, nextVertex, lineThickness);

            // Рисуем вершины если указан цвет
            if (obj.hasVertexColor()) {
                setColor(vertexColor);
                renderCube(currentVertex, pointThickness);
            }
        }
    }

    private void renderLineAsBox(V3d p0, V3d p1, double lineThickness) {
        // Центр параллелепипеда — середина между p0 и p1
        V3d center = V3d.midPoint(p0, p1);
        double length = p1.subtract(p0).magnitude();
        double height = lineThickness;

        // Ориентируем box вдоль направления p1-p0
        V3d dir = p1.subtract(p0).unit();

        gl.glPushMatrix();
        gl.glTranslated(center.getX(), center.getY(), center.getZ());

        // Поворачиваем относительно направления
        rotateToDirection(dir);

        // Рисуем прямоугольный параллелепипед
        renderBox(length, lineThickness, height);

        gl.glPopMatrix();
    }

    private void renderCube(V3d center, double size) {
        gl.glPushMatrix();
        gl.glTranslated(center.getX(), center.getY(), center.getZ());
        renderBox(size, size, size);
        gl.glPopMatrix();
    }

    private void renderBox(double width, double height, double depth) {
        double w = width / 2;
        double h = height / 2;
        double d = depth / 2;

        gl.glBegin(GL2.GL_QUADS);

        // Передняя грань
        gl.glNormal3f(0.0f, 0.0f, 1.0f);
        gl.glVertex3d(-w, -h, d);
        gl.glVertex3d(w, -h, d);
        gl.glVertex3d(w, h, d);
        gl.glVertex3d(-w, h, d);

        // Задняя грань
        gl.glNormal3f(0.0f, 0.0f, -1.0f);
        gl.glVertex3d(-w, -h, -d);
        gl.glVertex3d(-w, h, -d);
        gl.glVertex3d(w, h, -d);
        gl.glVertex3d(w, -h, -d);

        // Верхняя грань
        gl.glNormal3f(0.0f, 1.0f, 0.0f);
        gl.glVertex3d(-w, h, -d);
        gl.glVertex3d(-w, h, d);
        gl.glVertex3d(w, h, d);
        gl.glVertex3d(w, h, -d);

        // Нижняя грань
        gl.glNormal3f(0.0f, -1.0f, 0.0f);
        gl.glVertex3d(-w, -h, -d);
        gl.glVertex3d(w, -h, -d);
        gl.glVertex3d(w, -h, d);
        gl.glVertex3d(-w, -h, d);

        // Правая грань
        gl.glNormal3f(1.0f, 0.0f, 0.0f);
        gl.glVertex3d(w, -h, -d);
        gl.glVertex3d(w, h, -d);
        gl.glVertex3d(w, h, d);
        gl.glVertex3d(w, -h, d);

        // Левая грань
        gl.glNormal3f(-1.0f, 0.0f, 0.0f);
        gl.glVertex3d(-w, -h, -d);
        gl.glVertex3d(-w, -h, d);
        gl.glVertex3d(-w, h, d);
        gl.glVertex3d(-w, h, -d);

        gl.glEnd();
    }

    private void rotateToDirection(V3d dir) {
        V3d base = new V3d(1, 0, 0);

        // Если направление уже совпадает с базовым, поворот не нужен
        if (Math.abs(dir.getX() - 1.0) < 1e-6 && Math.abs(dir.getY()) < 1e-6 &&
            Math.abs(dir.getZ()) < 1e-6) {
            return;
        }

        // Если направление противоположно базовому
        if (Math.abs(dir.getX() + 1.0) < 1e-6 && Math.abs(dir.getY()) < 1e-6 &&
            Math.abs(dir.getZ()) < 1e-6) {
            gl.glRotated(180, 0, 1, 0);
            return;
        }

        V3d axis = base.cross(dir);
        double angle = Math.acos(Math.max(-1.0, Math.min(1.0, base.dot(dir))));

        if (axis.magnitude() > 1e-6) {
            axis = axis.unit();
            gl.glRotated(Math.toDegrees(angle), axis.getX(), axis.getY(), axis.getZ());
        }
    }

    private void setColor(Color color) {
        float r = (float) color.getRed() / 255.0f;
        float g = (float) color.getGreen() / 255.0f;
        float b = (float) color.getBlue() / 255.0f;
        float a = (float) color.getAlpha() / 255.0f;
        gl.glColor4f(r, g, b, a);
    }
}
