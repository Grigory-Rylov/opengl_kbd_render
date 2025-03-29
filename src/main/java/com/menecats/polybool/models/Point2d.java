package com.menecats.polybool.models;

import java.util.Objects;

public class Point2d {

    public final double x;
    public final double y;


    public Point2d(double x, double y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Point2d point2d = (Point2d) o;
        return Double.compare(point2d.x, x) == 0 &&
            Double.compare(point2d.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public String toString() {
        return String.format("[%s, %s]", x, y);
    }
}
