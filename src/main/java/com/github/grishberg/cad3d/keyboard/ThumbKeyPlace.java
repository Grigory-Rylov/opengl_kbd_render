package com.github.grishberg.cad3d.keyboard;

import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.Abstract3dModel;

public class ThumbKeyPlace {

    private static final int OFFSET_Z = 22;
    private static final int OFFSET_Y = -40;

    public static Abstract3dModel thumbPlace(Abstract3dModel obj) {
        return place1(obj)
            .addModel(place2(obj))
            .addModel(place3(obj));
    }

    public static Abstract3dModel place1(Abstract3dModel obj) {
        return place(obj, 14, -40, 10, new V3d(-5, -10 + OFFSET_Y, 5 + OFFSET_Z));
    }

    public static Abstract3dModel place2(Abstract3dModel obj) {
        return place(obj, 12, -39, 22, new V3d(-22, -14 + OFFSET_Y, -7 + OFFSET_Z));
    }

    public static Abstract3dModel place3(Abstract3dModel obj) {
        return place(obj, 8, -44, 28, new V3d(-35.0, -20.5 + OFFSET_Y, -21 + OFFSET_Z));
    }

    private static Abstract3dModel place(
        Abstract3dModel obj,
        double xAngle,
        double yAngle,
        double zAngle,
        V3d offset
    ) {
        return obj.rotate(xAngle, yAngle, zAngle).move(offset);
    }

}
