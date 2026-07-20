package com.github.grishberg.javascad.models.surfaces;

import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Atomic3dModel;
import com.github.grishberg.javascad.models.EdgeType;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.vrl.CSG;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Работает некорректно
 */
public class SmoothSurfaceBuilder extends Atomic3dModel {

    private final V3d[][] points;
    private final double thickness;

    private final EdgeType frontEdgeType;
    private final EdgeType leftEdgeType;
    private final EdgeType backEdgeType;
    private final EdgeType rightEdgeType;

    public SmoothSurfaceBuilder(V3d[][] points, double thickness) {
        this(
            points,
            thickness,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Normal
        );
    }

    public SmoothSurfaceBuilder(
        V3d[][] points, double thickness,
        EdgeType frontEdgeType,
        EdgeType leftEdgeType,
        EdgeType backEdgeType,
        EdgeType rightEdgeType
    ) {
        this.thickness = thickness;
        this.points = points;
        this.frontEdgeType = frontEdgeType;
        this.leftEdgeType = leftEdgeType;
        this.backEdgeType = backEdgeType;
        this.rightEdgeType = rightEdgeType;
    }

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        throw new UnsupportedOperationException("Surface is not supported in native mode");
    }

    protected CSG toInnerCSG(FacetGenerationContext context) {
        V3d[][] topSurfaceResult = points;
        V3d[][] topSurface = topSurfaceResult;
        V3d[][] bottomSurface = generateOffsetSurface(topSurface, -thickness);
        List<Polygon> polygons = new ArrayList<>();
        final int width = points.length;
        final int height = points[0].length;

        // Top faces
        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                V3d v2 = topSurface[y][x];
                V3d v1 = topSurface[y][x + 1];
                V3d v4 = topSurface[y + 1][x + 1];
                V3d v3 = topSurface[y + 1][x];
                addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
            }
        }

        // Bottom faces
        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                V3d v2 = topSurface[y][x];
                V3d v1 = topSurface[y][x + 1];
                V3d v4 = topSurface[y + 1][x + 1];
                V3d v3 = topSurface[y + 1][x];
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
            V3d v1 = topSurface[0][x];
            V3d v2 = topSurface[0][x + 1];
            V3d v3 = bottomSurface[0][x + 1];
            V3d v4 = bottomSurface[0][x];
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }
        // Side 2
        // left?
        for (int y = 0; y < height - 1; y++) {
            int x = 0;
            V3d v1 = topSurface[y][x];
            V3d v2 = bottomSurface[y][x];
            V3d v3 = bottomSurface[y + 1][x];
            V3d v4 = topSurface[y + 1][x];
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }

        // Side 3
        // right
        for (int y = 0; y < height - 1; y++) {
            int x = width - 1;
            V3d v1 = topSurface[y][x];
            V3d v2 = topSurface[y + 1][x];
            V3d v3 = bottomSurface[y + 1][x];
            V3d v4 = bottomSurface[y][x];
            addQuadAsTriangles(polygons, v1, v2, v3, v4, context.getColor());
        }

        // Side 4
        // bottom
        for (int x = 0; x < width - 1; x++) {
            int y = height - 1;
            V3d v2 = topSurface[y][x];
            V3d v1 = topSurface[y][x + 1];
            V3d v4 = bottomSurface[y][x + 1];
            V3d v3 = bottomSurface[y][x];
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

    private V3d[][] generateOffsetSurface(
        V3d[][] surface,
        double thickness
    ) {
        V3d[][] offsetSurface = new V3d[surface.length][];
        for (int i = 0; i < surface.length; i++) {
            offsetSurface[i] = Arrays.copyOf(surface[i], surface[i].length);
        }

        int height = surface.length;
        int width = surface[0].length;
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
        V3d[][] surface,
        double thickness,
        V3d[][] offsetSurface,
        int width,
        int height,
        int y,
        int x
    ) {
        V3d normal = calculateNormal(surface, y, x, width, height);
        V3d point = surface[y][x];
        offsetSurface[y][x] = new V3d(
            point.getX() - normal.getX() * thickness,
            point.getY() - normal.getY() * thickness,
            point.getZ() - normal.getZ() * thickness
        );
    }

    private void processFrontBackWall(
       V3d[][] surface,
        double thickness,
       V3d[][] offsetSurface,
        int width,
        int height,
        int y,
        int x,
        EdgeType edgeType
    ) {
        V3d normal = calculateNormal(surface, y, x, width, height);
        V3d point = surface[y][x];
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

        offsetSurface[y][x] = targetPoint;
    }

    private void processLeftRightWall(
        V3d[][] surface,
        double thickness,
        V3d[][] offsetSurface,
        int width,
        int height,
        int y,
        int x,
        EdgeType edgeType
    ) {
        V3d normal = calculateNormal(surface, y, x, width, height);
        V3d point = surface[y][x];
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

        offsetSurface[y][x] = targetPoint;
    }

    private static V3d calculateNormal(V3d[][] surface, int y, int x, int width, int height) {
        V3d center = surface[y][x];
        V3d right = (x < width - 1) ? surface[y][x + 1] : center;
        V3d left = (x > 0) ? surface[y][x - 1]: center;
        V3d up = (y > 0) ? surface[y - 1][x] : center;
        V3d down = (y < height - 1) ? surface[y + 1][x] : center;

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
    protected Model innerCloneModel() {
        return new SmoothSurfaceBuilder(points, thickness,
            frontEdgeType,
            leftEdgeType,
            backEdgeType,
            rightEdgeType
        );
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
