package eu.printingin3d.javascad.models.surfaces;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.SurfaceStrategy;
import java.util.ArrayList;
import java.util.List;

public class S6x3_mod {

	private final V3d[][] controlPoints;
    private int outWidth;
    private int outHeight;


    public S6x3_mod(V3d[][] points) {
		this.controlPoints = points;
        outWidth = 0;
        outHeight = 0;
    }

    public List<V3d> buildSurface(int resolution) {
        List<V3d> surfacePoints = new ArrayList<>();

        int m = 5; // Степень по u (6 точек => 5 степень)
        int n = 2; // Степень по v (3 точки => 2 степень)

        // Итерация по параметрам u и v с заданным шагом
        for (double v = 0; v <= 1; v += 1.0 / resolution) {
            outWidth++;
            outHeight = 0;
            for (double u = 0; u <= 1; u += 1.0 / resolution) {
                outHeight++;

                V3d point = computeSurfacePoint(controlPoints, u, v, m, n);
                surfacePoints.add(point);
            }
        }

        return surfacePoints;
    }

    private static double adjustStep(double current, double step) {
        current += step;
        return current > 1.0 ? 1.0 + step : current; // Гарантирует выход из цикла при достижении 1.0
    }

    private static V3d computeSurfacePoint(V3d[][] grid, double u, double v, int w, int h) {
        double x = 0.0, y = 0.0, z = 0.0;
        for (int i = 0; i <= w; i++) {
            double bu = bernstein(w, i, u);
            for (int j = 0; j <= h; j++) {
                double bv = bernstein(h, j, v);
                V3d p = grid[j][i];
                x += bu * bv * p.x;
                y += bu * bv * p.y;
                z += bu * bv * p.z;
            }
        }
        return new V3d(x, y, z);
    }

    private static double bernstein(int degree, int i, double t) {
        return binomial(degree, i) * Math.pow(t, i) * Math.pow(1 - t, degree - i);
    }

    private static long binomial(int n, int k) {
        if (k < 0 || k > n) return 0;
        if (k == 0 || k == n) return 1;
        long result = 1;
        for (int i = 1; i <= k; i++) {
            result = result * (n - i + 1) / i;
        }
        return result;
    }

    public SurfaceStrategy buildSurfaceStrategy(int resolution){
        final List<V3d> points  = buildSurface(resolution);
        return new SurfaceStrategy(){
            @Override
            public Result buildSurface() {
                return new Result(points, outWidth, outHeight);
            }
        };
    }


    public static S6x3_mod s6x3(V3d[][] points){
		return new S6x3_mod(points);
	}
    
}
