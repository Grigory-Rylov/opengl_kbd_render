package com.github.grishberg.javascad.models;

import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.manifold.Manifold3dEngine;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hull extends Atomic3dModel {

    private final List<Model> models;

    public Hull(Model... obj) {
        this.models = new ArrayList<>();
        Collections.addAll(models, obj);
    }

    public Hull(List<Model> obj) {
        this.models = new ArrayList<>();
        models.addAll(obj);
    }

    @Override
    protected Boundaries3d getModelBoundaries() {
        ArrayList<Boundaries3d> boundaries3ds = new ArrayList<>();
        for (Model model : models) {
            boundaries3ds.add(model.getBoundaries());
        }
        return Boundaries3d.combine(boundaries3ds);
    }

    @Override
    protected Model innerCloneModel() {
        return new Hull(models);
    }

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        long[] handles = new long[models.size()];
        int idx = 0;
        try {
            for (Model model : models) {
                long h = model.toNativeMesh(context);
                handles[idx++] = h;
            }
        } catch (Exception e) {
            for (int i = 0; i < idx; i++) {
                Manifold3dEngine.INSTANCE.delete(handles[i]);
            }
            throw e;
        }
        try {
            return Manifold3dEngine.INSTANCE.hullNative(handles);
        } catch (Exception e) {
            return Manifold3dEngine.INSTANCE.emptyManifold();
        } finally {
            for (int i = 0; i < idx; i++) {
                Manifold3dEngine.INSTANCE.delete(handles[i]);
            }
        }
    }
}