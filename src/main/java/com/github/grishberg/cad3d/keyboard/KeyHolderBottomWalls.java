package com.github.grishberg.cad3d.keyboard;

import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.OFFSET;
import static com.github.grishberg.cad3d.keyboard.Utils.cylinder;
import static com.github.grishberg.cad3d.keyboard.Utils.hull;
import static com.github.grishberg.cad3d.keyboard.Utils.sphere;
import static com.github.grishberg.cad3d.keyboard.Utils.union;

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import eu.printingin3d.javascad.coords.V3d;
import eu.printingin3d.javascad.models.Abstract3dModel;
import java.util.ArrayList;
import java.util.List;

public class KeyHolderBottomWalls {

    private static final double BOTTOM_OFFSET = 6;
    private static final int HOLE_SIZE = 14;
    public static final double HOLE_RADIUS = 5;
    private static final double HEIGHT = HOLE_RADIUS * 2;
    private final KeyboardConfig cfg;
    private final KeyPlace keyPlace;

    public KeyHolderBottomWalls(KeyboardConfig cfg, KeyPlace keyPlace) {
        this.cfg = cfg;
        this.keyPlace = keyPlace;
    }

    public Abstract3dModel build() {
        final List<Abstract3dModel> models = new ArrayList<>();

        final double offsetZ = BOTTOM_OFFSET;
        final V3d offset = new V3d(0, 0, offsetZ);
        // back
        for (int column = 0; column < cfg.getColumnsCount(); column++) {
            for (int row = 0; row < cfg.getRowsCount() - 1; row++) {
                models.add(hull(
                    keyPlace.place(column, row, singleHole(), offset),
                    keyPlace.place(column, row + 1, singleHole(), offset)
                ));

            }
        }

        final V3d edgesOffset = new V3d(0, 0, BOTTOM_OFFSET);

        for (int column = 0; column < cfg.getColumnsCount() - 1; column++) {
            for (int row = 0; row < cfg.getRowsCount(); row++) {
                models.add(hull(
                    keyPlace.place(
                        column,
                        row,
                        cornerModel().move(OFFSET, OFFSET, 0),
                        edgesOffset
                    ),
                    keyPlace.place(
                        column + 1,
                        row,
                        cornerModel().move(-OFFSET, OFFSET, 0),
                        edgesOffset
                    ),
                    keyPlace.place(
                        column,
                        row,
                        cornerModel().move(OFFSET, -OFFSET, 0),
                        edgesOffset
                    ),
                    keyPlace.place(
                        column + 1,
                        row,
                        cornerModel().move(-OFFSET, -OFFSET, 0),
                        edgesOffset
                    )

                ));
            }
        }

        // inner
        for (int column = 0; column < cfg.getColumnsCount() - 1; column++) {
            for (int row = 0; row < cfg.getRowsCount() - 1; row++) {
                models.add(hull(
                    keyPlace.place(
                        column,
                        row,
                        cornerModel().move(OFFSET, -OFFSET, 0),
                        edgesOffset
                    ),
                    keyPlace.place(
                        column + 1,
                        row,
                        cornerModel().move(-OFFSET, -OFFSET, 0),
                        edgesOffset
                    ),
                    keyPlace.place(
                        column,
                        row + 1,
                        cornerModel().move(OFFSET, OFFSET, 0),
                        edgesOffset
                    ),
                    keyPlace.place(
                        column + 1,
                        row + 1,
                        cornerModel().move(-OFFSET, OFFSET, 0),
                        edgesOffset
                    )

                ));
            }

        }

        return union(models);
    }

    private static Abstract3dModel singleHole() {
        return
            hull(
                cornerModel().move(-OFFSET, -OFFSET, 0),
                cornerModel().move(-OFFSET, OFFSET, 0),
                cornerModel().move(OFFSET, OFFSET, 0),
                cornerModel().move(OFFSET, -OFFSET, 0)
            );
    }

    private static Abstract3dModel cornerModel() {
        return sphere(HOLE_RADIUS).subtractModel(cylinder(HOLE_RADIUS, HOLE_RADIUS).move(
            0,
            0,
            HOLE_RADIUS / 2
        ));
    }
}
