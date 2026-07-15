package eu.printingin3d.javascad.models;

import eu.printingin3d.javascad.coords.Boundaries3d;
import eu.printingin3d.javascad.manifold.Manifold3dEngine;
import eu.printingin3d.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Hull extends Atomic3dModel {

    private final List<Abstract3dModel> models;

    public Hull(Abstract3dModel... obj) {
        this.models = new ArrayList<>();
        Collections.addAll(models, obj);
    }

    public Hull(List<Abstract3dModel> obj) {
        this.models = new ArrayList<>();
        models.addAll(obj);
    }

    @Override
    protected Boundaries3d getModelBoundaries() {
        ArrayList<Boundaries3d> boundaries3ds = new ArrayList<>();
        for (Abstract3dModel model : models) {
            boundaries3ds.add(model.getBoundaries());
        }
        return Boundaries3d.combine(boundaries3ds);
    }

    @Override
    protected Abstract3dModel innerCloneModel() {
        return new Hull(models);
    }

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        long[] handles = new long[models.size()];
        int idx = 0;
        try {
            for (Abstract3dModel model : models) {
                handles[idx++] = model.toNativeMesh(context);
            }
        } catch (Exception e) {
            for (int i = 0; i < idx; i++) {
                Manifold3dEngine.INSTANCE.delete(handles[i]);
            }
            throw e;
        }
        long result = Manifold3dEngine.INSTANCE.hullNative(handles);
        for (int i = 0; i < idx; i++) {
            Manifold3dEngine.INSTANCE.delete(handles[i]);
        }
        return result;
    }
}