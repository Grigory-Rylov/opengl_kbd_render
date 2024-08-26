package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.coords.V3d;
import java.util.List;

public interface SurfaceStrategySquare {

    List<List<V3d>> buildSurface(int resolution);
}
