package eu.printingin3d.javascad.models.surfaces;

import eu.printingin3d.javascad.coords.V3d;
import java.util.ArrayList;
import java.util.List;

public class BSplineSurface {

    private V3d[][] controlPoints;
    private int degree = 3; // Степень сплайна (кубический)

    public BSplineSurface(V3d[][] controlPoints) {
        this.controlPoints = controlPoints;
    }


    public List<V3d> buildSurface(int resolution) {
        List<V3d> surface = new ArrayList<>();
        int m = controlPoints.length;
        int n = controlPoints[0].length;

        double[] knotsU = generateKnots(m);
        double[] knotsV = generateKnots(n);

        for (double v = 0; v <= 1; v += 1.0 / resolution) {
            for (double u = 0; u <= 1; u += 1.0 / resolution) {
                V3d point = evaluateSurface(u, v, knotsU, knotsV);
                surface.add(point);
            }
        }
        return surface;
    }

    private V3d evaluateSurface(double u, double v, double[] knotsU, double[] knotsV) {
        double x = 0, y = 0, z = 0;
        double totalWeight = 0;
        for (int i = 0; i < controlPoints.length; i++) {
            for (int j = 0; j < controlPoints[0].length; j++) {
                double basisU = bsplineBasis(i, degree, u, knotsU);
                double basisV = bsplineBasis(j, degree, v, knotsV);
                double weight = basisU * basisV;
                totalWeight += weight;
                x += controlPoints[i][j].getX() * weight;
                y += controlPoints[i][j].getY() * weight;
                z += controlPoints[i][j].getZ() * weight;
            }
        }
        if (totalWeight == 0) {
            return new V3d(0, 0, 0); // или другое подходящее значение по умолчанию
        }
        return new V3d(x / totalWeight, y / totalWeight, z / totalWeight);
    }


    private double bsplineBasis(int i, int k, double t, double[] knots) {
        if (k == 0) {
            return (t >= knots[i] && t < knots[i + 1]) ? 1.0 : 0.0;
        }

        double left = 0, right = 0;
        if (knots[i + k] - knots[i] != 0) {
            left = (t - knots[i]) / (knots[i + k] - knots[i]);
        }
        if (knots[i + k + 1] - knots[i + 1] != 0) {
            right = (knots[i + k + 1] - t) / (knots[i + k + 1] - knots[i + 1]);
        }

        return left * bsplineBasis(i, k - 1, t, knots) +
            right * bsplineBasis(i + 1, k - 1, t, knots);
    }

    private double[] generateKnots(int n) {
        int knotCount = n + degree + 1;
        double[] knots = new double[knotCount];
        for (int i = 0; i < knotCount; i++) {
            if (i <= degree) {
                knots[i] = 0;
            } else if (i >= n) {
                knots[i] = 1;
            } else {
                knots[i] = (double) (i - degree) / (n - degree);
            }
        }
        return knots;
    }

    public static BSplineSurface bSplineSurface(V3d[][] points) {
        return new BSplineSurface(points);
    }

}
