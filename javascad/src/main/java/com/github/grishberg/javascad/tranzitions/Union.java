package com.github.grishberg.javascad.tranzitions;


import com.github.grishberg.javascad.context.IScadGenerationContext;
import com.github.grishberg.javascad.coords.Boundaries3d;
import com.github.grishberg.javascad.manifold.Manifold3dEngine;
import com.github.grishberg.javascad.models.Model;
import com.github.grishberg.javascad.models.Complex3dModel;
import com.github.grishberg.javascad.utils.Color;
import com.github.grishberg.javascad.utils.ListUtils;
import com.github.grishberg.javascad.vrl.FacetGenerationContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * <p>Represents an union of models. It is a descendant of {@link Model}, which means you
 * can use the convenient methods on unions too.</p>
 * <p>You don't have to worry about the optimization either, because the generated OpenSCAD code
 * will be
 * the optimal one in every case. The parameters could even contain null elements, those will
 * be ignored during the model generation.</p>
 * <p>You can use the {@link #addModel} method to add more models to the union.</p>
 */
public class Union extends Complex3dModel {

    protected final List<Model> models;

    /**
     * Construct the object.
     *
     * @param models list of models
     */
    public Union(List<Model> models) {
        this.models = models == null
            ? Collections.<Model>emptyList()
            : ListUtils.removeNulls(models);
    }

    /**
     * Construct the object.
     *
     * @param models array of models
     */
    public Union(Model... models) {
        this(Arrays.asList(models));
    }

    /**
     * Construct the object.
     *
     * @param models array of models
     */
    public Union(Color color, List<Model> models) {
        this(new ArrayList<>(models));
        this.setColor(color);
    }

    @Override
    protected Boundaries3d getModelBoundaries() {
        List<Boundaries3d> boundaries = new ArrayList<>();
        for (Model model : models) {
            boundaries.add(model.getBoundaries());
        }
        return Boundaries3d.combine(boundaries);
    }

    @Override
    protected Model innerCloneModel() {
        return new Union(new ArrayList<Model>(models));
    }

    @Override
    protected long toInnerNativeMesh(FacetGenerationContext context) {
        synchronized (Manifold3dEngine.JNI_SYNC) {
            long result = 0L;
            try {
                for (Model model : models) {
                    long m = model.toNativeMesh(context);
                    if (m == 0L) {
                        continue;
                    }
                    if (result == 0L) {
                        result = m;
                    } else {
                        long r = Manifold3dEngine.INSTANCE.unionNative(result, m);
                        Manifold3dEngine.INSTANCE.delete(m);
                        Manifold3dEngine.INSTANCE.delete(result);
                        result = r;
                    }
                }
            } catch (Exception e) {
                if (result != 0L) Manifold3dEngine.INSTANCE.delete(result);
                throw e;
            }
            return result;
        }
    }

    @Override
    public Model addModel(Model model) {
        if (isMoved() || isRotated()) {
            return super.addModel(model);
        }

        List<Model> newModels = new ArrayList<>(models);
        newModels.add(model);
        return new Union(newModels);
    }

    @Override
    protected Model innerSubModel(IScadGenerationContext context) {
        List<Model> subModels = new ArrayList<>();
        for (Model m : models) {
            subModels.add(m.subModel(context));
        }

        return new Union(subModels);
    }

    @Override
    protected List<Model> getChildrenModels() {
        return models;
    }
}
