package eu.printingin3d.javascad.models.surfaces;

import eu.printingin3d.javascad.coords.V3d;
import java.util.ArrayList;
import java.util.List;

public class S6x3 {

	private final V3d[][] controlPoints;

	public S6x3(V3d[][] points) {
		this.controlPoints = points;
	}


    public List<V3d> buildSurface(int resolution) {
        List<V3d> surface = new ArrayList<>();
        for (double v = 0; v <= 1; v += 1.0 / resolution) {
            for (double u = 0; u <= 1; u += 1.0 / resolution) {
                V3d[] curvePoints = new V3d[6];
                for (int i = 0; i < 6; i++) {
                    curvePoints[i] = bezierPoint(
                        u,
                        controlPoints[i][0],
                        controlPoints[i][1],
                        controlPoints[i][2]
                    );
                }
                V3d point = bezierPoint(v, curvePoints);
                surface.add(new V3d(point.getX(), point.getY(), point.getZ()));
            }
        }
        return surface;
    }

    private static V3d bezierPoint(double t, V3d... points) {
        int degree = points.length - 1;
        double oneMinusT = 1 - t;
        double[] basis = new double[degree + 1];
        double x = 0, y = 0, z = 0;

        for (int i = 0; i <= degree; i++) {
            basis[i] = binomialCoefficient(degree, i) * Math.pow(oneMinusT, degree - i) * Math.pow(t, i);
            x += basis[i] * points[i].getX();
            y += basis[i] * points[i].getY();
            z += basis[i] * points[i].getZ();
        }

        return new V3d(x, y, z);
    }

    private static int binomialCoefficient(int n, int k) {
        if (k == 0 || k == n) {
            return 1;
        }
        return binomialCoefficient(n - 1, k - 1) + binomialCoefficient(n - 1, k);
    }


    public static S6x3 s6x3(V3d[][] points){
		return new S6x3(points);
	}

    
}
