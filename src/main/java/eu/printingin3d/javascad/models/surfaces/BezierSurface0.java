package eu.printingin3d.javascad.models.surfaces;


import eu.printingin3d.javascad.coords.V3d;
import java.util.ArrayList;
import java.util.List;

public class BezierSurface0 {

    static class Segment {

        V3d[] points = new V3d[4];

        V3d calc(double t) {
            double t2 = t * t;
            double t3 = t2 * t;
            double nt = 1.0 - t;
            double nt2 = nt * nt;
            double nt3 = nt2 * nt;
            double x =
                nt3 * points[0].x + 3.0 * t * nt2 * points[1].x + 3.0 * t2 * nt * points[2].x +
                    t3 * points[3].x;
            double y =
                nt3 * points[0].y + 3.0 * t * nt2 * points[1].y + 3.0 * t2 * nt * points[2].y +
                    t3 * points[3].y;
            double z =
                nt3 * points[0].z + 3.0 * t * nt2 * points[1].z + 3.0 * t2 * nt * points[2].z +
                    t3 * points[3].z;
            return new V3d(x, y, z);
        }
    }

    private static final double EPSILON = 1.0e-5;
    private final V3d[][] controlPoints;

    public BezierSurface0(V3d[][] points) {
        this.controlPoints = points;
    }


    public List<V3d> buildSurface(int resolution) {
        List<V3d> surface = new ArrayList<>();
        int rows = controlPoints.length;
        int cols = controlPoints[0].length;

        for (int i = 0; i < rows - 1; i++) {
            List<Segment> uCurve = calculateSpline(controlPoints[i]);
            List<Segment> vCurve = calculateSpline(controlPoints[i + 1]);

            for (double u = 0; u <= 1; u += 1.0 / resolution) {
                for (double v = 0; v <= 1; v += 1.0 / resolution) {
                    V3d p1 = uCurve.get((int) (u * (uCurve.size() - 1)))
                        .calc(u % (1.0 / (uCurve.size() - 1)) * (uCurve.size() - 1));
                    V3d p2 = vCurve.get((int) (v * (vCurve.size() - 1)))
                        .calc(v % (1.0 / (vCurve.size() - 1)) * (vCurve.size() - 1));
                    surface.add(new V3d((p1.x + p2.x) / 2, (p1.y + p2.y) / 2, (p1.z + p2.z) / 2));
                }
            }
        }

        return surface;
    }

    private static List<Segment> calculateSpline(V3d[] values) {
        int n = values.length - 1;
        if (n < 2) {
            return new ArrayList<>();
        }

        List<Segment> bezier = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            bezier.add(new Segment());
        }

        V3d tgL = new V3d(0, 0, 0);
        V3d tgR = new V3d(0, 0, 0);
        V3d cur;
        V3d next = values[1].subtract(values[0]);
        next = next.unit();

        double l1 = 0.0;
        double l2 = 0;
        double tmp = 0;
        double x = 0;

        n--;

        for (int i = 0; i < n; ++i) {
            bezier.get(i).points[0] = bezier.get(i).points[1] = values[i];
            bezier.get(i).points[2] = bezier.get(i).points[3] = values[i + 1];

            cur = next;
            next = values[i + 2].subtract(values[i + 1]);
            next = next.unit();

            tgL = tgR;

            tgR = new V3d(cur.x + next.x, cur.y + next.y, cur.z + next.z);
            tgR = tgR.unit();


			if (Math.abs(values[i + 1].y - values[i].y) < EPSILON) {
				l1 = l2 = 0.0;
			} else {
				tmp = values[i + 1].x - values[i].x;
				l1 = Math.abs(tgL.x) > EPSILON ? tmp / (2.0 * tgL.x) : 1.0;
				l2 = Math.abs(tgR.x) > EPSILON ? tmp / (2.0 * tgR.x) : 1.0;
			}

			if (Math.abs(tgL.x) > EPSILON && Math.abs(tgR.x) > EPSILON) {
				tmp = tgL.y / tgL.x - tgR.y / tgR.x;
				if (Math.abs(tmp) > EPSILON) {
					x = (values[i + 1].y - tgR.y / tgR.x * values[i + 1].x - values[i].y + tgL.y /
					 tgL.x * values[i].x) / tmp;
					if (x > values[i].x && x < values[i + 1].x) {
						if (tgL.y > 0.0) {
							if (l1 > l2)
								l1 = 0.0;
							else
								l2 = 0.0;
						} else {
							if (l1 < l2)
								l1 = 0.0;
							else
								l2 = 0.0;
						}
					}
				}
			}

            bezier.get(i).points[1] = new V3d(
                bezier.get(i).points[1].x + tgL.x * l1,
                bezier.get(i).points[1].y + tgL.y * l1,
                bezier.get(i).points[1].z + tgL.z * l1
            );
            bezier.get(i).points[2] = new V3d(
                bezier.get(i).points[2].x - tgR.x * l2,
                bezier.get(i).points[2].y - tgR.y * l2,
                bezier.get(i).points[2].z - tgR.z * l2
            );
        }

        l1 = Math.abs(tgL.x) > EPSILON ? (values[n + 1].x - values[n].x) / (2.0 * tgL.x) : 1.0;

        bezier.get(n).points[0] = bezier.get(n).points[1] = values[n];
        bezier.get(n).points[2] = bezier.get(n).points[3] = values[n + 1];
        bezier.get(n).points[1] = new V3d(
            bezier.get(n).points[1].x + tgR.x * l1,
            bezier.get(n).points[1].y + tgR.y * l1,
            bezier.get(n).points[1].z + tgR.z * l1
        );

        return bezier;
    }

}
