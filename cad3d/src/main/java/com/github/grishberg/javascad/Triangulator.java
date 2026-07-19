package com.github.grishberg.javascad;

import static com.github.grishberg.javascad.vrl.Const.EPSILON;

import com.github.grishberg.javascad.optimizator.CrossEdgeValidator;
import com.github.grishberg.javascad.coords.EarClippingMod;
import com.github.grishberg.javascad.coords.Triangle3d;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.vrl.Facet;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.List;

public class Triangulator {

    public static List<Triangle3d> triangulate(List<V3d> vertices, V3d normal) {
        return EarClippingMod.triangulate(vertices, normal);
    }

    public static List<Triangle3d> triangulate0(List<V3d> vertices, V3d normal) {
        List<Triangle3d> result = new ArrayList<>();

        if (vertices.size() == 3) {
            result.add(new Triangle3d(vertices.get(0), vertices.get(1), vertices.get(2), normal));
            return result;
        }
        ArrayList<V3d> work = new ArrayList<>(vertices);

        int i = 1;

        while (work.size() >= 3) {
            int size = work.size();
            int prevIndex = (i - 1) % size;
            int currentIndex = i % size;
            int nextIndex = (i + 1) % size;
            int check1 = (i + 2) % size;
            int check2 = (i + 3) % size;

            V3d a = work.get(prevIndex);
            V3d b = work.get(currentIndex);
            V3d c = work.get(nextIndex);
            V3d d = work.get(check1);
            V3d e = work.get(check2);

            if (isValidTriangle(a, b, c) && !CrossEdgeValidator.isPointBetween(d, e, c)) {
                result.add(new Triangle3d(a, b, c, normal));
                work.remove(currentIndex);
                continue;
            }
            i++;
        }

        return result;
    }

    public static List<Facet> triangulate(Polygon polygon) {
        List<Facet> facets = new ArrayList<>();
        List<Triangle3d> triangle3ds =
            EarClippingMod.triangulate(polygon.getVertices(), polygon.getNormal());
        for (Triangle3d triangle : triangle3ds) {
            facets.add(new Facet(triangle, polygon.getNormal(), polygon.getColor()));
        }

        return facets;
    }

    public static List<Facet> triangulate(List<Polygon> polygons) {
        List<Facet> facetsFromPolygons = new ArrayList<>();

        for (Polygon p : polygons) {
            List<Triangle3d> currentPolygonTriangles =
                Triangulator.triangulate(p.getVertices(), p.getNormal());

            List<Facet> localFacet = new ArrayList<>();
            for (Triangle3d t : currentPolygonTriangles) {
                facetsFromPolygons.add(new Facet(t, p.getNormal(), p.getColor()));
                localFacet.add(new Facet(t, p.getNormal(), p.getColor()));
            }

        }

        return facetsFromPolygons;
    }

    private static boolean hasNoPointsBetween(List<V3d> vertices, int startPos, int modEnd) {
        V3d startPoint = vertices.get(modEnd);
        V3d endPoint = vertices.get(startPos);

        for (int i = modEnd + 1; i < vertices.size(); i++) {
            V3d currentPoint = vertices.get(i);
            if (CrossEdgeValidator.isPointBetween(currentPoint, startPoint, endPoint)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Проверяет, является ли угол выпуклым
     *
     * @param prev предыдущая вершина
     * @param curr текущая вершина
     * @param next следующая вершина
     * @return true, если угол выпуклый
     */
    static boolean isValidTriangle1(V3d prev, V3d curr, V3d next) {
        V3d edge1 = prev.subtract(curr);
        V3d edge2 = next.subtract(curr);
        V3d cross = edge1.cross(edge2);

        // Предполагаем, что полигон лежит в плоскости XY
        // Для правильной ориентации Z-компонента должна быть положительной
        return cross.getZ() > 0;
    }

    /**
     * Проверяет, являются ли три точки вершинами правильного треугольника.
     *
     * @param a первая точка
     * @param b вторая точка
     * @param c третья точка
     * @return true, если точки образуют правильный треугольник, false иначе
     */
    static boolean isValidTriangle(V3d a, V3d b, V3d c) {
        // Сначала проверяем, что точки не лежат на одной прямой
        if (areCollinear(a, b, c)) {
            return false;
        }

        return true;
    }

    /**
     * Проверяет, лежат ли три точки на одной прямой (коллинеарны).
     */
    private static boolean areCollinear(V3d a, V3d b, V3d c) {
        // Вектор AB
        double abX = b.x - a.x;
        double abY = b.y - a.y;
        double abZ = b.z - a.z;

        // Вектор AC
        double acX = c.x - a.x;
        double acY = c.y - a.y;
        double acZ = c.z - a.z;

        // Векторное произведение AB × AC
        double crossX = abY * acZ - abZ * acY;
        double crossY = abZ * acX - abX * acZ;
        double crossZ = abX * acY - abY * acX;

        // Если векторное произведение близко к нулю, точки коллинеарны

        return Math.abs(crossX) < EPSILON &&
            Math.abs(crossY) < EPSILON &&
            Math.abs(crossZ) < EPSILON;
    }

    /**
     * Вычисляет расстояние между двумя точками.
     */
    private static double distance(V3d p1, V3d p2) {
        double dx = p2.x - p1.x;
        double dy = p2.y - p1.y;
        double dz = p2.z - p1.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
