package com.github.grishberg.cad3d.keyboard;

import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBottom;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBottomLeft;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderBottomRight;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderLeft;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderRight;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderTop;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderTopLeft;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderTopRight;
import static com.github.grishberg.cad3d.keyboard.Utils.hull;
import static com.github.grishberg.cad3d.keyboard.Utils.union;

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import eu.printingin3d.javascad.models.Abstract3dModel;
import java.util.ArrayList;

public class Connections {

    private KeyboardConfig cfg;
    private final KeyPlace keyPlace;
    private final ArrayList<Abstract3dModel> models = new ArrayList<>();

    public Connections(KeyboardConfig cfg, KeyPlace keyPlace) {
        this.cfg = cfg;
        this.keyPlace = keyPlace;
    }

    public Abstract3dModel buildConnections() {
        models.clear();
        // diagonals
        for (int column = 0; column < cfg.getColumnsCount() - 1; column++) {
            for (int row = 0; row < cfg.getRowsCount() - 1; row++) {
                addHull(
                    keyPlace.place(column, row, placeHolderBottomRight()),
                    keyPlace.place(column + 1, row, placeHolderBottomLeft()),
                    keyPlace.place(column + 1, row + 1, placeHolderTopLeft()),
                    keyPlace.place(column, row + 1, placeHolderTopRight())
                );
            }
        }

        //columns
        for (int column = 0; column < cfg.getColumnsCount() - 1; column++) {
            for (int row = 0; row < cfg.getRowsCount(); row++) {
                addHull(
                    keyPlace.place(column, row, placeHolderRight()),
                    keyPlace.place(column + 1, row, placeHolderLeft())
                );
            }
        }

        // rows
        for (int column = 0; column < cfg.getColumnsCount(); column++) {
            for (int row = 0; row < cfg.getRowsCount() - 1; row++) {
                addHull(
                    keyPlace.place(column, row, placeHolderBottom())
                    , keyPlace.place(column, row + 1, placeHolderTop()));
            }

        }
        //side
//        for (int row = 0; row < cfg.getRowsCount(); row++) {
//            addHull(
//                keyPlace.place(0, row, placeHolderTopLeft()),
//                keyPlace.place(0, row, placeHolderBottomLeft())
//            );
//
//            addHull(
//                keyPlace.place(cfg.getColumnsCount() - 1, row, placeHolderTopRight()),
//                keyPlace.place(cfg.getColumnsCount() - 1, row, placeHolderBottomRight())
//            );
//        }
//        for (int row = 0; row < cfg.getRowsCount(); row++) {
//            addHull(
//                keyPlace.place(0, row, placeHolderBottomLeft()),
//                keyPlace.place(0, row + 1, placeHolderTopLeft())
//            );
//
//            addHull(
//                keyPlace.place(cfg.getColumnsCount() - 1, row, placeHolderBottomRight()),
//                keyPlace.place(cfg.getColumnsCount() - 1, row + 1, placeHolderTopRight())
//            );
//
//        }

        return union(models);
    }

    private void addHull(Abstract3dModel... children) {
        models.add(hull(children));
    }
}

