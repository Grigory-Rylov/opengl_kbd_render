package eu.printingin3d.javascad.models.surfaces;

import eu.printingin3d.javascad.context.IColorGenerationContext;
import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.coords.Boundary;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Atomic3dModel;
import eu.printingin3d.javascad.models.EdgeType;
import eu.printingin3d.javascad.models.SCAD;
import eu.printingin3d.javascad.models.SurfaceStrategy;
import eu.printingin3d.javascad.utils.Color;
import eu.printingin3d.javascad.vrl.CSG;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import eu.printingin3d.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.List;

public class SmoothSurface3 extends Atomic3dModel {

    private final SurfaceStrategy strategy;
    private final double thickness;

    private final EdgeType frontEdgeType;
    private final EdgeType leftEdgeType;
    private final EdgeType backEdgeType;
    private final EdgeType rightEdgeType;

    public SmoothSurface3(SurfaceStrategy strategy, double thickness) {
        this(
            strategy,
            thickness,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Normal
        );
    }

    public SmoothSurface3(
        SurfaceStrategy strategy, double thickness,
        EdgeType frontEdgeType,
        EdgeType leftEdgeType,
        EdgeType backEdgeType,
        EdgeType rightEdgeType
    ) {
        this.thickness = thickness;
        this.strategy = strategy;
        this.frontEdgeType = frontEdgeType;
        this.leftEdgeType = leftEdgeType;
        this.backEdgeType = backEdgeType;
        this.rightEdgeType = rightEdgeType;
    }

    @Override
    protected CSG toInnerCSG(FacetGenerationContext context) {
        SurfaceStrategy.Result topSurfaceResult = strategy.buildSurface();
        List<V3d> topSurface = topSurfaceResult.points;
        List<V3d> bottomSurface = generateOffsetSurface(topSurfaceResult, -thickness);
        List<Polygon> polygons = new ArrayList<>();
        final int width = topSurfaceResult.width;
        final int height = topSurfaceResult.height;

        // Top faces
        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                V3d v2 = topSurface.get(y * width + x);
                V3d v1 = topSurface.get(y * width + (x + 1));
                V3d v4 = topSurface.get((y + 1) * width + (x + 1));
                V3d v3 = topSurface.get((y + 1) * width + x);
                addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
            }
        }
        // Bottom faces
        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                V3d v2 = bottomSurface.get(y * width + x);
                V3d v1 = bottomSurface.get(y * width + (x + 1));
                V3d v4 = bottomSurface.get((y + 1) * width + (x + 1));
                V3d v3 = bottomSurface.get((y + 1) * width + x);
                addQuadAsTriangles(
                    polygons,
                    v4,
                    v3,
                    v2,
                    v1,
                    context.getColor()
                ); // Обратный порядок для правильной ориентации нормалей
            }
        }

        // Side faces
        // Side 1 back horizontal
        for (int x = 0; x < width - 1; x++) {
            V3d v1 = topSurface.get(x);
            V3d v2 = topSurface.get(x + 1);
            V3d v3 = bottomSurface.get(x + 1);
            V3d v4 = bottomSurface.get(x);
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }
        // Side 2
        // left?
        for (int y = 0; y < height - 1; y++) {
            int x = 0;
            V3d v1 = topSurface.get(y * width + x);
            V3d v2 = bottomSurface.get(y * width + x);
            V3d v3 = bottomSurface.get((y + 1) * width + x);
            V3d v4 = topSurface.get((y + 1) * width + x);
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }

        // Side 3
        // right
        for (int y = 0; y < height - 1; y++) {
            int x = width - 1;
            V3d v1 = topSurface.get(y * width + x);
            V3d v2 = topSurface.get((y + 1) * width + x);
            V3d v3 = bottomSurface.get((y + 1) * width + x);
            V3d v4 = bottomSurface.get(y * width + x);
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }

        // Side 4
        // bottom
        for (int x = 0; x < width - 1; x++) {
            int y = width * (height - 1);
            V3d v2 = topSurface.get(y + x);
            V3d v1 = topSurface.get(y + x + 1);
            V3d v4 = bottomSurface.get(y + x + 1);
            V3d v3 = bottomSurface.get(y + x);
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

    private List<V3d> generateOffsetSurface(
        SurfaceStrategy.Result surfaceResult,
        double thickness
    ) {
        List<V3d> offsetSurface = new ArrayList<>();
        List<V3d> surface = surfaceResult.points;

        int height = surfaceResult.height;
        int width = surfaceResult.width;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (y == 0) {
                    processFrontBackWall(
                        surface,
                        thickness,
                        offsetSurface,
                        width,
                        height,
                        y,
                        x,
                        backEdgeType
                    );
                } else if (y == height - 1) {
                    processFrontBackWall(
                        surface,
                        thickness,
                        offsetSurface,
                        width,
                        height,
                        y,
                        x,
                        frontEdgeType
                    );
                } else if (x == 0 && (y < height - 1)) {
                    processLeftRightWall(
                        surface,
                        thickness,
                        offsetSurface,
                        width,
                        height,
                        y,
                        x,
                        leftEdgeType
                    );
                } else if (x == width - 1) {
                    processLeftRightWall(
                        surface,
                        thickness,
                        offsetSurface,
                        width,
                        height,
                        y,
                        x,
                        rightEdgeType
                    );
                } else {
                    processInnerPoints(surface, thickness, offsetSurface,
                        width,
                        height,
                        y, x
                    );
                }
            }
        }

        return offsetSurface;
    }

    private static void processInnerPoints(
        List<V3d> surface,
        double thickness,
        List<V3d> offsetSurface,
        int width,
        int height,
        int y,
        int x
    ) {
        V3d normal = calculateNormal(surface, y, x, width, height);
        V3d point = surface.get(y * width + x);
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
        int width,
        int height,
        int y,
        int x,
        EdgeType edgeType
    ) {
        V3d normal = calculateNormal(surface, y, x, width, height);
        V3d point = surface.get(y * width + x);
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
        int width,
        int height,
        int i,
        int j,
        EdgeType edgeType
    ) {
        V3d normal = calculateNormal(surface, i, j, width, height);
        V3d point = surface.get(i * width + j);
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

    private static V3d calculateNormal(List<V3d> surface, int y, int x, int width, int height) {
        V3d center = surface.get(y * width + x);
        V3d right = (x < width - 1) ? surface.get(y * width + x + 1) : center;
        V3d left = (x > 0) ? surface.get(y * width + x - 1) : center;
        V3d up = (y > 0) ? surface.get((y - 1) * width + x) : center;
        V3d down = (y < height - 1) ? surface.get((y + 1) * width + x) : center;

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
        return new SmoothSurface3(strategy, thickness,
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
/*
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
*/
        return new Boundaries3d(
            new Boundary(minX, maxX),
            new Boundary(minY, maxY),
            new Boundary(minZ, maxZ)
        );
    }

}
