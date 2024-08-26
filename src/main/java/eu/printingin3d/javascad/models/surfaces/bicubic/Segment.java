package eu.printingin3d.javascad.models.surfaces.bicubic;

public class Segment {

    final Vec2[] points = new Vec2[4];

    /**
     * Calculate the intermediate curve points.
     *
     * @param t - parameter of the curve, should be in [0; 1].
     * @param regularize - flag determining if result should be calculated in a way that
     * x-coordinate is
     * linearly interpolated to ensure the segment subdivision points are distributed uniformly in
     * equal
     * distance from each other (true), or both coordinates should be interpolated cubically.
     * @return intermediate Bezier curve point that corresponds the given parameter.
     */
    public Vec2 calc(double t, boolean regularize) {
        if (regularize) {
            // We solve this by t to find out parameter giving regular grid:
            // x0 + t0 (x3 - x0) = (1 - t)^3 x0 + 3 t (1 - t)^2 x1 + 3 t^2 (1 - t) x2 + t^3 x3.
            double a = -points[0].x + 3.0 * (points[1].x - points[2].x) + points[3].x;
            double b = 3.0 * (points[0].x - 2.0 * points[1].x + points[2].x);
            double c = 3.0 * (-points[0].x + points[1].x);
            double d = t * (points[0].x - points[3].x);
            double[] roots = new double[4];
            int rn = CommonMath.solveCubicEq(a, b, c, d, roots);
            if (rn > 0) {
                double nearestRoot = roots[0];
                for (int i = 1; i < rn; ++i) {
                    if (roots[i] > 0.0 && roots[i] < 1.0 &&
                        Math.abs(t - roots[i]) < Math.abs(t - nearestRoot)) {
                        nearestRoot = roots[i];
                    }
                }
                if (nearestRoot > 0.0 && nearestRoot < 1.0) {
                    t = nearestRoot;
                } else {
                    rn = 0;
                }
            }
            if (rn == 0) {
                double t2 = t * t;
                double t3 = t2 * t;
                double nt = 1.0 - t;
                double nt2 = nt * nt;
                double nt3 = nt2 * nt;
                return new Vec2(
                    points[0].x + t * (points[3].x - points[0].x),
                    nt3 * points[0].y + 3.0 * t * nt2 * points[1].y + 3.0 * t2 * nt * points[2].y +
                        t3 * points[3].y
                );
            }
        }

        double t2 = t * t;
        double t3 = t2 * t;
        double nt = 1.0 - t;
        double nt2 = nt * nt;
        double nt3 = nt2 * nt;
        return new Vec2(
            nt3 * points[0].x + 3.0 * t * nt2 * points[1].x + 3.0 * t2 * nt * points[2].x +
                t3 * points[3].x,
            nt3 * points[0].y + 3.0 * t * nt2 * points[1].y + 3.0 * t2 * nt * points[2].y +
                t3 * points[3].y
        );
    }
}
