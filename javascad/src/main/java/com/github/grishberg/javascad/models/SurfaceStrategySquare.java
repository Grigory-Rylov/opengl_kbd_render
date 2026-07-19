package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.coords.V3d;
import java.util.List;

public interface SurfaceStrategySquare {

    List<List<V3d>> buildSurface(int resolution);
}
