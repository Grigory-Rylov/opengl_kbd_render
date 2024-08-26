package eu.printingin3d.javascad.models.surfaces.bicubic;

import eu.printingin3d.javascad.coords.V3d;

public class Vertex {
   /**
    * Vertex position.
    */
   public final V3d position;
   /**
    * Vertex normal.
    */
   public V3d normal = new V3d(0,0,0);

   public Vertex(V3d position, V3d normal) {
      this.position = position;
      this.normal = normal;
   }

   public Vertex(V3d position) {
      this.position = position;
   }
}
