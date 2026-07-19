package com.github.grishberg.javascad.models.surfaces;

import com.github.grishberg.javascad.coords.V3d;

public class S6x3Linear {

    private final V3d[][] controlPoints;
    private final int resolution;

    public S6x3Linear(V3d[][] points, int resolution) {
        this.controlPoints = points;
        this.resolution = resolution;
    }

    public V3d[][] buildSurface() {
        int rows = controlPoints.length;
        int cols = controlPoints[0].length;

        int outRows = (rows - 1) * resolution + 1;
        int outCols = (cols - 1) * resolution + 1;
        V3d[][] surface = new V3d[outRows][outCols];

        // Интерполяция границ
        interpolateLeftRight(surface, outRows, outCols);
        interpolateTopBottom(surface, outRows, outCols);

        // Заполнение внутренних точек
        fillInternalPoints(surface, outRows, outCols);

        return surface;
    }

    private void interpolateLeftRight(V3d[][] surface, int outRows, int outCols) {
        // Левая граница (u=0)
        for (int i = 0; i < outRows; i++) {
            double v = (double) i / (outRows - 1);
            surface[i][0] = interpolateEdge(v, true, 0);
        }

        // Правая граница (u=1)
        for (int i = 0; i < outRows; i++) {
            double v = (double) i / (outRows - 1);
            surface[i][outCols - 1] = interpolateEdge(v, true, controlPoints[0].length - 1);
        }
    }

    private void interpolateTopBottom(V3d[][] surface, int outRows, int outCols) {
        // Верхняя граница (v=0)
        for (int j = 0; j < outCols; j++) {
            double u = (double) j / (outCols - 1);
            surface[0][j] = interpolateEdge(u, false, 0);
        }

        // Нижняя граница (v=1)
        for (int j = 0; j < outCols; j++) {
            double u = (double) j / (outCols - 1);
            surface[outRows - 1][j] = interpolateEdge(u, false, controlPoints.length - 1);
        }
    }

    private V3d interpolateEdge(double t, boolean isVertical, int fixedIndex) {
        t = Math.max(0, Math.min(1, t));
        int pointsCount = isVertical ? controlPoints.length : controlPoints[0].length;
        int segments = pointsCount - 1;

        int segment = (int) (t * segments);
        segment = Math.min(segment, segments - 1);
        double frac = t * segments - segment;

        if (isVertical) {
            V3d start = controlPoints[segment][fixedIndex];
            V3d end = controlPoints[segment + 1][fixedIndex];
            return lerp(start, end, frac);
        } else {
            V3d start = controlPoints[fixedIndex][segment];
            V3d end = controlPoints[fixedIndex][segment + 1];
            return lerp(start, end, frac);
        }
    }

    private void fillInternalPoints(V3d[][] surface, int outRows, int outCols) {
        int rows = controlPoints.length;
        int cols = controlPoints[0].length;

        for (int i = 1; i < outRows - 1; i++) {
            double v = (double) i / (outRows - 1);
            for (int j = 1; j < outCols - 1; j++) {
                double u = (double) j / (outCols - 1);

                V3d[] uCurve = new V3d[rows];
                for (int k = 0; k < rows; k++) {
                    uCurve[k] = bezierPoint(u, controlPoints[k]);
                }
                surface[i][j] = bezierPoint(v, uCurve);
            }
        }
    }

    private V3d lerp(V3d a, V3d b, double t) {
        return new V3d(
            a.getX() + t * (b.getX() - a.getX()),
            a.getY() + t * (b.getY() - a.getY()),
            a.getZ() + t * (b.getZ() - a.getZ())
        );
    }

    private static V3d bezierPoint(double t, V3d... points) {
        int n = points.length - 1;
        double x = 0, y = 0, z = 0;
        for (int i = 0; i <= n; i++) {
            double blend = binomialCoefficient(n, i)
                * Math.pow(1 - t, n - i)
                * Math.pow(t, i);
            x += points[i].getX() * blend;
            y += points[i].getY() * blend;
            z += points[i].getZ() * blend;
        }
        return new V3d(x, y, z);
    }

    private static int binomialCoefficient(int n, int k) {
       if (k < 0 || k > n) {
          return 0;
       }
       if (k == 0 || k == n) {
          return 1;
       }
        return binomialCoefficient(n - 1, k - 1) + binomialCoefficient(n - 1, k);
    }

    public static S6x3Linear create(V3d[][] controlPoints, int resolution) {
        return new S6x3Linear(controlPoints, resolution);
    }
}
