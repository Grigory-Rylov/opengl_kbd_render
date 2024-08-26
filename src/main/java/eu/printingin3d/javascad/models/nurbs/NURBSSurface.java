package eu.printingin3d.javascad.models.nurbs;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.SurfaceStrategy;
import java.util.ArrayList;
import java.util.List;

class NURBSSurface {

   private final V3d[][] controlPoints;

   NURBSSurface(V3d[][] controlPoints) {
      this.controlPoints = controlPoints;
   }

   public List<V3d> buildSurface(int resolution) {
      List<V3d> surface = new ArrayList<>();

      Map2D<Long, Long, V3d> points = getPointGrid(controlPoints);
      NurbSurfaceGenerator nurb = new NurbSurfaceGenerator(points, 4, resolution);

      for (double v = 0; v <= 1; v += 1.0 / resolution) {
         for (double u = 0; u <= 1; u += 1.0 / resolution) {
            V3d[] curvePoints = new V3d[5];

            //V3d point = bezierPoint(v, curvePoints[0], curvePoints[1], curvePoints[2], curvePoints[3], curvePoints[4]);
            //surface.add(new V3d(point.getX(), point.getY(), point.getZ()));
         }
      }

      return surface;
   }

   private static Map2D<Long, Long, V3d> getPointGrid(V3d[][] points) {
      Map2D<Long, Long, V3d> ptsGrid = new Map2D<>();
      for (V3d[] y: points) {
         for (V3d x: y) {
            PutVector(ptsGrid, x);
         }
      }

      return ptsGrid;
   }

   private static void PutVector(Map2D<Long, Long, V3d> map, V3d v) {
      map.putValue((long)v.getX(), (long)v.getZ(), v);
   }

}
