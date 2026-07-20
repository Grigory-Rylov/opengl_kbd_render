package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Minkowski extends Atomic3dModel {

    private final List<Model> models;

    public Minkowski(Model... obj) {
        this.models = new ArrayList<>();
        Collections.addAll(models, obj);
    }

    public Minkowski(List<Model> obj) {
        this.models = new ArrayList<>();
        models.addAll(obj);
    }

    @Override
    protected Model innerCloneModel() {
        return new Minkowski(models);
    }

    @Override
    protected Boundaries3d getModelBoundaries() {
        //TODO calculate real boundary
        ArrayList<Boundaries3d> boundaries3ds = new ArrayList<>();
        for (Model model : models) {
            boundaries3ds.add(model.getBoundaries());
        }
        return Boundaries3d.combine(boundaries3ds);
    }

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        throw new UnsupportedOperationException("Minkowski is not supported in native mode");
    }
}
