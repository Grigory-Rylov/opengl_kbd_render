package com.menecats.polybool.models;

public final class Segment {
    public static final class SegmentFill {
        public Boolean above;
        public Boolean below;

        public SegmentFill() {
        }

        public SegmentFill(Boolean above, Boolean below) {
            this.above = above;
            this.below = below;
        }
    }

    public Point2d start;
    public Point2d end;
    public SegmentFill myFill;
    public SegmentFill otherFill;

    public Segment(Point2d start, Point2d end) {
        this(start, end, new SegmentFill());
    }

    public Segment(Point2d start, Point2d end, SegmentFill myFill) {
        this.start = start;
        this.end = end;
        this.myFill = myFill;
    }
}
