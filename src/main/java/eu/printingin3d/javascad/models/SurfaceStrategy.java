package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.coords.V3d;
import java.util.List;

public interface SurfaceStrategy {

    Result buildSurface();

    class Result {

        public final List<V3d> points;
        public final int width;
        public final int height;

        public Result(List<V3d> points, int width, int height) {
            this.points = points;
            this.width = width;
            this.height = height;
        }
    }
}
