package eu.printingin3d.javascad.models.surfaces;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.SurfaceStrategy;
import java.util.ArrayList;
import java.util.List;

public class S6x3 {

	private final V3d[][] controlPoints;
    private int outWidth;
    private int outHeight;


    public S6x3(V3d[][] points) {
		this.controlPoints = points;
        outWidth = 0;
        outHeight = 0;
    }

    public List<V3d> buildSurface(int resolution) {
        List<V3d> surface = new ArrayList<>();

        for (double v = 0; v <= 1; v += 1.0 / resolution) {
            outWidth++;
            outHeight = 0;
            for (double u = 0; u <= 1; u += 1.0 / resolution) {
                outHeight++;
                V3d[] curvePoints = new V3d[6];
                for (int i = 0; i < 6; i++) {
                    curvePoints[i] = bezierPoint(
                        u,
                        controlPoints[0][i],
                        controlPoints[1][i],
                        controlPoints[2][i]
                    );
                }
                V3d point = bezierPoint(v, curvePoints);
                surface.add(new V3d(point.getX(), point.getY(), point.getZ()));
            }
        }

        return surface;
    }

    public SurfaceStrategy buildSurfaceStrategy(int resolution){
        final List<V3d> points  = buildSurface(resolution);
        int h = controlPoints.length;
        int w = controlPoints[0].length;
        return new SurfaceStrategy(){
            @Override
            public Result buildSurface() {
                return new Result(points, outWidth, outHeight);
            }
        };
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
