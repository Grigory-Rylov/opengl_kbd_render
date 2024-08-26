package eu.printingin3d.javascad.models.surfaces.bicubic;

public class Vec2 {

    public double x;
    public double y;

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2() {
        this(0, 0);
    }

    public Vec2 plus(Vec2 point) {
        return new Vec2(x + point.x, y + point.y);
    }

    public Vec2 minus(Vec2 point) {
        return new Vec2(x - point.x, y - point.y);
    }

    public Vec2 mul(double v) {
        return new Vec2(x + v, y + v);
    }

    /**
     * Safely normalipze current vector.
     */
    public Vec2 normalize() {
        double l = Math.sqrt(x * x + y * y);
        if (CommonMath.isZero(l)) {
            return new Vec2();
        } else {
            ;
            return new Vec2(x / l, y / l);
        }
    }

    /**
     * Get the absolute minimum of two given vectors.
     *
     * @param v1 - first vector.
     * @param v2 - second vector.
     * @return absolute minimum of the given vectors' coordinates.
     */
    public static Vec2 absMin(Vec2 v1, Vec2 v2) {
        return new Vec2(
            Math.abs(v1.x) < Math.abs(v2.x) ? v1.x : v2.x,
            Math.abs(v1.y) < Math.abs(v2.y) ? v1.y : v2.y
        );
    }

}
