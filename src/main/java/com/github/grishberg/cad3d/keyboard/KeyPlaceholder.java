package com.github.grishberg.cad3d.keyboard;

import static com.github.grishberg.cad3d.keyboard.Utils.cube;

import eu.printingin3d.javascad.models.Abstract3dModel;

public class KeyPlaceholder {

    public static final double CORNER_OFFSET = 7.85;
    public static final double OFFSET = 6.85;
    private static final double EDGE_HEIGHT = 1.2;
    private static final double OUTER_WIDTH = 17.3;
    private static final double OUTER_HEIGHT = 17.3;
    private static final double KEY_HOLE_OUTER_WIDTH = 14.7;
    private static final double KEY_HOLE_INNER_WIDTH = 14;
    private static final double KEY_HOLE_HEIGHT = 14;
    private static final double WALL_THICKNESS = 1.5;
    private static final double HORIZONTAL_WALL_HEIGHT = 4;
    private static final double VERTICAL_WALL_HEIGHT = 3.2;
    private static final double TOP_THICKNESS = 4;
    private static final double CORNER_PLACEHOLDER_TOP_OFFSET = 2;
    private static final double BASE_TOP_OFFSET = 1.5 + 0.5;
    private static final double VERTICAL_TOP_OFFSET =
        BASE_TOP_OFFSET - VERTICAL_WALL_HEIGHT / 2 + 2.6 / 2;
    private static final double HORIZONTAL_TOP_OFFSET =
        BASE_TOP_OFFSET - HORIZONTAL_WALL_HEIGHT / 2 + 2.6 / 2;

    public static Abstract3dModel placeHolder() {
        return cube(OUTER_WIDTH, OUTER_HEIGHT, TOP_THICKNESS).move(0, 0, BASE_TOP_OFFSET)
            .subtractModel(cube(KEY_HOLE_INNER_WIDTH, KEY_HOLE_HEIGHT, 10))
            .subtractModel(cube(KEY_HOLE_OUTER_WIDTH, KEY_HOLE_HEIGHT, TOP_THICKNESS).move(
                0,
                0,
                VERTICAL_TOP_OFFSET - EDGE_HEIGHT
            ))
            .subtractModel(cube(KEY_HOLE_INNER_WIDTH, KEY_HOLE_HEIGHT, 2).move(
                0,
                CORNER_OFFSET,
                VERTICAL_TOP_OFFSET - 2
            ));
    }

    static Abstract3dModel placeHolderTop() {
        return cube(17.2, WALL_THICKNESS, TOP_THICKNESS).
            move(0.0, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET);
    }

    static Abstract3dModel placeHolderLeft() {
        return cube(WALL_THICKNESS, 17.2, TOP_THICKNESS).move(
            -CORNER_OFFSET - WALL_THICKNESS,
            0,
            CORNER_PLACEHOLDER_TOP_OFFSET
        );
    }

    static Abstract3dModel placeHolderRight() {
        return cube(WALL_THICKNESS, 17.2, TOP_THICKNESS)
            .move(CORNER_OFFSET + WALL_THICKNESS, 0, CORNER_PLACEHOLDER_TOP_OFFSET);
    }

    static Abstract3dModel placeHolderBottom() {
        return cube(17.2, WALL_THICKNESS, TOP_THICKNESS).move(
            0,
            -CORNER_OFFSET - WALL_THICKNESS,
            CORNER_PLACEHOLDER_TOP_OFFSET
        );
    }

    static Abstract3dModel placeHolderTopLeft() {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS)
            .move(-CORNER_OFFSET - WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET);
    }

    static Abstract3dModel placeHolderTopLeft(int height) {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height)
            .move(-CORNER_OFFSET - WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET);
    }

    static Abstract3dModel placeHolderTopRight() {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS)
            .move(CORNER_OFFSET + WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET);
    }
    static Abstract3dModel placeHolderTopRight(int height) {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height)
            .move(CORNER_OFFSET + WALL_THICKNESS, CORNER_OFFSET + WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET);
    }

    static Abstract3dModel placeHolderBottomLeft() {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS)
            .move(-CORNER_OFFSET - WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET);
    }
    static Abstract3dModel placeHolderBottomLeft(int height) {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height)
            .move(-CORNER_OFFSET - WALL_THICKNESS, -CORNER_OFFSET - WALL_THICKNESS, CORNER_PLACEHOLDER_TOP_OFFSET);
    }

    static Abstract3dModel placeHolderBottomRight() {
        return cube(WALL_THICKNESS, WALL_THICKNESS, TOP_THICKNESS).move(
            CORNER_OFFSET + WALL_THICKNESS,
            -CORNER_OFFSET - WALL_THICKNESS,
            CORNER_PLACEHOLDER_TOP_OFFSET
        );
    }
    static Abstract3dModel placeHolderBottomRight(int height) {
        return cube(WALL_THICKNESS, WALL_THICKNESS, height).move(
            CORNER_OFFSET + WALL_THICKNESS,
            -CORNER_OFFSET - WALL_THICKNESS,
            CORNER_PLACEHOLDER_TOP_OFFSET
        );
    }
}
