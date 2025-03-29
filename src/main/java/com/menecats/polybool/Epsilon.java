package com.menecats.polybool;

import static com.menecats.polybool.helpers.PolyBoolHelper.point;

import com.menecats.polybool.models.Point2d;
import java.util.List;

public class Epsilon {
    public static class EpsilonIntersectionResult {
        public int alongA;
        public int alongB;
        public Point2d pt;
    }

    protected double eps;

    public Epsilon() {
        this(1e-10);
    }

    public Epsilon(double eps) {
        this.eps = eps;
    }

    public double epsilon(double eps) {
        return (this.eps = Math.abs(eps));
    }

    public boolean pointAboveOrOnLine(Point2d pt, Point2d left, Point2d right) {
        double Ax = left.x;
        double Ay = left.y;
        double Bx = right.x;
        double By = right.y;
        double Cx = pt.x;
        double Cy = pt.y;

        return (Bx - Ax) * (Cy - Ay) - (By - Ay) * (Cx - Ax) >= -this.eps;
    }

    public boolean pointBetween(Point2d p, Point2d left, Point2d right) {
        // p must be collinear with left->right
        // returns false if p == left, p == right, or left == right
        double d_py_ly = p.y - left.y;
        double d_rx_lx = right.x - left.x;
        double d_px_lx = p.x - left.x;
        double d_ry_ly = right.y - left.y;

        double dot = d_px_lx * d_rx_lx + d_py_ly * d_ry_ly;
        // if `dot` is 0, then `p` == `left` or `left` == `right` (reject)
        // if `dot` is less than 0, then `p` is to the left of `left` (reject)
        if (dot < this.eps)
            return false;

        double sqlen = d_rx_lx * d_rx_lx + d_ry_ly * d_ry_ly;
        // if `dot` > `sqlen`, then `p` is to the right of `right` (reject)
        // therefore, if `dot - sqlen` is greater than 0, then `p` is to the right of `right` (reject)
        return !(dot - sqlen > -this.eps);
    }

    public boolean pointsSameX(Point2d p1, Point2d p2) {
        return Math.abs(p1.x - p2.x) < this.eps;
    }

    public boolean pointsSameY(Point2d p1, Point2d p2) {
        return Math.abs(p1.y - p2.y) < this.eps;
    }

    public boolean pointsSame(Point2d p1, Point2d p2) {
        return this.pointsSameX(p1, p2) && this.pointsSameY(p1, p2);
    }

    public int pointsCompare(Point2d p1, Point2d p2) {
        // returns -1 if p1 is smaller, 1 if p2 is smaller, 0 if equal
        if (this.pointsSameX(p1, p2))
            return this.pointsSameY(p1, p2) ? 0 : (p1.y < p2.y ? -1 : 1);
        return p1.x < p2.x ? -1 : 1;
    }

    public boolean pointsCollinear(Point2d pt1, Point2d pt2, Point2d pt3) {
        // does pt1->pt2->pt3 make a straight line?
        // essentially this is just checking to see if the slope(pt1->pt2) === slope(pt2->pt3)
        // if slopes are equal, then they must be collinear, because they share pt2
        double dx1 = pt1.x - pt2.x;
        double dy1 = pt1.y - pt2.y;
        double dx2 = pt2.x - pt3.x;
        double dy2 = pt2.y - pt3.y;
        return Math.abs(dx1 * dy2 - dx2 * dy1) < this.eps;
    }

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
        double adx = a1.x - a0.x;
        double ady = a1.y - a0.y;
        double bdx = b1.x - b0.x;
        double bdy = b1.y - b0.y;

        double axb = adx * bdy - ady * bdx;
        if (Math.abs(axb) < this.eps)
            return null; // lines are coincident

        double dx = a0.x - b0.x;
        double dy = a0.y - b0.y;

        double A = (bdx * dy - bdy * dx) / axb;
        double B = (adx * dy - ady * dx) / axb;

        EpsilonIntersectionResult ret = new EpsilonIntersectionResult();
        ret.pt = point(
                a0.x + A * adx,
                a0.y + A * ady
        );

        // categorize where intersection point is along A and B

        if (A <= -this.eps)
            ret.alongA = -2;
        else if (A < this.eps)
            ret.alongA = -1;
        else if (A - 1 <= -this.eps)
            ret.alongA = 0;
        else if (A - 1 < this.eps)
            ret.alongA = 1;
        else
            ret.alongA = 2;

        if (B <= -this.eps)
            ret.alongB = -2;
        else if (B < this.eps)
            ret.alongB = -1;
        else if (B - 1 <= -this.eps)
            ret.alongB = 0;
        else if (B - 1 < this.eps)
            ret.alongB = 1;
        else
            ret.alongB = 2;

        return ret;
    }

    public boolean pointInsideRegion(Point2d pt, List<Point2d> region) {
        double x = pt.x;
        double y = pt.y;
        double last_x = region.get(region.size() - 1).x;
        double last_y = region.get(region.size() - 1).y;
        boolean inside = false;
        for (Point2d regionPt : region) {
            double curr_x = regionPt.x;
            double curr_y = regionPt.y;

            // if y is between curr_y and last_y, and
            // x is to the right of the boundary created by the line
            if ((curr_y - y > this.eps) != (last_y - y > this.eps) && (last_x - curr_x) * (y - curr_y) / (last_y - curr_y) + curr_x - x > this.eps)
                inside = !inside;

            last_x = curr_x;
            last_y = curr_y;
        }
        return inside;
    }
}
