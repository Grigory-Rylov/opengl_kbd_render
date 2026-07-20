package com.github.grishberg.javascad.models.surfaces;

import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.coords.Boundary;
import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Atomic3dModel;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.vrl.CSG;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import com.github.grishberg.javascad.vrl.Polygon;
import java.util.ArrayList;
import java.util.List;

public class SurfaceBuilder extends Atomic3dModel {

    private final V3d[][] points;
    private final double thickness;
    private Color color = Color.BLUE;

    public SurfaceBuilder(V3d[][] points, double thickness) {
        this.points = points;
        this.thickness = thickness;
    }


    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        throw new UnsupportedOperationException("Surface is not supported in native mode");
    }

    protected CSG toInnerCSG(FacetGenerationContext context) {
        color = context.getColor();
        List<Polygon> polygons = build(context.getColor());

        return new CSG(polygons);
    }

    private List<Polygon> build(Color color) {
        List<Polygon> polygons = new ArrayList<>();

        // Рассчитываем нормали для всех точек
        V3d[][] normals = calculateNormals();

        // Создаем верхний и нижний слои
        V3d[][] upper = offsetPoints(normals, 0);
        V3d[][] lower = offsetPoints(normals, -thickness);

        // Строим полигоны для верхнего слоя
        buildLayer(polygons, upper, true, color);
        // Строим полигоны для нижнего слоя
        buildLayer(polygons, lower, false, color);
        // Строим боковые грани
        buildSideWalls(polygons, upper, lower);

        return polygons;
    }


    private V3d[][] calculateNormals() {
        int rows = points.length;
        int cols = points[0].length;
        V3d[][] normals = new V3d[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                // Вычисляем касательные векторы
                V3d tangentU = (j < cols - 1 ? points[i][j + 1] : points[i][j])
                    .subtract(j > 0 ? points[i][j - 1] : points[i][j]);

                V3d tangentV = (i < rows - 1 ? points[i + 1][j] : points[i][j])
                    .subtract(i > 0 ? points[i - 1][j] : points[i][j]);

                // Нормаль как векторное произведение
                normals[i][j] = tangentU.cross(tangentV).unit();
            }
        }
        return normals;
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

    private V3d[][] offsetPoints(V3d[][] normals, double offset) {
        int rows = points.length;
        int cols = points[0].length;
        V3d[][] result = new V3d[rows][cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                result[i][j] = points[i][j].add(normals[i][j].mul(offset));
            }
        }
        return result;
    }

    private void buildLayer(List<Polygon> polygons, V3d[][] layer, boolean isTop, Color color) {
        int rows = layer.length;
        int cols = layer[0].length;

        for (int i = 0; i < rows - 1; i++) {
            for (int j = 0; j < cols - 1; j++) {
                V3d v1 = layer[i][j];
                V3d v2 = layer[i][j + 1];
                V3d v3 = layer[i + 1][j + 1];
                V3d v4 = layer[i + 1][j];

                // Меняем порядок для нижнего слоя
                if (!isTop) {
                    addQuadAsTriangles(polygons, v4, v3, v2, v1, color);
                } else {
                    addQuadAsTriangles(polygons, v1, v2, v3, v4, color);
                }
            }
        }
    }

    private void buildSideWalls(List<Polygon> polygons, V3d[][] upper, V3d[][] lower) {
        int rows = upper.length;
        int cols = upper[0].length;

        // Левая и правая границы
        for (int i = 0; i < rows - 1; i++) {
            // Левая грань
            addQuadAsTriangles(polygons,
                upper[i][0], upper[i + 1][0],
                lower[i + 1][0], lower[i][0],
                color
            );

            // Правая грань
            addQuadAsTriangles(polygons,
                upper[i][cols - 1], upper[i + 1][cols - 1],
                lower[i + 1][cols - 1], lower[i][cols - 1],
                color
            );
        }

        // Верхняя и нижняя границы
        for (int j = 0; j < cols - 1; j++) {
            // Верхняя грань
            addQuadAsTriangles(polygons,
                upper[0][j], upper[0][j + 1],
                lower[0][j + 1], lower[0][j],
                color
            );

            // Нижняя грань
            addQuadAsTriangles(polygons,
                upper[rows - 1][j], upper[rows - 1][j + 1],
                lower[rows - 1][j + 1], lower[rows - 1][j],
                color
            );
        }
    }

    private static void addQuadAsTriangles(
        List<Polygon> polygons,
        V3d v1, V3d v2, V3d v3, V3d v4,
        Color color
    ) {
        polygons.add(Polygon.fromPolygons(v1, v2, v3, color));
        polygons.add(Polygon.fromPolygons(v1, v3, v4, color));
    }

    @Override
    protected Model innerCloneModel() {
        return new SurfaceBuilder(points, thickness);
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
