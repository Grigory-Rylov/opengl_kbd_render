package eu.printingin3d.javascad.models.surfaces;

import eu.printingin3d.javascad.context.IColorGenerationContext;
import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.coords.Boundary;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Atomic3dModel;
import eu.printingin3d.javascad.models.EdgeType;
import eu.printingin3d.javascad.models.SCAD;
import eu.printingin3d.javascad.utils.Color;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.List;

public class SmoothSurface extends Atomic3dModel {

    private final V3d[][] points;
    private final double thickness;
    private final int resolution;

    private final EdgeType frontEdgeType;
    private final EdgeType leftEdgeType;
    private final EdgeType backEdgeType;
    private final EdgeType rightEdgeType;

    public SmoothSurface(V3d[][] points, double thickness, int resolution) {
        this(
            points,
            thickness,
            resolution,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Normal
        );
    }

    public SmoothSurface(
        V3d[][] points, double thickness, int resolution,
        EdgeType frontEdgeType,
        EdgeType leftEdgeType,
        EdgeType backEdgeType,
        EdgeType rightEdgeType
    ) {
        this.thickness = thickness;
        this.points = points;
        this.resolution = resolution;
        this.frontEdgeType = frontEdgeType;
        this.leftEdgeType = leftEdgeType;
        this.backEdgeType = backEdgeType;
        this.rightEdgeType = rightEdgeType;
    }

