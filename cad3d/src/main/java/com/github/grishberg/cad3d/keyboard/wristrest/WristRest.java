package com.github.grishberg.cad3d.keyboard.wristrest;

import static com.github.grishberg.cad3d.keyboard.Utils.v3d;

import com.github.grishberg.javascad.coords.V3d;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Cylinder;
import com.github.grishberg.javascad.models.EdgeType;
import com.github.grishberg.javascad.models.surfaces.BicubicInterpolator;
import com.github.grishberg.javascad.models.surfaces.SmoothSurface;
import com.github.grishberg.javascad.models.surfaces.bicubic.BicubicSurfaceSpline;

public class WristRest {

    private static final int resolution = 15;
    private static final int thickness = 4;

    private static final V3d[][] controlPoints = {
        {
            v3d(-60, -80, 5),
            v3d(-40, -80, 30),
            v3d(-10, -90, 20),
            v3d(16, -60, 15),
            v3d(60, -80, 15),
            v3d(70, -80, 10),
        },
        {
            v3d(-42, -100, 5),
            v3d(-30, -100, 30),
            v3d(-16, -100, 20),
            v3d(16, -100, 10),
            v3d(60, -100, 20),
            v3d(70, -100, 10),
        },
        {
            v3d(-60, -150, 15),
            v3d(-40, -150, 30),
            v3d(-16, -130, 40),
            v3d(16, -130, 40),
            v3d(60, -150, 20),
            v3d(90, -150, 30),
        }
    };

    public static Model build() {
        //V3d[][] points = S6x3Linear.create(controlPoints, 5).buildSurface();
        V3d[][] points = new BicubicInterpolator(controlPoints).generateSurface(10);

        Model surfaceBuilder = new SmoothSurface(
            BicubicSurfaceSpline.bSplineSurface(controlPoints, resolution),
            thickness,
            EdgeType.Vertical,
            EdgeType.Normal,
            EdgeType.Normal,
            EdgeType.Vertical
        );
        //       List<Model> models = new ArrayList<>();
        //       for(V3d[] w: points) {
        //           for (V3d p: w) {
        //               models.add(new Cube(1).move(p));
        //           }
        //       }

        //      Model topSurface = new SmoothSurface3(
        //          S6x3.s6x3(controlPoints).buildSurfaceStrategy(resolution),
        //          thickness
        //      );
        //
        //      Model wristRest = topSurface.addModel(wristRestMount())
        //          .subtractModel(new Cube(300, 300, 50).move(0, 0, -25));

        return surfaceBuilder;//Utils.union(models);
    }

    private static Model wristRestMount() {
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

    private static Model padsHoles() {
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
