package com.github.grishberg.javascad.models.surfaces;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.models.SurfaceStrategy;
import java.util.ArrayList;
import java.util.List;

public class S12x3 {
    private final V3d[][] controlPoints;
    private int outWidth;
    private int outHeight;

    // Количество контрольных точек по осям
    private static final int U_POINTS = 12;
    private static final int V_POINTS = 3;

    public S12x3(V3d[][] points) {
        this.controlPoints = points;
        validateControlPoints();
        outWidth = 0;
        outHeight = 0;
    }

    private void validateControlPoints() {
        if (controlPoints.length != V_POINTS ||
            controlPoints[0].length != U_POINTS) {
            throw new IllegalArgumentException(
                "Control points grid must be " + V_POINTS + "x" + U_POINTS
            );
        }
    }

    public List<V3d> buildSurface(int resolution) {
        List<V3d> surface = new ArrayList<>();

        // Рассчитываем количество сегментов между контрольными точками
        final int uSegments = U_POINTS - 1;
        final int vSegments = V_POINTS - 1;

        // Рассчитываем шаг с учетом промежуточных точек
        final double uStep = 1.0 / (uSegments * (resolution + 1));
        final double vStep = 1.0 / (vSegments * (resolution + 1));

        for (double v = 0; v <= 1.0 + 1e-9; v += vStep) {
            outWidth = 0;
            for (double u = 0; u <= 1.0 + 1e-9; u += uStep) {
                V3d[] curvePoints = new V3d[U_POINTS];
                for (int i = 0; i < U_POINTS; i++) {
                    curvePoints[i] = bezierPoint(
                        v,
                        controlPoints[0][i],
                        controlPoints[1][i],
                        controlPoints[2][i]
                    );
                }
                V3d point = bezierPoint(u, curvePoints);
                surface.add(point);
                outWidth++;
            }
            outHeight++;
        }

        return surface;
    }

    public SurfaceStrategy buildSurfaceStrategy(int resolution) {
        final List<V3d> points = buildSurface(resolution);
        return new SurfaceStrategy() {
            @Override
            public Result buildSurface() {
                return new Result(points, outWidth, outHeight);
            }
        };
    }

    private static V3d bezierPoint(double t, V3d... points) {
        t = Math.max(0.0, Math.min(1.0, t)); // Clamp to [0,1]
        int degree = points.length - 1;
        double[] basis = new double[degree + 1];
        double x = 0, y = 0, z = 0;

        for (int i = 0; i <= degree; i++) {
            basis[i] = binomialCoefficient(degree, i)
                * Math.pow(1 - t, degree - i)
                * Math.pow(t, i);
            x += basis[i] * points[i].getX();
            y += basis[i] * points[i].getY();
            z += basis[i] * points[i].getZ();
        }
        return new V3d(x, y, z);
    }

    private static int binomialCoefficient(int n, int k) {
        if (k > n - k) k = n - k;
        int result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - k + i) / i;
        }
        return result;
    }

    public static S12x3 create(V3d[][] points) {
        return new S12x3(points);
    }
}