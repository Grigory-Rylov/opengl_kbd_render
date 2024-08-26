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
        return place(obj, 14, -40, 10, new V3d(-15, -10 + OFFSET_Y, 5 + OFFSET_Z));
    }

    public static Abstract3dModel place2(Abstract3dModel obj) {
        return place(obj, 10, -32, 34, new V3d(-32, -17 + OFFSET_Y, -8 + OFFSET_Z));
    }

    public static Abstract3dModel place3(Abstract3dModel obj) {
        return place(obj, 6, -30, 40, new V3d(-46.0, -28.5 + OFFSET_Y, -19 + OFFSET_Z));
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
