package com.menecats.polybool.models.geojson;

import com.menecats.polybool.models.Point2d;
import java.util.List;

public abstract class Geometry<T> {
    public static final class PolygonGeometry extends Geometry<List<List<Point2d>>> {
        public PolygonGeometry() {
            this(null);
        }

        public PolygonGeometry(List<List<Point2d>> coordinates) {
            super(coordinates);
        }

        @Override
        public String getType() {
            return "Polygon";
        }
    }
    public static final class MultiPolygonGeometry extends Geometry<List<List<List<Point2d>>>> {
        public MultiPolygonGeometry() {
            this(null);
        }

        public MultiPolygonGeometry(List<List<List<Point2d>>> coordinates) {
            super(coordinates);
        }

        @Override
        public String getType() {
            return "MultiPolygon";
        }
    }

    private T coordinates;

    protected Geometry(T coordinates) {
        setCoordinates(coordinates);
    }

    public abstract String getType();

    public T getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(T coordinates) {
        this.coordinates = coordinates;
    }
}
