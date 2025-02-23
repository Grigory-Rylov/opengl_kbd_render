package com.github.grishberg.cad3d.keyboard;

import eu.printingin3d.javascad.basic.Radius;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import eu.printingin3d.javascad.models.Cube;
import eu.printingin3d.javascad.models.Cylinder;
import eu.printingin3d.javascad.models.Hull;
import eu.printingin3d.javascad.models.Sphere;
import eu.printingin3d.javascad.tranzitions.Union;
import java.util.List;

public class Utils {

    public static Abstract3dModel union(List<Abstract3dModel> models) {
        return new Union(models);
    }

    public static Abstract3dModel hull(Abstract3dModel... models) {
        return new Hull(models);
    }

    public static Cube cube(double x, double y, double z) {
        return new Cube(x, y, z);
    }

    public static Cube cube(double size) {
        return new Cube(size);
    }


    public static Cylinder cylinder(double radius, double height) {
        return new Cylinder(height, Radius.fromRadius(radius));
    }

    public static Sphere sphere(double radius) {
        return new Sphere(Radius.fromRadius(radius));
    }

    public static V3d v3d(double x, double y, double z) {
        return new V3d(x, y, z);
    }
}
