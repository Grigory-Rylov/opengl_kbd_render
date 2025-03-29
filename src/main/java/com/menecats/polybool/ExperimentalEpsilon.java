package com.menecats.polybool;

import static com.menecats.polybool.helpers.PolyBoolHelper.point;

import com.menecats.polybool.models.Point2d;

public class ExperimentalEpsilon extends Epsilon {
    public ExperimentalEpsilon() {
        super();
    }

    public ExperimentalEpsilon(double eps) {
        super(eps);
    }

    @Override
    public boolean pointAboveOrOnLine(Point2d pt, Point2d left, Point2d right) {
        final double Ax = left.x;
        final double Ay = left.y;
        final double Bx = right.x;
        final double By = right.y;
        final double Cx = pt.x;
        final double Cy = pt.y;
        final double ABx = Bx - Ax;
        final double ABy = By - Ay;
        final double AB = Math.sqrt(ABx * ABx + ABy * ABy);
        // algebraic distance of 'pt' to ('left', 'right') line is:
        // [ABx * (Cy - Ay) - ABy * (Cx - Ax)] / AB
        return ABx * (Cy - Ay) - ABy * (Cx - Ax) >= -eps * AB;
    }

    @Override
    public boolean pointBetween(Point2d p, Point2d left, Point2d right) {
        // p must be collinear with left->right
        // returns false if p == left, p == right, or left == right
        if (pointsSame(p, left) || pointsSame(p, right)) return false;
        final double d_py_ly = p.y - left.y;
        final double d_rx_lx = right.x - left.x;
        final double d_px_lx = p.x - left.x;
        final double d_ry_ly = right.y - left.y;

        double dot = d_px_lx * d_rx_lx + d_py_ly * d_ry_ly;
        // dot < 0 is p is to the left of 'left'
        if (dot < 0) return false;
        final double sqlen = d_rx_lx * d_rx_lx + d_ry_ly * d_ry_ly;
        // dot <= sqlen is p is to the left of 'right'
        return dot <= sqlen;
    }

    @Override
    public boolean pointsCollinear(Point2d pt1, Point2d pt2, Point2d pt3) {
        // does pt1->pt2->pt3 make a straight line?
        // essentially this is just checking to see if the slope(pt1->pt2) === slope(pt2->pt3)
        // if slopes are equal, then they must be collinear, because they share pt2
        final double dx1 = pt1.x - pt2.x;
        final double dy1 = pt1.y - pt2.y;
        final double dx2 = pt2.x - pt3.x;
        final double dy2 = pt2.y - pt3.y;
        final double n1 = Math.sqrt(dx1 * dx1 + dy1 * dy1);
        final double n2 = Math.sqrt(dx2 * dx2 + dy2 * dy2);
        // Assuming det(u, v) = 0, we have:
        // |det(u + u_err, v + v_err)| = |det(u + u_err, v + v_err) - det(u,v)|
        // =|det(u, v_err) + det(u_err. v) + det(u_err, v_err)|
        // <= |det(u, v_err)| + |det(u_err, v)| + |det(u_err, v_err)|
        // <= N(u)N(v_err) + N(u_err)N(v) + N(u_err)N(v_err)
        // <= eps * (N(u) + N(v) + eps)
        // We have N(u) ~ N(u + u_err) and N(v) ~ N(v + v_err).
        // Assuming eps << N(u) and eps << N(v), we end with:
        // |det(u + u_err, v + v_err)| <= eps * (N(u + u_err) + N(v + v_err))
        return Math.abs(dx1 * dy2 - dx2 * dy1) <= eps * (n1 + n2);
    }

    @Override
    public EpsilonIntersectionResult linesIntersect(Point2d a0, Point2d a1, Point2d b0, Point2d b1) {
        // returns false if the lines are coincident (e.g., parallel or on top of each other)
        //
        // returns an object if the lines intersect:
        //   {
        //     pt: [x, y],    where the intersection point is at
        //     alongA: where intersection point is along A,
        //     alongB: where intersection point is along B
        //   }
        //
        //  alongA and alongB will each be one of: -2, -1, 0, 1, 2
        //
        //  with the following meaning:
        //
        //    -2   intersection point is before segment's first point
        //    -1   intersection point is directly on segment's first point
        //     0   intersection point is between segment's first and second points (exclusive)
        //     1   intersection point is directly on segment's second point
        //     2   intersection point is after segment's second point
        final double adx = a1.x - a0.x;
        final double ady = a1.y - a0.y;
        final double bdx = b1.x - b0.x;
        final double bdy = b1.y - b0.y;

        final double axb = adx * bdy - ady * bdx;
        final double n1 = Math.sqrt(adx * adx + ady * ady);
        final double n2 = Math.sqrt(bdx * bdx + bdy * bdy);
        if (Math.abs(axb) <= eps * (n1 + n2))
            return null; // lines are coincident

        final double dx = a0.x - b0.x;
        final double dy = a0.y - b0.y;

        final double A = (bdx * dy - bdy * dx) / axb;
        final double B = (adx * dy - ady * dx) / axb;
        final Point2d pt = point(
                a0.x + A * adx,
                a0.y + A * ady
        );

        final EpsilonIntersectionResult ret = new EpsilonIntersectionResult();
        ret.pt = pt;

        // categorize where intersection point is along A and B
        if (pointsSame(pt, a0))
            ret.alongA = -1;
        else if (pointsSame(pt, a1))
            ret.alongA = 1;
        else if (A < 0)
            ret.alongA = -2;
        else if (A > 1)
            ret.alongA = 2;

        if (pointsSame(pt, b0))
            ret.alongB = -1;
        else if (pointsSame(pt, b1))
            ret.alongB = 1;
        else if (B < 0)
            ret.alongB = -2;
        else if (B > 1)
            ret.alongB = 2;

        return ret;
    }
}
