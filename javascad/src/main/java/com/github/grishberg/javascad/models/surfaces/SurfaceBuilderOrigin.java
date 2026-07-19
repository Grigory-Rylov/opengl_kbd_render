package com.github.grishberg.javascad.models.surfaces;

import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.models.Abstract3dModel;
import com.github.grishberg.javascad.models.Atomic3dModel;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.vrl.CSG;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import com.github.grishberg.javascad.vrl.Polygon;

import java.util.ArrayList;
import java.util.List;

public class SurfaceBuilderOrigin extends Atomic3dModel {
    private final V3d[][] surfacePoints;
    private final double thickness;
    private Color color;
    double minX, maxX, minY, maxY, minZ, maxZ;


    public SurfaceBuilderOrigin(V3d[][] surfacePoints, double thickness) {
        minX = minY = minZ = Double.POSITIVE_INFINITY;
        maxX = maxY = maxZ = Double.NEGATIVE_INFINITY;

        validateSurface(surfacePoints);
        this.surfacePoints = surfacePoints;
        this.thickness = thickness;
        this.color = Color.YELLOW;
    }

    @Override
    protected Abstract3dModel innerCloneModel() {
        return new SurfaceBuilderOrigin(surfacePoints, thickness);
    }

    @Override
    protected Boundaries3d getModelBoundaries() {
        if (minX != Double.POSITIVE_INFINITY && maxX != Double.NEGATIVE_INFINITY) {
            return new Boundaries3d(
                new Boundary(minX, maxX),
                new Boundary(minY, maxY),
                new Boundary(minZ, maxZ)
            );
        }

        minX = minY = minZ = Double.POSITIVE_INFINITY;
        maxX = maxY = maxZ = Double.NEGATIVE_INFINITY;

        for (V3d[] outerArr : surfacePoints) {
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

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        throw new UnsupportedOperationException("Surface is not supported in native mode");
    }

    protected CSG toInnerCSG(FacetGenerationContext context) {
        color = context.getColor();
        List<Polygon> polygons = buildSurface();

        return new CSG(polygons);
    }

    public List<Polygon> buildSurface() {
        List<Polygon> polygons = new ArrayList<>();

        // Генерируем верхнюю поверхность
        generateTopSurface(polygons);

        // Генерируем нижнюю поверхность
        V3d[][] bottomSurface = generateBottomSurface();
        generateBottomSurface(polygons, bottomSurface);

        // Генерируем боковые грани
        generateSideFaces(polygons, bottomSurface);

        return polygons;
    }

    private void generateTopSurface(List<Polygon> polygons) {
        final int height = surfacePoints.length;
        final int width = surfacePoints[0].length;

        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                V3d v1 = surfacePoints[y][x];
                V3d v2 = surfacePoints[y][x + 1];
                V3d v3 = surfacePoints[y + 1][x + 1];
                V3d v4 = surfacePoints[y + 1][x];

                addQuadAsTriangles(polygons, v1, v2, v3, v4, color);
            }
        }
    }

    private V3d[][] generateBottomSurface() {
        final int height = surfacePoints.length;
        final int width = surfacePoints[0].length;
        V3d[][] bottom = new V3d[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                V3d normal = calculateNormal(y, x);
                bottom[y][x] = surfacePoints[y][x].subtract(normal.mul(thickness));
            }
        }
        return bottom;
    }

    private void generateBottomSurface(List<Polygon> polygons, V3d[][] bottom) {
        final int height = bottom.length;
        final int width = bottom[0].length;

        for (int y = 0; y < height - 1; y++) {
            for (int x = 0; x < width - 1; x++) {
                V3d v1 = bottom[y][x];
                V3d v2 = bottom[y][x + 1];
                V3d v3 = bottom[y + 1][x + 1];
                V3d v4 = bottom[y + 1][x];

                // Инвертируем порядок для правильной ориентации нормалей
                addQuadAsTriangles(polygons, v3, v2, v1, v4, color);
            }
        }
    }

    private void generateSideFaces(List<Polygon> polygons, V3d[][] bottom) {
        final int height = surfacePoints.length;
        final int width = surfacePoints[0].length;

        // Боковые грани по периметру
        generateVerticalEdges(polygons, bottom, 0);      // Левая грань
        generateVerticalEdges(polygons, bottom, width - 1); // Правая грань
        generateHorizontalEdges(polygons, bottom, 0);    // Задняя грань
        generateHorizontalEdges(polygons, bottom, height - 1); // Передняя грань
    }

    private void generateVerticalEdges(List<Polygon> polygons, V3d[][] bottom, int x) {
        final int height = surfacePoints.length;

        for (int y = 0; y < height - 1; y++) {
            V3d top1 = surfacePoints[y][x];
            V3d bottom1 = bottom[y][x];
            V3d top2 = surfacePoints[y + 1][x];
            V3d bottom2 = bottom[y + 1][x];

            addQuadAsTriangles(polygons, top1, top2, bottom2, bottom1, color);
        }
    }

    private void generateHorizontalEdges(List<Polygon> polygons, V3d[][] bottom, int y) {
        final int width = surfacePoints[0].length;

        for (int x = 0; x < width - 1; x++) {
            V3d top1 = surfacePoints[y][x];
            V3d top2 = surfacePoints[y][x + 1];
            V3d bottom1 = bottom[y][x];
            V3d bottom2 = bottom[y][x + 1];

            addQuadAsTriangles(polygons, top2, top1, bottom1, bottom2, color);
        }
    }

    private V3d calculateNormal(int y, int x) {
        final int height = surfacePoints.length;
        final int width = surfacePoints[0].length;

        V3d right = x < width - 1 ? surfacePoints[y][x + 1] : surfacePoints[y][x];
        V3d left = x > 0 ? surfacePoints[y][x - 1] : surfacePoints[y][x];
        V3d down = y < height - 1 ? surfacePoints[y + 1][x] : surfacePoints[y][x];
        V3d up = y > 0 ? surfacePoints[y - 1][x] : surfacePoints[y][x];

        V3d dx = right.subtract(left);
        V3d dy = down.subtract(up);

        return dx.cross(dy).unit();
    }

    private static void addQuadAsTriangles(
        List<Polygon> polygons,
        V3d v1, V3d v2, V3d v3, V3d v4,
        Color color
    ) {
        polygons.add(Polygon.fromPolygons(v1, v2, v3, color));
        polygons.add(Polygon.fromPolygons(v1, v3, v4, color));
    }

    private void validateSurface(V3d[][] surface) {
        if (surface == null || surface.length < 2) {
            throw new IllegalArgumentException("Surface must have at least 2 rows");
        }
        int cols = surface[0].length;
        for (V3d[] row : surface) {
            if (row.length != cols || row.length < 2) {
                throw new IllegalArgumentException("Invalid surface matrix dimensions");
            }
        }
    }
}