    @Override
    protected CSG toInnerCSG(FacetGenerationContext context) {

        List<V3d> topSurface = generateSurface(points);
        List<V3d> bottomSurface = generateOffsetSurface(topSurface, -thickness);

        List<Polygon> polygons = new ArrayList<>();

        // Top faces
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                V3d v2 = topSurface.get(i * (resolution + 1) + j);
                V3d v1 = topSurface.get(i * (resolution + 1) + (j + 1));
                V3d v4 = topSurface.get((i + 1) * (resolution + 1) + (j + 1));
                V3d v3 = topSurface.get((i + 1) * (resolution + 1) + j);
                addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
            }
        }

        // Bottom faces
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                V3d v2 = bottomSurface.get(i * (resolution + 1) + j);
                V3d v1 = bottomSurface.get(i * (resolution + 1) + (j + 1));
                V3d v4 = bottomSurface.get((i + 1) * (resolution + 1) + (j + 1));
                V3d v3 = bottomSurface.get((i + 1) * (resolution + 1) + j);
                addQuadAsTriangles(polygons, v4, v3, v2, v1, context.getColor()); // Обратный порядок для правильной ориентации нормалей
            }
        }

        // Side faces
        // Side 1
        for (int i = 0; i < resolution; i++) {
            V3d v1 = topSurface.get(i);
            V3d v2 = topSurface.get(i + 1);
            V3d v3 = bottomSurface.get(i + 1);
            V3d v4 = bottomSurface.get(i);
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }

        // Side 2
        for (int i = 0; i < resolution; i++) {
            int j = i * (resolution + 1);
            V3d v1 = topSurface.get(j);
            V3d v2 = bottomSurface.get(j);
            V3d v3 = bottomSurface.get(j + resolution + 1);
            V3d v4 = topSurface.get(j + resolution + 1);
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }

        // Side 3
        for (int i = 0; i < resolution; i++) {
            int j = resolution;
            V3d v1 = topSurface.get(i * (resolution + 1) + j);
            V3d v2 = topSurface.get((i + 1) * (resolution + 1) + j);
            V3d v3 = bottomSurface.get((i + 1) * (resolution + 1) + j);
            V3d v4 = bottomSurface.get(i * (resolution + 1) + j);
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }

        // Side 4
        for (int i = 0; i < resolution; i++) {
            int j = resolution * (resolution + 1);
            V3d v2 = topSurface.get(j + i);
            V3d v1 = topSurface.get(j + i + 1);
            V3d v4 = bottomSurface.get(j + i + 1);
            V3d v3 = bottomSurface.get(j + i);
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }

        return new CSG(polygons);
    }

    private static void addQuadAsTriangles(
        List<Polygon> polygons,
        V3d v1, V3d v2, V3d v3, V3d v4,
        Color color
    ) {
        polygons.add(Polygon.fromPolygons(v1, v2, v3, color));
        polygons.add(Polygon.fromPolygons(v1, v3, v4, color));
    }


    private static V3d bezierPoint(
        double t,
        V3d p0,
        V3d p1,
        V3d p2,
        V3d p3
    ) {
        double x = Math.pow(1 - t, 3) * p0.getX() + 3 * Math.pow(1 - t, 2) * t * p1.getX() +
            3 * (1 - t) * Math.pow(t, 2) * p2.getX() + Math.pow(t, 3) * p3.getX();
        double y = Math.pow(1 - t, 3) * p0.getY() + 3 * Math.pow(1 - t, 2) * t * p1.getY() +
            3 * (1 - t) * Math.pow(t, 2) * p2.getY() + Math.pow(t, 3) * p3.getY();
        double z = Math.pow(1 - t, 3) * p0.getZ() + 3 * Math.pow(1 - t, 2) * t * p1.getZ() +
            3 * (1 - t) * Math.pow(t, 2) * p2.getZ() + Math.pow(t, 3) * p3.getZ();
        return new V3d(x, y, z);
    }

    private List<V3d> generateSurface(
        V3d[][] controlPoints
    ) {
        List<V3d> surface = new ArrayList<>();
        for (double u = 0; u <= 1; u += 1.0 / resolution) {
            for (double v = 0; v <= 1; v += 1.0 / resolution) {
                V3d p0 = bezierPoint(
                    v,
                    controlPoints[0][0],
                    controlPoints[0][1],
                    controlPoints[0][2],
                    controlPoints[0][3]
                );
                V3d p1 = bezierPoint(
                    v,
                    controlPoints[1][0],
                    controlPoints[1][1],
                    controlPoints[1][2],
                    controlPoints[1][3]
                );
                V3d p2 = bezierPoint(
                    v,
                    controlPoints[2][0],
                    controlPoints[2][1],
                    controlPoints[2][2],
                    controlPoints[2][3]
                );
                V3d p3 = bezierPoint(
                    v,
                    controlPoints[3][0],
                    controlPoints[3][1],
                    controlPoints[3][2],
                    controlPoints[3][3]
                );
                V3d point = bezierPoint(u, p0, p1, p2, p3);
                surface.add(new V3d(point.getX(), point.getY(), point.getZ()));
            }
        }
        return surface;
    }

    private List<V3d> generateOffsetSurface(List<V3d> surface, double thickness) {
        List<V3d> offsetSurface = new ArrayList<>();
        int resolution = (int) Math.sqrt(surface.size());

        for (int y = 0; y < resolution; y++) {
            for (int x = 0; x < resolution; x++) {
                if(true){
                    processInnerPoints(surface, thickness, offsetSurface, resolution, y, x);
                    continue;
                }
                if (y == 0) {
                    processFrontBackWall(
                        surface,
                        thickness,
                        offsetSurface,
                        resolution,
                        y,
                        x,
                        backEdgeType
                    );
                } else if (y == resolution - 1) {
                    processFrontBackWall(
                        surface,
                        thickness,
                        offsetSurface,
                        resolution,
                        y,
                        x,
                        frontEdgeType
                    );
                } else if (x == 0 && (y < resolution - 1)) {
                    processLeftRightWall(
                        surface,
                        thickness,
                        offsetSurface,
                        resolution,
                        y,
                        x,
                        leftEdgeType
                    );
                } else if (x == resolution - 1) {
                    processLeftRightWall(
                        surface,
                        thickness,
                        offsetSurface,
                        resolution,
                        y,
                        x,
                        rightEdgeType
                    );
                } else {
                    processInnerPoints(surface, thickness, offsetSurface, resolution, y, x);
                }

            }
        }

        return offsetSurface;
    }

    private static void processInnerPoints(
        List<V3d> surface,
        double thickness,
        List<V3d> offsetSurface,
        int resolution,
        int i,
        int j
    ) {
        V3d normal = calculateNormal(surface, i, j, resolution);
        V3d point = surface.get(i * resolution + j);
        offsetSurface.add(new V3d(
            point.getX() - normal.getX() * thickness,
            point.getY() - normal.getY() * thickness,
            point.getZ() - normal.getZ() * thickness
        ));
    }

    private void processFrontBackWall(
        List<V3d> surface,
        double thickness,
        List<V3d> offsetSurface,
        int resolution,
        int i,
        int j,
        EdgeType edgeType
    ) {
        V3d normal = calculateNormal(surface, i, j, resolution);
        V3d point = surface.get(i * resolution + j);
        V3d targetPoint;
        switch (edgeType) {
            case Vertical:
                int direction = normal.getZ() < 0 ? 1 : -1;
                targetPoint = new V3d(
                    point.getX(),
                    point.getY(),
                    point.getZ() + direction * thickness
                );
                break;
            case HorizontalX:
                direction = normal.getX() > 0 ? 1 : -1;
                targetPoint = new V3d(
                    point.getX() + direction * thickness,
                    point.getY(),
                    point.getZ()
                );
                break;
            case HorizontalY:
                direction = normal.getY() > 0 ? -1 : 1;
                targetPoint = new V3d(
                    point.getX(),
                    point.getY() + direction * thickness,
                    point.getZ()
                );
                break;
            default:
                targetPoint = new V3d(
                    point.getX() - normal.getX() * thickness,
                    point.getY() - normal.getY() * thickness,
                    point.getZ() - normal.getZ() * thickness
                );
        }

        offsetSurface.add(targetPoint);
    }

    private void processLeftRightWall(
        List<V3d> surface,
        double thickness,
        List<V3d> offsetSurface,
        int resolution,
        int i,
        int j,
        EdgeType edgeType
    ) {
        V3d normal = calculateNormal(surface, i, j, resolution);
        V3d point = surface.get(i * resolution + j);
        V3d targetPoint;
        switch (edgeType) {
            case Vertical:
                int direction = normal.getZ() < 0 ? 1 : -1;
                targetPoint = new V3d(
                    point.getX(),
                    point.getY(),
                    point.getZ() + direction * thickness
                );
                break;
            case HorizontalX:
                direction = normal.getX() > 0 ? 1 : -1;
                targetPoint = new V3d(
                    point.getX() + direction * thickness,
                    point.getY(),
                    point.getZ()
                );
                break;
            case HorizontalY:
                direction = normal.getY() > 0 ? 1 : -1;
                targetPoint = new V3d(
                    point.getX(),
                    point.getY() + direction * thickness,
                    point.getZ()
                );
                break;
            default:
                targetPoint = new V3d(
                    point.getX() - normal.getX() * thickness,
                    point.getY() - normal.getY() * thickness,
                    point.getZ() - normal.getZ() * thickness
                );
        }

        offsetSurface.add(targetPoint);
    }

    private static V3d calculateNormal(List<V3d> surface, int i, int j, int resolution) {
        V3d center = surface.get(i * resolution + j);
        V3d right = (j < resolution - 1) ? surface.get(i * resolution + j + 1) : center;
        V3d left = (j > 0) ? surface.get(i * resolution + j - 1) : center;
        V3d up = (i > 0) ? surface.get((i - 1) * resolution + j) : center;
        V3d down = (i < resolution - 1) ? surface.get((i + 1) * resolution + j) : center;

        V3d dx = right.subtract(left);
        V3d dy = down.subtract(up);

        V3d normal = dx.cross(dy);
        return normal.unit();
    }

    private static V3d normalize(V3d v) {
        double length = Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if (length == 0) {
            return new V3d(0, 0, 0);
        }
        return new V3d(v.x / length, v.y / length, v.z / length);
    }

    @Override
    protected Abstract3dModel innerCloneModel() {
        return new SmoothSurface(points, thickness, resolution,
            frontEdgeType,
            leftEdgeType,
            backEdgeType,
            rightEdgeType
        );
    }

    @Override
    protected SCAD innerToScad(IColorGenerationContext context) {
        return null;
    }


    @Override
    protected Boundaries3d getModelBoundaries() {
        double minX, maxX, minY, maxY, minZ, maxZ;

        minX = minY = minZ = Double.POSITIVE_INFINITY;
        maxX = maxY = maxZ = Double.NEGATIVE_INFINITY;

        for (V3d[] outerArr : points) {
            for (V3d point : outerArr) {
                // Обновляем минимальные значения
                minX = Math.min(minX, point.getX());
                minY = Math.min(minY, point.getY());
                minZ = Math.min(minZ, point.getZ());

                // Обновляем максимальные значения
                maxX = Math.max(maxX, point.getX());
                maxY = Math.max(maxY, point.getY());
                maxZ = Math.max(maxZ, point.getZ());
            }

        }

        return new Boundaries3d(
            new Boundary(minX, maxX),
            new Boundary(minY, maxY),
            new Boundary(minZ, maxZ)
        );
    }
}
