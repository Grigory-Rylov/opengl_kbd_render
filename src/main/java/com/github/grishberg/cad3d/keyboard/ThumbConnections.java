package com.github.grishberg.cad3d.keyboard;

import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderLeft;
import static com.github.grishberg.cad3d.keyboard.KeyPlaceholder.placeHolderRight;
import static com.github.grishberg.cad3d.keyboard.Utils.hull;
import static com.github.grishberg.cad3d.keyboard.Utils.union;

import com.github.grishberg.cad3d.keyboard.cfg.KeyboardConfig;
import eu.printingin3d.javascad.models.Abstract3dModel;
import java.util.ArrayList;

public class ThumbConnections {

    private KeyboardConfig cfg;
    private final KeyPlace keyPlace;

    private final ArrayList<Abstract3dModel> models = new ArrayList<>();


    public ThumbConnections(KeyboardConfig cfg, KeyPlace keyPlace) {
        this.cfg = cfg;
        this.keyPlace = keyPlace;
    }

    public Abstract3dModel buildThumbPlaceConnections() {
        models.clear();

        addHull(
            ThumbKeyPlace.place1(placeHolderLeft()),
            ThumbKeyPlace.place2(placeHolderRight())
        );

        addHull(
            ThumbKeyPlace.place2(placeHolderLeft()),
            ThumbKeyPlace.place3(placeHolderRight())
        );

        return union(models);
    }

    private void addHull(Abstract3dModel... children) {
        models.add(hull(children));
    }

}
