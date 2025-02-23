package eu.printingin3d.javascad.models.surfaces;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.SurfaceStrategy;
import java.util.ArrayList;
import java.util.List;

public class S5x5 {

	private final V3d[][] controlPoints;

	public S5x5(V3d[][] points) {
		this.controlPoints = points;
	}
	

	public List<V3d> buildSurface(int resolution) {

        List<V3d> surface = new ArrayList<>();
        for (double v = 0; v <= 1; v += 1.0 / resolution) {
            for (double u = 0; u <= 1; u += 1.0 / resolution) {
                V3d[] curvePoints = new V3d[5];
                for (int i = 0; i < 5; i++) {
                    curvePoints[i] = bezierPoint(
                        u,
                        controlPoints[i][0],
                        controlPoints[i][1],
                        controlPoints[i][2],
                        controlPoints[i][3],
                        controlPoints[i][4]
                    );
                }
                V3d point = bezierPoint(v, curvePoints[0], curvePoints[1], curvePoints[2], curvePoints[3], curvePoints[4]);
                surface.add(new V3d(point.getX(), point.getY(), point.getZ()));
            }
        }
        return surface;
    }

    public SurfaceStrategy buildSurfaceStrategy(int resolution){
        final List<V3d> points  = buildSurface(resolution);
        int h = controlPoints.length;
        int w = controlPoints[0].length;
        int outWidth = (resolution - 1) * (w - 1) + 1;
        int outHeight = (resolution - 1) * (h - 1) + 1;
        return new SurfaceStrategy(){
            @Override
            public Result buildSurface() {
                return new Result(points, outWidth, outHeight);
            }
        };
    }
	
	private static V3d bezierPoint0(double t, V3d p0, V3d p1, V3d p2, V3d p3, V3d p4) {
        double oneMinusT = 1 - t;
        double t2 = t * t;
        double t3 = t2 * t;
        double t4 = t3 * t;

        double b0 = Math.pow(oneMinusT, 4);
        double b1 = 4 * Math.pow(oneMinusT, 3) * t;
        double b2 = 6 * Math.pow(oneMinusT, 2) * t2;
        double b3 = 4 * oneMinusT * t3;
        double b4 = t4;

        double x = b0 * p0.getX() + b1 * p1.getX() + b2 * p2.getX() + b3 * p3.getX() + b4 * p4.getX();
        double y = b0 * p0.getY() + b1 * p1.getY() + b2 * p2.getY() + b3 * p3.getY() + b4 * p4.getY();
        double z = b0 * p0.getZ() + b1 * p1.getZ() + b2 * p2.getZ() + b3 * p3.getZ() + b4 * p4.getZ();

        return new V3d(x, y, z);
    }

    private static V3d bezierPoint(double t, V3d p0, V3d p1, V3d p2, V3d p3, V3d p4) {
        double oneMinusT = 1 - t;
        double t2 = t * t;
        double t3 = t2 * t;
        double t4 = t3 * t;

        // Весовые коэффициенты для каждой контрольной точки
        double w0 = 1.0;
        double w1 = 20.0; // Увеличенный вес для p1
        double w2 = 20.0; // Увеличенный вес для p2
        double w3 = 20.0; // Увеличенный вес для p3
        double w4 = 1.0;

        double b0 = w0 * Math.pow(oneMinusT, 4);
        double b1 = w1 * 4 * Math.pow(oneMinusT, 3) * t;
        double b2 = w2 * 6 * Math.pow(oneMinusT, 2) * t2;
        double b3 = w3 * 4 * oneMinusT * t3;
        double b4 = w4 * t4;

        double sumWeights = b0 + b1 + b2 + b3 + b4;

        double x = (b0 * p0.getX() + b1 * p1.getX() + b2 * p2.getX() + b3 * p3.getX() + b4 * p4.getX()) / sumWeights;
        double y = (b0 * p0.getY() + b1 * p1.getY() + b2 * p2.getY() + b3 * p3.getY() + b4 * p4.getY()) / sumWeights;
        double z = (b0 * p0.getZ() + b1 * p1.getZ() + b2 * p2.getZ() + b3 * p3.getZ() + b4 * p4.getZ()) / sumWeights;

        return new V3d(x, y, z);
    }

    private static V3d rationalBezierPoint(double t, V3d p0, V3d p1, V3d p2, V3d p3, V3d p4, double w0, double w1, double w2, double w3, double w4) {
        double oneMinusT = 1 - t;
        double t2 = t * t;
        double t3 = t2 * t;
        double t4 = t3 * t;

        double b0 = w0 * Math.pow(oneMinusT, 4);
        double b1 = w1 * 4 * Math.pow(oneMinusT, 3) * t;
        double b2 = w2 * 6 * Math.pow(oneMinusT, 2) * t2;
        double b3 = w3 * 4 * oneMinusT * t3;
        double b4 = w4 * t4;

        double sumWeights = b0 + b1 + b2 + b3 + b4;

        double x = (b0 * p0.getX() + b1 * p1.getX() + b2 * p2.getX() + b3 * p3.getX() + b4 * p4.getX()) / sumWeights;
        double y = (b0 * p0.getY() + b1 * p1.getY() + b2 * p2.getY() + b3 * p3.getY() + b4 * p4.getY()) / sumWeights;
        double z = (b0 * p0.getZ() + b1 * p1.getZ() + b2 * p2.getZ() + b3 * p3.getZ() + b4 * p4.getZ()) / sumWeights;

        return new V3d(x, y, z);
    }



    public static S5x5 s5x5(V3d[][] points){
		return new S5x5(points);
	}
	

    
}
