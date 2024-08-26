package eu.printingin3d.javascad.models.surfaces.bicubic;

class CurveBuilder {

    static boolean build(Vec2[] values, Segment[] curve, int startIndex, double c) {
        int n = values.length - 1;

        if (n < 2) {
            return false;
        }

        Vec2 cur, next, tgL, deltaC;
        Vec2 tgR = new Vec2();
        double l1, l2, tmp, x;
        boolean zL, zR;

        next = values[1].minus(values[0]);
        next.normalize();

        for (int i = 0; i < n; ++i) {
            tgL = tgR;
            cur = next;

            deltaC = values[i + 1].minus(values[i]);

            if (i < n - 1) {
                next = values[i + 2].minus(values[i + 1]);
                next.normalize();
                tgR = cur.plus(next);
                tgR.normalize();
            } else {
                tgR.x = tgR.y = 0.0;
            }

            if (CommonMath.sign(tgL.x) != CommonMath.sign(deltaC.x)) {
                tgL.x = 0.0;
            }
            if (CommonMath.sign(tgL.y) != CommonMath.sign(deltaC.y)) {
                tgL.y = 0.0;
            }
            if (CommonMath.sign(tgR.x) != CommonMath.sign(deltaC.x)) {
                tgR.x = 0.0;
            }
            if (CommonMath.sign(tgR.y) != CommonMath.sign(deltaC.y)) {
                tgR.y = 0.0;
            }

            zL = CommonMath.isZero(tgL.x);
            zR = CommonMath.isZero(tgR.x);

            l1 = zL ? 0.0 : deltaC.x / (c * tgL.x);
            l2 = zR ? 0.0 : deltaC.x / (c * tgR.x);

            if (Math.abs(l1 * tgL.y) > Math.abs(deltaC.y)) {
                l1 = CommonMath.isZero(tgL.y) ? 0.0 : deltaC.y / tgL.y;
            }
            if (Math.abs(l2 * tgR.y) > Math.abs(deltaC.y)) {
                l2 = CommonMath.isZero(tgR.y) ? 0.0 : deltaC.y / tgR.y;
            }

            if (!zL && !zR) {
                tmp = tgL.y / tgL.x - tgR.y / tgR.x;
                if (!CommonMath.isZero(tmp)) {
                    x = (values[i + 1].y - tgR.y / tgR.x * values[i + 1].x - values[i].y +
                        tgL.y / tgL.x * values[i].x) / tmp;
                    if (x > values[i].x && x < values[i + 1].x) {
                        if (Math.abs(l1) > Math.abs(l2)) {
                            l1 = 0.0;
                        } else {
                            l2 = 0.0;
                        }
                    }
                }
            }

            curve[startIndex + i].points[0] = values[i];
            curve[startIndex + i].points[1] = curve[startIndex + i].points[0].plus(tgL.mul(l1));
            curve[startIndex + i].points[3] = values[i + 1];
            curve[startIndex + i].points[2] = curve[startIndex + i].points[3].minus(tgR.mul(l2));
        }

        return true;
    }
}
