package com.github.grishberg.cad3d.keyboard.wristrest;

import static com.github.grishberg.cad3d.keyboard.Utils.v3d;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.models.Cylinder;
import eu.printingin3d.javascad.models.surfaces.S6x3;
import eu.printingin3d.javascad.models.surfaces.SmoothSurface3;

public class WristRest {
   private static final int resolution = 15;
   private static final int thickness = 4;

   private static final V3d[][] controlPoints = {
       {
           v3d(-60, -80, 5),
           v3d(-40, -80, 30),
           v3d(-10, -90, 20),
           v3d( 16, -60, 15),
           v3d(60, -80, 15),
           v3d(60, -80, 10),
       },
       {
           v3d(-65, -100, 5),
           v3d(-40, -100, 30),
           v3d(-16, -100, 20),
           v3d(16, -100,  10),
           v3d(60, -100, 20),
           v3d(60, -100, 10),
       },
       {
           v3d(-60,-150,  15),
           v3d(-40, -150, 30),
           v3d(-16,-130,  40),
           v3d(16,-130,  40),
           v3d(60, -150, 20),
           v3d(60,-150,  30),
       }
   };
   public static Abstract3dModel build(){
      Abstract3dModel topSurface = new SmoothSurface3(
          S6x3.s6x3(controlPoints).buildSurfaceStrategy(resolution),
          thickness
      );

      Abstract3dModel wristRest = topSurface.addModel(wristRestMount())
          .subtractModel(new Cube(300, 300, 50).move(0, 0, -25));

      return wristRest;
   }

   private static Abstract3dModel wristRestMount() {
      // left back
       int r = 6;
       return new Cylinder(42, r)
          .move(-57, -88, -8)
          .addModel(
              // left front
              new Cylinder(56, r).move(-55, -142, -10)
          )
          .addModel(
              // right back
              new Cylinder(48, r).move(56, -85, -7)
          ).addModel(
              // right front
              new Cylinder(62, r).move(53, -140, -5)
          )
           .subtractModel(
               padsHoles()
           );
   }
   private static Abstract3dModel padsHoles(){
       double padsRad = 5;
       double h = 20;
       double offsetZ = -9;
       return new Cylinder(h, padsRad)
           .move(-57, -88, offsetZ)
           .addModel(
               // left front
               new Cylinder(h, padsRad).move(-55, -142, offsetZ)
           )
           .addModel(
               // right back
               new Cylinder(h, padsRad).move(56, -85, offsetZ)
           ).addModel(
               // right front
               new Cylinder(h, padsRad).move(53, -140, offsetZ)
           );
   }
}
