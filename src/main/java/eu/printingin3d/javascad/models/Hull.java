package eu.printingin3d.javascad.models;

import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import eu.printingin3d.javascad.context.IColorGenerationContext;
import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Hull extends Atomic3dModel {

    private final List<Abstract3dModel> models;

    public Hull(Abstract3dModel... obj) {
        this.models = new ArrayList<>();
        Collections.addAll(models, obj);
    }

    public Hull(List<Abstract3dModel> obj) {
        this.models = new ArrayList<>();
        models.addAll(obj);
    }

    @Override
    protected Boundaries3d getModelBoundaries() {
        ArrayList<Boundaries3d> boundaries3ds = new ArrayList<>();
        for (Abstract3dModel model : models) {
            boundaries3ds.add(model.getBoundaries());
        }
        return Boundaries3d.combine(boundaries3ds);
    }

    @Override
    protected Abstract3dModel innerCloneModel() {
        return new Hull(models);
    }

    @Override
    protected SCAD innerToScad(IColorGenerationContext context) {
        return null;
    }

    @Override
    protected CSG toInnerCSG(FacetGenerationContext context) {
        List<V3d> points = new ArrayList<>();

        // Собираем все точки из всех CSG объектов
        for (Abstract3dModel model : models) {
            for (Polygon polygon : model.toCSG(context).getPolygons()) {
                points.addAll(polygon.getVertices());
            }
        }

        // Удаляем дубликаты точек
        List<Polygon> hullPolygons = generateHull(context, points);
        return new CSG(hullPolygons);
    }

    public static List<Polygon> generateHull(
        FacetGenerationContext context,
        List<V3d> points
    ) {
        points = new ArrayList<>(new HashSet<>(points));

        // Выполняем алгоритм QuickHull
        QuickHull3D hull = new QuickHull3D(points);
        List<Polygon> hullPolygons = new ArrayList<>();

        Point3d[] vertices = hull.getVertices();
        int[][] faces = hull.getFaces();

        for (int[] face : faces) {
            if (face.length == 3) {
                // Если грань уже треугольник, просто добавляем его
                hullPolygons.add(Polygon.fromPolygons(
                    vertices[face[0]].toCoords3d(),
                    vertices[face[1]].toCoords3d(),
                    vertices[face[2]].toCoords3d(),
                    context.getColor()
                ));
            } else {
                // Если грань - многоугольник, разбиваем его на треугольники
                Point3d firstPoint = vertices[face[0]];
                for (int i = 1; i < face.length - 1; i++) {
                    hullPolygons.add(Polygon.fromPolygons(
                        firstPoint.toCoords3d(),
                        vertices[face[i]].toCoords3d(),
                        vertices[face[i + 1]].toCoords3d(),
                        context.getColor()
                    ));
                }
            }
        }
        return hullPolygons;
    }

    private static List<Polygon> quickHull(List<V3d> points, FacetGenerationContext context) {
        if (points.size() < 4) {
            throw new IllegalArgumentException("At least 4 points are required to compute a 3D " +
                "hull");
        }

        // Находим начальный тетраэдр
        V3d p0 = points.get(0);
        V3d p1 = findFurthestPoint(points, p0);
        V3d p2 = findFurthestFromLine(points, p0, p1);
        V3d p3 = findFurthestFromPlane(points, p0, p1, p2, context);

        // Создаем начальные грани
        List<Polygon> hull = new ArrayList<>();

        hull.add(Polygon.fromPolygons(p0, p1, p2, context.getColor()));
        hull.add(Polygon.fromPolygons(p0, p2, p3, context.getColor()));
        hull.add(Polygon.fromPolygons(p0, p3, p1, context.getColor()));
        hull.add(Polygon.fromPolygons(p1, p3, p2, context.getColor()));

        // Рекурсивно добавляем точки
        List<V3d> remainingPoints = new ArrayList<>(points);
        remainingPoints.removeAll(Arrays.asList(p0, p1, p2, p3));

        for (Polygon face : hull) {
            addPointsToFace(remainingPoints, face, hull, context);
        }

        return hull;
    }

    private static void addPointsToFace(
        List<V3d> points,
        Polygon face,
        List<Polygon> hull,
        FacetGenerationContext context
    ) {
        if (points.isEmpty()) {
            return;
        }

        V3d furthestPoint = null;
        double maxDistance = Double.NEGATIVE_INFINITY;

        for (V3d point : points) {
            double distance = distanceFromPlane(point, face);
            if (distance > maxDistance) {
                maxDistance = distance;
                furthestPoint = point;
            }
        }

        if (furthestPoint == null || maxDistance <= 0) {
            return;
        }

        List<Polygon> visibleFaces = new ArrayList<>();
        for (Polygon polygon : hull) {
            if (distanceFromPlane(furthestPoint, polygon) > 0) {
                visibleFaces.add(polygon);
            }
        }

        List<V3d> horizon = findHorizon(visibleFaces);

        // Удаляем видимые грани
        hull.removeAll(visibleFaces);

        // Добавляем новые грани
        List<Polygon> newFaces = new ArrayList<>();
        for (int i = 0; i < horizon.size(); i++) {
            V3d p1 = horizon.get(i);
            V3d p2 = horizon.get((i + 1) % horizon.size());
            newFaces.add(Polygon.fromPolygons(furthestPoint, p1, p2, context.getColor()));
        }
        hull.addAll(newFaces);

        // Рекурсивно обрабатываем новые грани
        points.remove(furthestPoint);
        for (Polygon newFace : newFaces) {
            addPointsToFace(points, newFace, hull, context);
        }
    }

    private static List<V3d> findHorizon(List<Polygon> visibleFaces) {
        Set<Edge> edges = new HashSet<>();
        for (Polygon face : visibleFaces) {
            for (int i = 0; i < 3; i++) {
                Edge edge =
                    new Edge(face.getVertices().get(i), face.getVertices().get((i + 1) % 3));
                if (edges.contains(edge)) {
                    edges.remove(edge);
                } else {
                    edges.add(edge);
                }
            }
        }
        List<V3d> horizon = new ArrayList<>();
        Edge start = edges.iterator().next();
        horizon.add(start.a);
        horizon.add(start.b);
        edges.remove(start);
        while (!edges.isEmpty()) {
            for (Edge edge : edges) {
                if (edge.a.equals(horizon.get(horizon.size() - 1))) {
                    horizon.add(edge.b);
                    edges.remove(edge);
                    break;
                }
                if (edge.b.equals(horizon.get(horizon.size() - 1))) {
                    horizon.add(edge.a);
                    edges.remove(edge);
                    break;
                }
            }
        }
        return horizon;
    }

    private static class Edge {

        V3d a, b;

        Edge(V3d a, V3d b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Edge)) {
                return false;
            }
            Edge other = (Edge) obj;
            return (a.equals(other.a) && b.equals(other.b)) ||
                (a.equals(other.b) && b.equals(other.a));
        }

        @Override
        public int hashCode() {
            return a.hashCode() + b.hashCode();
        }
    }

    private static V3d findFurthestPoint(List<V3d> points, V3d from) {
        V3d furthest = null;
        double maxDistanceSquared = Double.NEGATIVE_INFINITY;
        for (V3d point : points) {
            double distanceSquared = distanceSquared(from, point);
            if (distanceSquared > maxDistanceSquared) {
                maxDistanceSquared = distanceSquared;
                furthest = point;
            }
        }
        return furthest;
    }


    private static V3d findFurthestFromLine(List<V3d> points, V3d a, V3d b) {
        V3d furthest = null;
        double maxDistance = Double.NEGATIVE_INFINITY;
        for (V3d point : points) {
            double distance = distanceFromLine(point, a, b);
            if (distance > maxDistance) {
                maxDistance = distance;
                furthest = point;
            }
        }
        return furthest;
    }

    private static V3d findFurthestFromPlane(
        List<V3d> points,
        V3d a,
        V3d b,
        V3d c,
        FacetGenerationContext context
    ) {
        V3d furthest = null;
        double maxDistance = Double.NEGATIVE_INFINITY;
        for (V3d point : points) {
            double distance = Math.abs(distanceFromPlane(
                point,
                Polygon.fromPolygons(a, b, c, context.getColor())
            ));
            if (distance > maxDistance) {
                maxDistance = distance;
                furthest = point;
            }
        }
        return furthest;
    }

    private static double distanceSquared(V3d a, V3d b) {
        double dx = b.getX() - a.getX();
        double dy = b.getY() - a.getY();
        double dz = b.getZ() - a.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static double distanceFromLine(V3d point, V3d a, V3d b) {
        V3d ab = subtract(b, a);
        V3d ap = subtract(point, a);
        V3d cross = crossProduct(ab, ap);
        return magnitude(cross) / magnitude(ab);
    }

    private static double distanceFromPlane(V3d point, Polygon plane) {
        V3d normal = getNormal(plane);
        V3d v = subtract(point, plane.getVertices().get(0));
        return dotProduct(normal, v);
    }

    private static V3d getNormal(Polygon plane) {
        List<V3d> vertices = plane.getVertices();
        V3d v1 = subtract(vertices.get(1), vertices.get(0));
        V3d v2 = subtract(vertices.get(2), vertices.get(0));
        return normalize(crossProduct(v1, v2));
    }

    private static V3d subtract(V3d a, V3d b) {
        return new V3d(a.getX() - b.getX(), a.getY() - b.getY(), a.getZ() - b.getZ());
    }

    private static V3d crossProduct(V3d a, V3d b) {
        return new V3d(
            a.getY() * b.getZ() - a.getZ() * b.getY(),
            a.getZ() * b.getX() - a.getX() * b.getZ(),
            a.getX() * b.getY() - a.getY() * b.getX()
        );
    }

    private static double dotProduct(V3d a, V3d b) {
        return a.getX() * b.getX() + a.getY() * b.getY() + a.getZ() * b.getZ();
    }

    private static double magnitude(V3d v) {
        return Math.sqrt(v.getX() * v.getX() + v.getY() * v.getY() + v.getZ() * v.getZ());
    }

    private static V3d normalize(V3d v) {
        double mag = magnitude(v);
        return new V3d(v.getX() / mag, v.getY() / mag, v.getZ() / mag);
    }
}
