package eu.printingin3d.javascad.models.surfaces;


import eu.printingin3d.javascad.coords.V3d;
import java.util.ArrayList;
import java.util.List;

public class BezierSurface {

    private final V3d[][] controlPoints;

    public BezierSurface(V3d[][] points) {
        this.controlPoints = points;
    }


    public List<V3d> buildSurface(int resolution) {
        List<V3d> surface = new ArrayList<>();

        for (double v = 0; v <= 1; v += 1.0 / resolution) {
            for (double u = 0; u <= 1; u += 1.0 / resolution) {
                V3d point = lagrangeSurfacePoint(u, v);
                surface.add(new V3d(point.getX(), point.getY(), point.getZ()));
            }
        }

        return surface;
    }

    private V3d lagrangeSurfacePoint(double u, double v) {
        int n = controlPoints.length - 1;
        int m = controlPoints[0].length - 1;

        double x = 0.0;
        double y = 0.0;
        double z = 0.0;

        for (int i = 0; i <= n; i++) {
            for (int j = 0; j <= m; j++) {
                double basisU = lagrangeBasis(i, n, u);
                double basisV = lagrangeBasis(j, m, v);

                V3d controlPoint = controlPoints[i][j];

                x += basisU * basisV * controlPoint.getX();
                y += basisU * basisV * controlPoint.getY();
                z += basisU * basisV * controlPoint.getZ();
            }
        }

        return new V3d(x, y, z);
    }

    private double lagrangeBasis(int i, int n, double t) {
        double basis = 1.0;

        for (int j = 0; j <= n; j++) {
            if (j != i) {
                double numerator = t - (double) j / n;
                double denominator = (double) i / n - (double) j / n;
                basis *= numerator / denominator;
            }
        }

        return basis;
    }
}
